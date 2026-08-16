/**
 * BeatWave Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.beatwave.music.listentogether

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import kotlinx.coroutines.*
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket

class LocalSocketTransport(
    private val context: Context,
    private val onMessageReceived: (ByteArray) -> Unit,
    private val onConnectionStateChanged: (ConnectionState) -> Unit,
    private val onLog: (String) -> Unit
) {
    private val serviceType = "_beatwave-sync._tcp"
    private val serviceName = "BeatWaveOffline"
    private val port = 8282

    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var nsdManager: NsdManager? = null
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var readJob: Job? = null
    private var acceptJob: Job? = null

    var isHost = false
        private set

    init {
        nsdManager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager
    }

    fun startHost() {
        isHost = true
        onConnectionStateChanged(ConnectionState.CONNECTING)
        scope.launch {
            try {
                serverSocket = ServerSocket(0)
                val assignedPort = serverSocket!!.localPort
                registerService(assignedPort)
                onLog("Local Server started on port $assignedPort. Advertising via NSD...")
                
                // Immediately notify that we are connected/hosting so the UI can show the room
                onConnectionStateChanged(ConnectionState.CONNECTED)
                
                acceptJob = launch {
                    while (isActive) {
                        try {
                            val socket = serverSocket?.accept() ?: break
                            onLog("Client connected from ${socket.inetAddress}")
                            clientSocket = socket
                            startReading(socket)
                            // Note: we only support 1 client at a time currently
                            // We don't need to call onConnectionStateChanged here because the Host is already CONNECTED
                        } catch (e: Exception) {
                            if (!isActive) break
                        }
                    }
                }
            } catch (e: Exception) {
                onLog("Error hosting server: ${e.message}")
                onConnectionStateChanged(ConnectionState.DISCONNECTED)
            }
        }
    }

    fun startDiscovery(onDeviceDiscovered: (String, String) -> Unit) {
        onLog("Starting network discovery...")
        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
                onLog("Discovery start failed: $errorCode")
            }

            override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
                onLog("Discovery stop failed: $errorCode")
            }

            override fun onDiscoveryStarted(serviceType: String?) {
                onLog("Discovery started")
            }

            override fun onDiscoveryStopped(serviceType: String?) {
                onLog("Discovery stopped")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo?) {
                if (serviceInfo == null) return
                if (serviceInfo.serviceType == serviceType) {
                    nsdManager?.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                            onLog("Resolve failed: $errorCode")
                        }

                        override fun onServiceResolved(resolvedServiceInfo: NsdServiceInfo?) {
                            if (resolvedServiceInfo == null) return
                            val hostIp = resolvedServiceInfo.host.hostAddress
                            val resolvedPort = resolvedServiceInfo.port
                            val name = resolvedServiceInfo.serviceName
                            onLog("Service resolved: $name at $hostIp:$resolvedPort")
                            if (hostIp != null) {
                                onDeviceDiscovered(name, "$hostIp:$resolvedPort")
                            }
                        }
                    })
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo?) {
                onLog("Service lost: ${serviceInfo?.serviceName}")
            }
        }
        nsdManager?.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    fun stopDiscovery() {
        try {
            if (discoveryListener != null) {
                nsdManager?.stopServiceDiscovery(discoveryListener)
                discoveryListener = null
            }
        } catch (e: Exception) {}
    }

    fun connectToHost(hostIp: String, hostPort: Int = this.port) {
        isHost = false
        onConnectionStateChanged(ConnectionState.CONNECTING)
        scope.launch {
            try {
                onLog("Connecting to host at $hostIp:$hostPort...")
                val socket = Socket(hostIp, hostPort)
                clientSocket = socket
                onLog("Connected to host!")
                onConnectionStateChanged(ConnectionState.CONNECTED)
                startReading(socket)
            } catch (e: Exception) {
                onLog("Connection failed: ${e.message}")
                onConnectionStateChanged(ConnectionState.DISCONNECTED)
            }
        }
    }

    fun send(data: ByteArray): Boolean {
        val socket = clientSocket ?: return false
        if (socket.isClosed) return false
        return try {
            val out = DataOutputStream(socket.getOutputStream())
            out.writeInt(data.size)
            out.write(data)
            out.flush()
            true
        } catch (e: Exception) {
            onLog("Send error: ${e.message}")
            false
        }
    }

    private fun startReading(socket: Socket) {
        readJob?.cancel()
        readJob = scope.launch {
            try {
                val input = DataInputStream(socket.getInputStream())
                while (isActive) {
                    val size = input.readInt()
                    if (size <= 0) break
                    val buffer = ByteArray(size)
                    input.readFully(buffer)
                    onMessageReceived(buffer)
                }
            } catch (e: Exception) {
                onLog("Read disconnected: ${e.message}")
            } finally {
                disconnect()
            }
        }
    }

    private fun registerService(port: Int) {
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = this@LocalSocketTransport.serviceName
            serviceType = this@LocalSocketTransport.serviceType
            setPort(port)
        }
        registrationListener = object : NsdManager.RegistrationListener {
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                onLog("Registration failed: $errorCode")
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                onLog("Unregistration failed: $errorCode")
            }

            override fun onServiceRegistered(serviceInfo: NsdServiceInfo?) {
                onLog("Service registered successfully: ${serviceInfo?.serviceName}")
            }

            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo?) {
                onLog("Service unregistered successfully")
            }
        }
        nsdManager?.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
    }

    fun disconnect() {
        onConnectionStateChanged(ConnectionState.DISCONNECTED)
        readJob?.cancel()
        acceptJob?.cancel()
        
        try {
            clientSocket?.close()
        } catch (e: IOException) {}
        clientSocket = null

        try {
            serverSocket?.close()
        } catch (e: IOException) {}
        serverSocket = null

        try {
            if (registrationListener != null) {
                nsdManager?.unregisterService(registrationListener)
                registrationListener = null
            }
        } catch (e: Exception) {}
        
        stopDiscovery()
    }
}
