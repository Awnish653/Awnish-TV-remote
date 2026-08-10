package com.example.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.util.Log
import com.example.data.models.ConnectionStatus
import com.example.data.models.TvDevice
import com.example.data.models.TvType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.Socket

class NetworkDeviceScanner(private val context: Context) {
    private val TAG = "NetworkDeviceScanner"
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private var multicastLock: WifiManager.MulticastLock? = null

    private val _discoveredDevices = MutableStateFlow<List<TvDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<TvDevice>> = _discoveredDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val activeDiscoveryListeners = mutableListOf<NsdManager.DiscoveryListener>()
    private var subnetScanJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    // Supported service types for TV discovery
    private val serviceTypes = listOf(
        "_androidtvremote2._tcp." to TvType.ANDROID_TV,
        "_googlecast._tcp." to TvType.ANDROID_TV,
        "_samsungconnect._tcp." to TvType.SAMSUNG_TIZEN,
        "_airplay._tcp." to TvType.GENERIC_SMART_TV
    )

    fun startScanning(includeDemoDevices: Boolean = false) {
        if (_isScanning.value) return
        _isScanning.value = true

        acquireMulticastLock()

        val currentList = mutableListOf<TvDevice>()
        if (includeDemoDevices) {
            currentList.addAll(getDemoTvDevices())
        }
        _discoveredDevices.value = currentList

        // Register NSD listeners for each service type
        serviceTypes.forEach { (serviceType, tvType) ->
            try {
                val listener = createDiscoveryListener(serviceType, tvType)
                activeDiscoveryListeners.add(listener)
                nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, listener)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start NSD for $serviceType: ${e.message}")
            }
        }

        // Active IP Subnet Sweep in background thread for fast detection
        subnetScanJob?.cancel()
        subnetScanJob = scope.launch {
            scanLocalSubnet(includeDemoDevices)
        }
    }

    fun stopScanning() {
        if (!_isScanning.value) return
        _isScanning.value = false

        subnetScanJob?.cancel()

        activeDiscoveryListeners.forEach { listener ->
            try {
                nsdManager.stopServiceDiscovery(listener)
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping NSD listener: ${e.message}")
            }
        }
        activeDiscoveryListeners.clear()
        releaseMulticastLock()
    }

    fun addManualDevice(ipAddress: String, name: String = "Manual TV", type: TvType = TvType.ANDROID_TV) {
        val newDevice = TvDevice(
            id = ipAddress,
            name = name.ifBlank { "Smart TV ($ipAddress)" },
            ipAddress = ipAddress,
            port = when (type) {
                TvType.ANDROID_TV -> 6466
                TvType.SAMSUNG_TIZEN -> 8001
                TvType.LG_WEBOS -> 3000
                TvType.GENERIC_SMART_TV -> 8008
            },
            type = type,
            connectionStatus = ConnectionStatus.DISCONNECTED
        )
        val updated = _discoveredDevices.value.toMutableList()
        if (updated.none { it.ipAddress == ipAddress }) {
            updated.add(0, newDevice)
            _discoveredDevices.value = updated
        }
    }

    private fun createDiscoveryListener(serviceType: String, defaultTvType: TvType): NsdManager.DiscoveryListener {
        return object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Log.d(TAG, "Service discovery started for $regType")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                Log.d(TAG, "Service found: ${service.serviceName} (${service.serviceType})")
                resolveService(service, defaultTvType)
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                Log.d(TAG, "Service lost: ${service.serviceName}")
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.d(TAG, "Discovery stopped for $serviceType")
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Discovery failed for $serviceType with code $errorCode")
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Stop discovery failed for $serviceType with code $errorCode")
            }
        }
    }

    private fun resolveService(serviceInfo: NsdServiceInfo, tvType: TvType) {
        try {
            nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    Log.e(TAG, "Resolve failed for ${serviceInfo.serviceName}: $errorCode")
                }

                override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                    val host = serviceInfo.host ?: return
                    val ipAddress = host.hostAddress ?: return
                    val name = cleanDeviceName(serviceInfo.serviceName)

                    val device = TvDevice(
                        id = ipAddress,
                        name = name,
                        ipAddress = ipAddress,
                        port = serviceInfo.port,
                        type = tvType,
                        connectionStatus = ConnectionStatus.DISCONNECTED,
                        modelName = serviceInfo.serviceType
                    )

                    addOrUpdateDiscoveredDevice(device)
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving service: ${e.message}")
        }
    }

    private fun addOrUpdateDiscoveredDevice(device: TvDevice) {
        val current = _discoveredDevices.value.toMutableList()
        val index = current.indexOfFirst { it.ipAddress == device.ipAddress || it.id == device.id }
        if (index >= 0) {
            current[index] = device
        } else {
            current.add(device)
        }
        _discoveredDevices.value = current
    }

    private suspend fun scanLocalSubnet(includeDemoDevices: Boolean) {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return
        val dhcp = wifiManager.dhcpInfo ?: return
        val ip = dhcp.ipAddress
        if (ip == 0) return

        val myIpStr = String.format(
            "%d.%d.%d.%d",
            ip and 0xff,
            ip shr 8 and 0xff,
            ip shr 16 and 0xff,
            ip shr 24 and 0xff
        )

        val prefix = myIpStr.substringBeforeLast(".")
        val myLastOctet = myIpStr.substringAfterLast(".").toIntOrNull() ?: 0

        // Probe standard TV ports on local subnet (e.g. 6466 for Android TV, 8008 for Cast, 8001 for Samsung)
        val portsToProbe = listOf(6466, 8008, 8001, 3000)

        for (i in 1..254) {
            if (i == myLastOctet) continue
            val targetIp = "$prefix.$i"

            for (port in portsToProbe) {
                if (!_isScanning.value) return
                try {
                    val socket = Socket()
                    socket.connect(java.net.InetSocketAddress(targetIp, port), 120)
                    socket.close()

                    val tvType = when (port) {
                        6466 -> TvType.ANDROID_TV
                        8001 -> TvType.SAMSUNG_TIZEN
                        3000 -> TvType.LG_WEBOS
                        else -> TvType.ANDROID_TV
                    }

                    val detectedDevice = TvDevice(
                        id = targetIp,
                        name = "${tvType.displayName} ($targetIp)",
                        ipAddress = targetIp,
                        port = port,
                        type = tvType,
                        connectionStatus = ConnectionStatus.DISCONNECTED
                    )
                    withContext(Dispatchers.Main) {
                        addOrUpdateDiscoveredDevice(detectedDevice)
                    }
                    break
                } catch (ignored: Exception) {
                    // Port not open or device offline
                }
            }
        }
    }

    private fun cleanDeviceName(rawName: String): String {
        return rawName.replace("\\\\032", " ")
            .replace("-", " ")
            .replace("_", " ")
            .trim()
            .ifEmpty { "Living Room TV" }
    }

    private fun acquireMulticastLock() {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            multicastLock = wifiManager?.createMulticastLock("AWNISHTVRemoteMulticastLock")?.apply {
                setReferenceCounted(true)
                acquire()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Multicast lock error: ${e.message}")
        }
    }

    private fun releaseMulticastLock() {
        try {
            multicastLock?.let {
                if (it.isHeld) it.release()
            }
            multicastLock = null
        } catch (e: Exception) {
            Log.e(TAG, "Multicast release error: ${e.message}")
        }
    }

    private fun getDemoTvDevices(): List<TvDevice> {
        return listOf(
            TvDevice(
                id = "demo_android_tv",
                name = "AWNISH Android TV 4K",
                ipAddress = "192.168.1.105",
                port = 6466,
                type = TvType.ANDROID_TV,
                isAuthorized = true,
                modelName = "Android TV 12 (Google TV)"
            ),
            TvDevice(
                id = "demo_samsung_tv",
                name = "Living Room Samsung QLED",
                ipAddress = "192.168.1.112",
                port = 8001,
                type = TvType.SAMSUNG_TIZEN,
                isAuthorized = false,
                modelName = "Samsung Tizen OS 7.0"
            ),
            TvDevice(
                id = "demo_lg_tv",
                name = "Bedroom LG OLED TV",
                ipAddress = "192.168.1.120",
                port = 3000,
                type = TvType.LG_WEBOS,
                isAuthorized = false,
                modelName = "LG webOS 23"
            )
        )
    }
}
