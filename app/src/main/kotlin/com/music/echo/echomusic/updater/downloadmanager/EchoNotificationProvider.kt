package iad1tya.echo.music.echomusic.updater.downloadmanager

import android.app.Notification
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import iad1tya.echo.music.R
import java.util.Locale


@OptIn(UnstableApi::class)
class EchoNotificationProvider(
    private val context: Context,
    notificationIdProvider: DefaultMediaNotificationProvider.NotificationIdProvider,
    channelId: String,
    channelNameResourceId: Int,
) : MediaNotification.Provider {

    private val defaultProvider = DefaultMediaNotificationProvider(
        context,
        notificationIdProvider,
        channelId,
        channelNameResourceId
    )

    fun setSmallIcon(iconResId: Int): EchoNotificationProvider {
        defaultProvider.setSmallIcon(iconResId)
        return this
    }

    override fun createNotification(
        mediaSession: MediaSession,
        customLayout: ImmutableList<CommandButton>,
        actionFactory: MediaNotification.ActionFactory,
        onNotificationChangedCallback: MediaNotification.Provider.Callback,
    ): MediaNotification {
        val mediaNotification = defaultProvider.createNotification(
            mediaSession,
            customLayout,
            actionFactory,
            onNotificationChangedCallback
        )

        val player = mediaSession.player
        val shouldBeOngoing = player.playWhenReady &&
            player.playbackState != Player.STATE_IDLE &&
            player.playbackState != Player.STATE_ENDED

        val builder = Notification.Builder.recoverBuilder(context, mediaNotification.notification)
            .setOngoing(shouldBeOngoing)
            .setCategory(Notification.CATEGORY_TRANSPORT)
            .setSmallIcon(R.drawable.ic_launcher_nobg)

        if (!shouldBeOngoing) {
            builder.setUsesChronometer(false)
            builder.setShowWhen(false)
        }

        applyAndroid16MediaEnhancements(builder, player, shouldBeOngoing)

        val updatedNotification = builder.build()
        copyMediaSessionToken(mediaNotification.notification, updatedNotification)

        return MediaNotification(mediaNotification.notificationId, updatedNotification)
    }

    override fun handleCustomCommand(session: MediaSession, action: String, extras: Bundle): Boolean =
        defaultProvider.handleCustomCommand(session, action, extras)

    private fun applyAndroid16MediaEnhancements(
        builder: Notification.Builder,
        player: Player,
        shouldBeOngoing: Boolean,
    ) {
        val isAndroid16 = Build.VERSION.SDK_INT >= 36 || Build.VERSION.CODENAME == "Baklava"
        if (!isAndroid16) return

        builder.setColorized(false)
        setRequestPromotedOngoingSafely(builder, shouldBeOngoing)
        builder.extras.putBoolean("android.requestPromotedOngoing", shouldBeOngoing)

        val durationMs = player.duration
        val currentPosMs = player.currentPosition
        val formattedTime = if (durationMs != C.TIME_UNSET && durationMs > 0) {
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
        } else {
            null
        }

        if (shouldBeOngoing) {
            setShortCriticalTextSafely(builder, formattedTime ?: context.getString(R.string.playing_status))

            if (durationMs != C.TIME_UNSET && durationMs > 0) {
                val remainingMs = (durationMs - currentPosMs).coerceAtLeast(0L)
                val endTime = System.currentTimeMillis() + remainingMs
                builder.setWhen(endTime)
                builder.setUsesChronometer(true)
                setChronometerCountDownSafely(builder, true)
            } else {
                builder.setWhen(System.currentTimeMillis())
                builder.setShowWhen(true)
            }
        } else {
            setShortCriticalTextSafely(builder, formattedTime ?: context.getString(R.string.paused_status))
            builder.setShowWhen(false)
            builder.setUsesChronometer(false)
        }
    }

    private fun copyMediaSessionToken(source: Notification, target: Notification) {
        if (Build.VERSION.SDK_INT >= 33) {
            source.extras.getParcelable(
                Notification.EXTRA_MEDIA_SESSION,
                android.media.session.MediaSession.Token::class.java
            )?.let {
                target.extras.putParcelable(Notification.EXTRA_MEDIA_SESSION, it)
            }
        } else {
            @Suppress("DEPRECATION")
            source.extras.getParcelable<android.media.session.MediaSession.Token>(
                Notification.EXTRA_MEDIA_SESSION
            )?.let {
                target.extras.putParcelable(Notification.EXTRA_MEDIA_SESSION, it)
            }
        }
    }

    private fun setShortCriticalTextSafely(builder: Notification.Builder, text: String) {
        try {
            val method = Notification.Builder::class.java.getMethod("setShortCriticalText", CharSequence::class.java)
            method.invoke(builder, text)
        } catch (e: Exception) {
            builder.extras.putCharSequence("android.shortCriticalText", text)
        }
    }

    private fun setChronometerCountDownSafely(builder: Notification.Builder, countDown: Boolean) {
        try {
            val method = Notification.Builder::class.java.getMethod(
                "setChronometerCountDown",
                Boolean::class.javaPrimitiveType
            )
            method.invoke(builder, countDown)
        } catch (e: Exception) {
            builder.extras.putBoolean("android.chronometerCountDown", countDown)
        }
    }

    private fun setRequestPromotedOngoingSafely(builder: Notification.Builder, promoted: Boolean) {
        val methodNames = arrayOf("setRequestPromotedOngoing", "setPromotedOngoing", "setOngoingActivity")
        for (name in methodNames) {
            try {
                val method = Notification.Builder::class.java.getMethod(name, Boolean::class.javaPrimitiveType)
                method.invoke(builder, promoted)
                return
            } catch (e: Exception) {
                // Method is only available on some platform previews/releases.
            }
        }
    }
}
