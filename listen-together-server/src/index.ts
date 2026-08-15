/**
 * BeatWave Listen Together — Cloudflare Worker router.
 *
 * The Worker is stateless. Its only job is to turn a room code into the one
 * Durable Object that owns that room, and hand the socket over. Everything
 * that needs to be consistent — who is host, what is playing, what is queued —
 * lives inside that single DO, so there is never a second server with a
 * different opinion about the same room.
 */

import { Room } from './room';
import { Guard } from './guard';
import { TEST_CLIENT_HTML } from './testclient';

export { Room, Guard };

export interface Env {
  ROOM: DurableObjectNamespace;
  GUARD: DurableObjectNamespace;
  ROOM_TTL_HOURS: string;
  MAX_EXTENSIONS: string;
  MAX_MEMBERS: string;
}

/**
 * Throttle by client IP. Returns true when the caller should be turned away.
 * Fails OPEN: if the guard itself errors, let the request through rather than
 * locking every user out of the app because a counter misbehaved.
 */
async function throttled(request: Request, env: Env, limit: number): Promise<boolean> {
  const ip = request.headers.get('CF-Connecting-IP');
  if (!ip) return false;
  try {
    const stub = env.GUARD.get(env.GUARD.idFromName(ip));
    // Fully-qualified dummy host. A single-label hostname here (https://guard/)
    // is what made the room claim fail with Cloudflare error 1042.
    const res = await stub.fetch(`https://do.invalid/guard?limit=${limit}`);
    return res.status === 429;
  } catch {
    return false;
  }
}

// No 0/O, no 1/I/L. Room codes get read aloud and typed by hand; the ambiguous
// glyphs are where "it says the code is wrong" actually comes from.
const CODE_ALPHABET = 'ABCDEFGHJKMNPQRSTUVWXYZ23456789';
const CODE_LENGTH = 6;

function generateCode(): string {
  const bytes = new Uint8Array(CODE_LENGTH);
  crypto.getRandomValues(bytes);
  let out = '';
  for (const b of bytes) out += CODE_ALPHABET[b % CODE_ALPHABET.length];
  return out;
}

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'content-type': 'application/json' },
  });
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const url = new URL(request.url);

    // Health check, so you can tell "server is down" from "room is gone".
    if (url.pathname === '/health') {
      return json({ ok: true });
    }

    // Browser-based second participant, for testing without a second phone.
    if (url.pathname === '/test') {
      return new Response(TEST_CLIENT_HTML, {
        headers: { 'content-type': 'text/html; charset=utf-8' },
      });
    }

    // Allocate a room. Done over HTTP rather than after the socket opens
    // because the Worker has to know the room code to pick a Durable Object,
    // and at WebSocket-upgrade time it would not know it yet.
    if (url.pathname === '/api/rooms' && request.method === 'POST') {
      if (await throttled(request, env, 10)) {
        return json({ error: 'rate_limited' }, 429);
      }
      // idFromName is deterministic, so a duplicate code does not create a
      // second room — it silently joins someone else's. Codes must therefore
      // be checked for liveness, not just generated and hoped over.
      for (let attempt = 0; attempt < 5; attempt++) {
        const code = generateCode();
        try {
          const stub = env.ROOM.get(env.ROOM.idFromName(code));
          const probe = await stub.fetch(`https://do.invalid/claim?code=${code}`, {
            method: 'POST',
          });
          if (probe.ok) {
            return json({ room_code: code });
          }
        } catch (e) {
          console.error('claim failed', code, String(e), (e as Error)?.stack);
          return json({ error: 'claim_failed', detail: String(e) }, 500);
        }
      }
      return json({ error: 'could_not_allocate_room' }, 503);
    }

    // wss://host/room/<CODE>
    const match = url.pathname.match(/^\/room\/([A-Z0-9]{4,12})$/i);
    if (match) {
      if (request.headers.get('Upgrade') !== 'websocket') {
        return new Response('expected websocket', { status: 426 });
      }
      // Walking the code space means many connects from one IP in a short
      // window, which is exactly what this catches.
      if (await throttled(request, env, 20)) {
        return new Response('rate limited', { status: 429 });
      }
      const code = match[1].toUpperCase();
      const stub = env.ROOM.get(env.ROOM.idFromName(code));
      return stub.fetch(request);
    }

    // A bare-root connection means a client that has not been updated for the
    // room-in-the-URL flow. Say so, rather than returning an anonymous 404 that
    // shows up in the log as an unexplained "GET / - Ok".
    if (url.pathname === '/' || url.pathname === '') {
      return json(
        {
          error: 'room_required',
          message:
            'Allocate a room with POST /api/rooms, then connect to wss://<host>/room/<CODE>. ' +
            'Connecting to the root is the pre-v2 flow and is not supported here.',
        },
        426,
      );
    }

    return json({ error: 'not_found', path: url.pathname }, 404);
  },
};
