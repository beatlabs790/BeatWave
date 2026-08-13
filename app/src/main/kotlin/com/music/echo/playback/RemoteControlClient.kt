package iad1tya.echo.music.playback

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.PrintWriter
import java.net.Socket
import timber.log.Timber

object RemoteControlClient {
    private var socket: Socket? = null
    private var writer: PrintWriter? = null

    data class DiscoveredDevice(val name: String, val ip: String, val port: Int)
    val discoveredDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())

    private var nsdManager: NsdManager? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    fun startDiscovery(context: Context) {
        if (nsdManager != null) return
        discoveredDevices.value = emptyList()
        nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Timber.tag("RemoteClient").e("NSD Discovery start failed: $errorCode")
                stopDiscovery()
            }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Timber.tag("RemoteClient").e("NSD Discovery stop failed: $errorCode")
                stopDiscovery()
            }
            override fun onDiscoveryStarted(regType: String) {
                Timber.tag("RemoteClient").i("NSD Discovery started")
            }
            override fun onDiscoveryStopped(serviceType: String) {
                Timber.tag("RemoteClient").i("NSD Discovery stopped")
            }
            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                Timber.tag("RemoteClient").i("NSD Service found: ${serviceInfo.serviceName}")
                if (serviceInfo.serviceType == "_beatwave._tcp.") {
                    nsdManager?.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                            Timber.tag("RemoteClient").e("NSD Resolve failed: $errorCode")
                        }
                        override fun onServiceResolved(resolvedInfo: NsdServiceInfo) {
                            Timber.tag("RemoteClient").i("NSD Service resolved: ${resolvedInfo.host.hostAddress}:${resolvedInfo.port}")
                            val device = DiscoveredDevice(
                                name = resolvedInfo.serviceName.replace("-", " "),
                                ip = resolvedInfo.host.hostAddress ?: "",
                                port = resolvedInfo.port
                            )
                            discoveredDevices.value = (discoveredDevices.value + device).distinctBy { it.ip }
                        }
                    })
                }
            }
            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                Timber.tag("RemoteClient").i("NSD Service lost: ${serviceInfo.serviceName}")
                discoveredDevices.value = discoveredDevices.value.filterNot { it.name == serviceInfo.serviceName.replace("-", " ") }
            }
        }
        try {
            nsdManager?.discoverServices("_beatwave._tcp", NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            Timber.tag("RemoteClient").e(e, "Error starting NSD discovery")
        }
    }

    fun stopDiscovery() {
        try {
            if (nsdManager != null && discoveryListener != null) {
                nsdManager?.stopServiceDiscovery(discoveryListener)
            }
        } catch (e: Exception) {
            Timber.tag("RemoteClient").e(e, "Error stopping NSD discovery")
        } finally {
            discoveryListener = null
            nsdManager = null
        }
    }

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
