package iad1tya.echo.music.playback

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.PrintWriter
import java.net.Socket
import timber.log.Timber

object RemoteControlClient {
    private var socket: Socket? = null
    private var writer: PrintWriter? = null

    suspend fun connect(ip: String, port: Int = 8085): Boolean = withContext(Dispatchers.IO) {
        try {
            disconnect()
            socket = Socket(ip, port)
            writer = PrintWriter(socket?.getOutputStream(), true)
            Timber.tag("RemoteClient").i("Connected to server at $ip:$port")
            true
        } catch (e: Exception) {
            Timber.tag("RemoteClient").e(e, "Connection failed to $ip:$port")
            false
        }
    }

    suspend fun sendCommand(command: String, value: Any? = null) = withContext(Dispatchers.IO) {
        val currentWriter = writer
        if (currentWriter != null && socket?.isConnected == true) {
            try {
                val json = JSONObject().apply {
                    put("command", command)
                    if (value != null) {
                        put("value", value)
                    }
                }
                currentWriter.println(json.toString())
            } catch (e: Exception) {
                Timber.tag("RemoteClient").e(e, "Failed to send command $command")
            }
        }
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        runCatching { writer?.close() }
        runCatching { socket?.close() }
        writer = null
        socket = null
    }
}
