package iad1tya.echo.music.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import iad1tya.echo.music.LocalPlayerConnection
import iad1tya.echo.music.R
import iad1tya.echo.music.models.MediaMetadata
import timber.log.Timber

/**
 * "Now Casting" session sheet shown when the user taps the Cast button
 * while connected to a Cast device.
 *
 * Shows: device name, current track, and disconnect.
 * Volume control is handled by the player's own volume slider
 * (Apple Music Inspired mode) — not duplicated here.
 */
@Composable
fun CastSessionSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playerConnection = LocalPlayerConnection.current
    val castHandler = playerConnection?.service?.castConnectionHandler

    if (castHandler == null) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }

    // Cast state
    val isCasting by castHandler.isCasting.collectAsState()
    val castDeviceName by castHandler.castDeviceName.collectAsState()

    // Current media metadata
    val currentMetadata by playerConnection.mediaMetadata.collectAsState()

    // Auto-dismiss if disconnected
    if (!isCasting) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
    ) {
        // ── Header ──────────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.cast_connected),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Now Casting",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Device info card ────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.cast_connected),
                    contentDescription = null,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = castDeviceName ?: "Cast Device",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Connected",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Now playing ────────────────────────────────────────────
        if (currentMetadata != null) {
            NowPlayingCard(metadata = currentMetadata!!)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Disconnect button ──────────────────────────────────────
        TextButton(
            onClick = {
                Timber.d("User disconnecting from Cast")
                castHandler.disconnect()
                onDismiss()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.cast_connected),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Disconnect",
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun NowPlayingCard(
    metadata: MediaMetadata,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        // Track thumbnail placeholder
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.music_note),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = metadata.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = metadata.artists.joinToString(", ") { it.name },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
