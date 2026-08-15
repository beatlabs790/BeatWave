/**
 * A second listener you can open in a browser tab.
 *
 * Testing a shared-listening feature normally needs two phones. This is a real
 * protocol client — it joins a room, shows the live room state, and can drive
 * playback — so one device plus a browser tab is enough to exercise host/member
 * behaviour, the control-mode toggle and the countdown.
 *
 * Served from the Worker itself so there is nothing to run locally.
 */
export const TEST_CLIENT_HTML = `<!doctype html>
<html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>BeatWave Sync — test member</title>
<style>
:root{color-scheme:dark}
*{box-sizing:border-box}
body{margin:0;font:15px/1.5 ui-sans-serif,system-ui,-apple-system,Segoe UI,Roboto,sans-serif;
background:#0b0b0f;color:#f2f2f7;padding:24px;max-width:760px;margin-inline:auto}
h1{font-size:19px;margin:0 0 2px}
.sub{color:#8e8e93;font-size:13px;margin-bottom:20px}
.card{background:#1c1c1e;border:1px solid #2c2c2e;border-radius:16px;padding:16px;margin-bottom:14px}
label{display:block;font-size:12px;color:#8e8e93;margin-bottom:6px}
input{width:100%;padding:11px 13px;border-radius:11px;border:1px solid #3a3a3c;
background:#2c2c2e;color:#fff;font-size:15px}
.row{display:flex;gap:10px;flex-wrap:wrap;margin-top:12px}
button{padding:11px 16px;border-radius:11px;border:0;background:#0a84ff;color:#fff;
font-size:14px;font-weight:600;cursor:pointer}
button.sec{background:#2c2c2e;color:#f2f2f7}
button:disabled{opacity:.35;cursor:not-allowed}
.pill{display:inline-block;padding:3px 10px;border-radius:999px;font-size:12px;
background:#2c2c2e;color:#8e8e93;margin-right:6px}
.pill.on{background:#0a84ff33;color:#64d2ff}
.pill.warn{background:#ff453a33;color:#ff6961}
pre{background:#000;border-radius:12px;padding:12px;max-height:320px;overflow:auto;
font:12px/1.5 ui-monospace,SFMono-Regular,Menlo,monospace;color:#98989d;margin:0}
.k{color:#64d2ff}
</style></head><body>
<h1>BeatWave Sync — test member</h1>
<div class="sub">A second participant, without a second phone. Joins as a normal member.</div>

<div class="card">
  <label>Room code (from the app)</label>
  <input id="code" placeholder="ABC123" autocapitalize="characters">
  <label style="margin-top:12px">Name</label>
  <input id="name" value="browser">
  <div class="row">
    <button id="join">Join room</button>
    <button id="leave" class="sec" disabled>Leave</button>
  </div>
</div>

<div class="card">
  <div id="pills"><span class="pill">disconnected</span></div>
  <div class="row">
    <button class="act sec" data-a="play" disabled>Play</button>
    <button class="act sec" data-a="pause" disabled>Pause</button>
    <button class="act sec" data-a="skip_next" disabled>Next</button>
    <button class="act sec" data-a="skip_prev" disabled>Prev</button>
  </div>
  <div class="sub" style="margin:10px 0 0" id="hint">
    While the room is owner-only these are refused by the server — that is the
    control mode working, not a bug.
  </div>
</div>

<div class="card"><pre id="log"></pre></div>

<script>
const $ = (s) => document.querySelector(s);
const logEl = $('#log');
let ws = null, me = null, token = null;

function log(dir, type, extra) {
  const t = new Date().toLocaleTimeString();
  logEl.textContent = \`\${t}  \${dir}  \${type}\${extra ? '  ' + extra : ''}\\n\` + logEl.textContent;
}
function send(type, payload) {
  ws?.send(JSON.stringify(payload === undefined ? { type } : { type, payload }));
  log('->', type);
}
function pills(items) { $('#pills').innerHTML = items.join(''); }
function setActs(on) { document.querySelectorAll('.act').forEach(b => b.disabled = !on); }

function render(state) {
  if (!state) return;
  const mode = state.control_mode || 'owner';
  const left = state.expires_at ? Math.max(0, state.expires_at - Date.now()) : 0;
  const mins = Math.floor(left / 60000), secs = Math.floor((left % 60000) / 1000);
  pills([
    '<span class="pill on">connected</span>',
    \`<span class="pill">\${(state.users || []).length} in room</span>\`,
    \`<span class="pill \${mode === 'everyone' ? 'on' : ''}">control: \${mode}</span>\`,
    left ? \`<span class="pill \${left < 600000 ? 'warn' : ''}">ends in \${mins}m \${secs}s</span>\` : '',
    state.current_track ? \`<span class="pill">\${state.current_track.title}</span>\` : '',
  ]);
}

let lastState = null;
setInterval(() => render(lastState), 1000);

$('#join').onclick = () => {
  const code = $('#code').value.trim().toUpperCase();
  if (!code) return;
  ws = new WebSocket(location.origin.replace(/^http/, 'ws') + '/room/' + code);
  ws.onopen = () => { log('--', 'socket open'); send('join_room', { username: $('#name').value || 'browser' }); };
  ws.onclose = () => { log('--', 'socket closed'); setActs(false); $('#leave').disabled = true; $('#join').disabled = false; };
  ws.onmessage = (e) => {
    const m = JSON.parse(e.data);
    if (m.type === 'pong') return;
    log('<-', m.type, m.payload?.reason || m.payload?.message || '');
    if (m.type === 'join_approved') {
      me = m.payload.user_id; token = m.payload.session_token;
      lastState = m.payload.state; render(lastState);
      setActs(true); $('#leave').disabled = false; $('#join').disabled = true;
    }
    if (m.type === 'join_rejected') alert('Rejected: ' + (m.payload?.reason || 'unknown'));
    if (m.type === 'sync_state') { lastState = { ...lastState, ...m.payload }; render(lastState); }
    if (m.type === 'control_mode_changed') { lastState = { ...lastState, control_mode: m.payload.control_mode }; render(lastState); }
    if (m.type === 'sync_playback') { lastState = { ...lastState, is_playing: m.payload.action === 'play' }; render(lastState); }
    if (m.type === 'room_closed') { alert('Room closed'); ws.close(); }
  };
  setInterval(() => ws?.readyState === 1 && ws.send(JSON.stringify({ type: 'ping' })), 25000);
};

$('#leave').onclick = () => { send('leave_room'); ws?.close(); };
document.querySelectorAll('.act').forEach((b) => {
  b.onclick = () => send('playback_action', { action: b.dataset.a, position: 0, server_time: Date.now() });
});
</script></body></html>`;
