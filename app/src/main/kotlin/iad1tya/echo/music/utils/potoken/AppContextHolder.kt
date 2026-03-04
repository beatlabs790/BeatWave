package iad1tya.echo.music.utils.potoken

import android.content.Context

object AppContextHolder {
    lateinit var appContext: Context
        private set

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }
}
