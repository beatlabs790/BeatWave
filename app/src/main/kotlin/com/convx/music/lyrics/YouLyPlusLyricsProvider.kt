/**
 * BeatWave Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.convx.music.lyrics

import android.content.Context
import com.music.youlyplus.YouLyPlus
import com.convx.music.constants.EnableYouLyPlusKey
import com.convx.music.utils.dataStore
import com.convx.music.utils.get

object YouLyPlusLyricsProvider : LyricsProvider {
    override val name = "YouLyPlus"

    override fun isEnabled(context: Context): Boolean =
        context.dataStore[EnableYouLyPlusKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
    ): Result<String> = YouLyPlus.getLyrics(title, artist, duration, album, id)

    override suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
        callback: (String) -> Unit,
    ) {
        YouLyPlus.getAllLyrics(title, artist, duration, album, id, null, callback)
    }
}

