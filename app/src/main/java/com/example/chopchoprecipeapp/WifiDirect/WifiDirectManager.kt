package com.example.chopchoprecipeapp.wifidirect

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.Channel
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages WiFi Direct (P2P) connections and device discovery.
 * @author Amelie Dzierzawa
 */
class WiFiDirectManager(private val context: Context) {

    companion object {
        private const val TAG = "WiFiDirect"
    }

    private val wifiP2pManager: WifiP2pManager =
        context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
    private lateinit var channel: Channel
    private val handler = Handler(Looper.getMainLooper())

    private val _availablePeers = MutableStateFlow<List<WifiP2pDevice>>(emptyList())
    val availablePeers: StateFlow<List<WifiP2pDevice>> = _availablePeers.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    private val _connectionInfo = MutableStateFlow<WifiP2pInfo?>(null)
    val connectionInfo: StateFlow<WifiP2pInfo?> = _connectionInfo.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val broadcastReceiver = WiFiDirectBroadcastReceiver(this)
    private val intentFilter = IntentFilter().apply {
        addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
    }

    private var discoveryRunnable: Runnable? = null

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(broadcastReceiver, intentFilter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(broadcastReceiver, intentFilter)
        }
        channel = wifiP2pManager.initialize(context, Looper.getMainLooper(), null)
        Log.d(TAG, "WiFiDirectManager initialized")
    }


    /**
     * Starts WiFi Direct peer discovery (periodically).
     */
    fun discoverPeers() {
        if (ActivityCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "Missing LOCATION permission")
            return
        }

        _isDiscovering.value = true
        Log.d(TAG, "Starting peer discovery")

        wifiP2pManager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "Discovery started successfully")
            }

            override fun onFailure(reasonCode: Int) {
                Log.e(TAG, "Discovery failed: reason code $reasonCode")
                _isDiscovering.value = false
            }
        })

        discoveryRunnable = Runnable {
            if (_isDiscovering.value) {
                Log.d(TAG, "Restarting discovery (periodic)")
                wifiP2pManager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {}
                    override fun onFailure(reasonCode: Int) {
                        Log.e(TAG, "Periodic discovery failed: $reasonCode")
                    }
                })
                handler.postDelayed(discoveryRunnable!!, 15000)
            }
        }
        handler.postDelayed(discoveryRunnable!!, 15000)
    }

    /**
     * Stops WiFi Direct peer discovery.
     */
    fun stopDiscovery() {
        _isDiscovering.value = false
        discoveryRunnable?.let { handler.removeCallbacks(it) }

        if (ActivityCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        wifiP2pManager.stopPeerDiscovery(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "Discovery stopped")
            }

            override fun onFailure(reasonCode: Int) {
                Log.e(TAG, "Stop discovery failed: $reasonCode")
            }
        })
    }

    /**
     * Connects to a specific peer device.
     */
    fun connectToPeer(device: WifiP2pDevice) {
        if (ActivityCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val config = WifiP2pConfig().apply {
            deviceAddress = device.deviceAddress
        }

        Log.d(TAG, "Connecting to ${device.deviceName} (${device.deviceAddress})")

        wifiP2pManager.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "Connect request sent")
            }

            override fun onFailure(reasonCode: Int) {
                Log.e(TAG, "Connect failed: reason code $reasonCode")
            }
        })
    }

    /**
     * Disconnects from current peer.
     */
    fun disconnect() {
        if (ActivityCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        wifiP2pManager.removeGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "Disconnected")
                _isConnected.value = false
                _connectionInfo.value = null
            }

            override fun onFailure(reasonCode: Int) {
                Log.e(TAG, "Disconnect failed: $reasonCode")
            }
        })
    }

    /**
     * Updates peer list (called by BroadcastReceiver).
     */
    internal fun updatePeerList(peers: WifiP2pDeviceList) {
        _availablePeers.value = peers.deviceList.toList()
        Log.d(TAG, "Peer list updated: ${peers.deviceList.size} devices found")
        peers.deviceList.forEach { device ->
            Log.d(TAG, "  - ${device.deviceName} (${device.deviceAddress})")
        }
    }

    /**
     * Updates connection info (called by BroadcastReceiver).
     */
    internal fun updateConnectionInfo(info: WifiP2pInfo) {
        _connectionInfo.value = info
        _isConnected.value = info.groupFormed
        Log.d(
            TAG,
            "Connection info updated: groupFormed=${info.groupFormed}, GO=${info.isGroupOwner}"
        )
    }

    /**
     * Stops discovery and cleanup.
     */
    fun cleanup() {
        stopDiscovery()
        try {
            context.unregisterReceiver(broadcastReceiver)
            Log.d(TAG, "WiFiDirectManager cleaned up")
        } catch (e: Exception) {
            //already unregistered
        }
    }
}

/**
 * BroadcastReceiver for WiFi Direct events.
 */
class WiFiDirectBroadcastReceiver(private val manager: WiFiDirectManager) : BroadcastReceiver() {

    companion object {
        private const val TAG = "WiFiDirect-BR"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "Broadcast received: ${intent?.action}")

        when (intent?.action) {
            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                Log.d(TAG, "WiFi P2P state: $state")
            }

            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                //Peer list changed
                val peersExtra = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent?.getParcelableExtra(
                        WifiP2pManager.EXTRA_P2P_DEVICE_LIST,
                        WifiP2pDeviceList::class.java
                    )
                } else {
                    @Suppress("DEPRECATION")
                    intent?.getParcelableExtra(WifiP2pManager.EXTRA_P2P_DEVICE_LIST)
                }
                peersExtra?.let { manager.updatePeerList(it) }
            }

            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                //Connection state changed
                val connInfoExtra = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent?.getParcelableExtra(
                        WifiP2pManager.EXTRA_WIFI_P2P_INFO,
                        WifiP2pInfo::class.java
                    )
                } else {
                    @Suppress("DEPRECATION")
                    intent?.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO)
                }
                connInfoExtra?.let { manager.updateConnectionInfo(it) }
            }

            WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent?.getParcelableExtra(
                        WifiP2pManager.EXTRA_WIFI_P2P_DEVICE,
                        WifiP2pDevice::class.java
                    )
                } else {
                    @Suppress("DEPRECATION")
                    intent?.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE)
                }
                Log.d(TAG, "This device: ${device?.deviceName}")
            }
        }
    }
}