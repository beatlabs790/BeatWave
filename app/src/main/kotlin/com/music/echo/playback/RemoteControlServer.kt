package iad1tya.echo.music.playback

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import iad1tya.echo.music.playback.MusicService
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CopyOnWriteArrayList
import timber.log.Timber

class RemoteControlServer(private val service: MusicService) {
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private var stateBroadcastJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeClients = CopyOnWriteArrayList<Socket>()

    private var nsdManager: NsdManager? = null
    private var registrationListener: NsdManager.RegistrationListener? = null

    fun start(port: Int = 8085) {
        if (serverSocket != null) return
        registerNsd(port)
        startStateBroadcasting()
        serverJob = scope.launch {
            try {
                serverSocket = ServerSocket(port)
                Timber.tag("RemoteServer").i("Server started on port $port")
                while (isActive) {
                    val clientSocket = serverSocket?.accept()
                    if (clientSocket != null) {
                        activeClients.add(clientSocket)
                        handleClient(clientSocket)
                        // Immediate broadcast on client connect
                        broadcastCurrentState()
                    }
                }
            } catch (e: Exception) {
                Timber.tag("RemoteServer").e(e, "Error in server socket loop")
            }
        }
    }

    private fun startStateBroadcasting() {
        stateBroadcastJob = scope.launch {
            while (isActive) {
                broadcastCurrentState()
                delay(1000L)
            }
        }
    }

    private fun broadcastCurrentState() {
        val clients = activeClients.filter { it.isConnected && !it.isClosed }
        if (clients.isEmpty()) return

        val player = service.player
        val metadata = service.currentMediaMetadata.value

        val stateJson = JSONObject().apply {
            put("type", "playback_state")
            put("title", metadata?.title ?: "")
            put("artist", metadata?.artists?.joinToString { it.name } ?: "")
            put("thumbnailUrl", metadata?.thumbnailUrl ?: "")
            val duration = if (player.duration > 0) player.duration else (metadata?.duration?.toLong()?.times(1000L) ?: 0L)
            put("duration", duration)
            put("position", player.currentPosition)
            put("isPlaying", player.isPlaying)
            put("volume", player.volume.toDouble())
        }.toString()

        for (client in clients) {
            try {
                val writer = PrintWriter(client.getOutputStream(), true)
                writer.println(stateJson)
            } catch (e: Exception) {
                activeClients.remove(client)
                runCatching { client.close() }
                Timber.tag("RemoteServer").w("Error sending state to client: ${e.message}")
            }
        }
    }

    private fun registerNsd(port: Int) {
        try {
            nsdManager = service.getSystemService(Context.NSD_SERVICE) as NsdManager
            val serviceInfo = NsdServiceInfo().apply {
                serviceName = "BeatWave-${android.os.Build.MODEL.replace(" ", "-")}"
                serviceType = "_beatwave._tcp"
                setPort(port)
            }
            registrationListener = object : NsdManager.RegistrationListener {
                override fun onServiceRegistered(nsdServiceInfo: NsdServiceInfo) {
                    Timber.tag("RemoteServer").i("NSD Service registered: ${nsdServiceInfo.serviceName}")
                }
                override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    Timber.tag("RemoteServer").e("NSD Registration failed: $errorCode")
                }
                override fun onServiceUnregistered(arg0: NsdServiceInfo) {
                    Timber.tag("RemoteServer").i("NSD Service unregistered")
                }
                override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    Timber.tag("RemoteServer").e("NSD Unregistration failed: $errorCode")
                }
            }
            nsdManager?.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
        } catch (e: Exception) {
            Timber.tag("RemoteServer").e(e, "Failed to register NSD service")
        }
    }

    private fun unregisterNsd() {
        try {
            if (nsdManager != null && registrationListener != null) {
                nsdManager?.unregisterService(registrationListener)
            }
        } catch (e: Exception) {
            Timber.tag("RemoteServer").e(e, "Failed to unregister NSD service")
        } finally {
            registrationListener = null
            nsdManager = null
        }
    }

    private fun handleClient(socket: Socket) {
        scope.launch {
            try {
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                while (socket.isConnected && !socket.isClosed) {
                    val line = reader.readLine() ?: break
                    val json = JSONObject(line)
                    val command = json.optString("command")
                    withContext(Dispatchers.Main) {
                        when (command) {
                            "play" -> service.player.play()
                            "pause" -> service.player.pause()
                            "next" -> service.player.seekToNext()
                            "prev" -> service.player.seekToPrevious()
                            "volume" -> {
                                val vol = json.optDouble("value", 1.0).toFloat()
                                service.player.volume = vol
                            }
                            "seek" -> {
                                val pos = json.optLong("value", 0L)
                                service.player.seekTo(pos)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.tag("RemoteServer").w("Client connection error: ${e.message}")
            } finally {
                activeClients.remove(socket)
                runCatching { socket.close() }
            }
        }
    }

    fun stop() {
        unregisterNsd()
        stateBroadcastJob?.cancel()
        serverJob?.cancel()
        activeClients.forEach { runCatching { it.close() } }
        activeClients.clear()
        runCatching { serverSocket?.close() }
        serverSocket = null
    }
}
