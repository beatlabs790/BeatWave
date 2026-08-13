package iad1tya.echo.music.playback

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import iad1tya.echo.music.playback.MusicService
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import timber.log.Timber

class RemoteControlServer(private val service: MusicService) {
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var nsdManager: NsdManager? = null
    private var registrationListener: NsdManager.RegistrationListener? = null

    fun start(port: Int = 8085) {
        if (serverSocket != null) return
        registerNsd(port)
        serverJob = scope.launch {
            try {
                serverSocket = ServerSocket(port)
                Timber.tag("RemoteServer").i("Server started on port $port")
                while (isActive) {
                    val clientSocket = serverSocket?.accept()
                    if (clientSocket != null) {
                        handleClient(clientSocket)
                    }
                }
            } catch (e: Exception) {
                Timber.tag("RemoteServer").e(e, "Error in server socket loop")
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
                runCatching { socket.close() }
            }
        }
    }

    fun stop() {
        unregisterNsd()
        serverJob?.cancel()
        runCatching { serverSocket?.close() }
        serverSocket = null
    }
}
