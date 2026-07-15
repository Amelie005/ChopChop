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
 * Manages WiFi direct (P2P) connections and device discovery.
 * @param context The application context.
 * @author Amelie Dzierzawa
 */
class WiFiDirectManager(private val context: Context) {

    /**
     * Constants for logging and debugging.
     */
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

    private val _thisDevice = MutableStateFlow<WifiP2pDevice?>(null)
    val thisDevice: StateFlow<WifiP2pDevice?> = _thisDevice.asStateFlow()

    private val broadcastReceiver = WiFiDirectBroadcastReceiver(this)

    /**
     * Intent filter for the broadcast receiver
     */
    private val intentFilter = IntentFilter().apply {
        addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
    }

    private var discoveryRunnable: Runnable? = null

    /**
     * Initializes the WiFi Direct manager.
     */
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
     * Starts the peer discovery process.
     *
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

        //restart periodically
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
     * Stops the peer discovery process.
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
     * Connects to the specified WiFi Direct device.
     * @param device The device to connect to.
     */
    fun connectToPeer(device: WifiP2pDevice) {
        if (ActivityCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "Missing LOCATION permission for connect!")
            return
        }

        val config = WifiP2pConfig().apply {
            deviceAddress = device.deviceAddress
        }

        Log.d(TAG, "ATTEMPTING CONNECTION to ${device.deviceName} (${device.deviceAddress})")

        wifiP2pManager.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "Connect request SENT successfully")
            }

            override fun onFailure(reasonCode: Int) {
                Log.e(TAG, "Connect FAILED: reason code $reasonCode")
            }
        })
    }

    /**
     * Disconnects from the current WiFi Direct connection.
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
     * Updates the list of available WiFi Direct peers.
     * @param peers The list of available peers.
     */
    internal fun updatePeerList(peers: WifiP2pDeviceList) {
        _availablePeers.value = peers.deviceList.toList()
        Log.d(TAG, "📱 Peer list updated: ${peers.deviceList.size} devices")
        peers.deviceList.forEach { device ->
            Log.d(TAG, "  - ${device.deviceName} (${device.deviceAddress}) Status=${device.status}")
        }
    }

    /**
     * Updates the connection information.
     * @param info The connection information.
     */
    internal fun updateConnectionInfo(info: WifiP2pInfo) {
        _connectionInfo.value = info
        _isConnected.value = info.groupFormed
        Log.d(TAG, "🔌 Connection: groupFormed=${info.groupFormed}, GO=${info.isGroupOwner}, IP=${info.groupOwnerAddress?.hostAddress}")
    }

    /**
     * Updates the device information.
     * @param device The device information.
     */
    internal fun updateThisDevice(device: WifiP2pDevice) {
        _thisDevice.value = device
        Log.d(TAG, "This device: ${device.deviceName} (${device.deviceAddress}) Status=${device.status}")
    }

    /**
     * Cleans up resources.
     */
    fun cleanup() {
        stopDiscovery()
        try {
            context.unregisterReceiver(broadcastReceiver)
            Log.d(TAG, "WiFiDirectManager cleaned up")
        } catch (e: Exception) {
            // already unregistered
        }
    }
}

/**
 * Broadcast receiver for WiFi Direct events.
 * @param manager The WiFi Direct manager.
 */
class WiFiDirectBroadcastReceiver(private val manager: WiFiDirectManager) : BroadcastReceiver() {

    /**
     * Constants for logging and debugging.
     */
    companion object {
        private const val TAG = "WiFiDirect-BR"
    }

    /**
     * Handles incoming broadcast intents.
     * @param context The application context.
     * @param intent The broadcast intent.
     */
    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                Log.d(TAG, "WiFi P2P State: $state")
            }

            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                val connInfoExtra = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent?.getParcelableExtra(
                        WifiP2pManager.EXTRA_WIFI_P2P_INFO,
                        WifiP2pInfo::class.java
                    )
                } else {
                    @Suppress("DEPRECATION") //Suppress deprecation warning
                    intent?.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO)
                }
                connInfoExtra?.let { manager.updateConnectionInfo(it) }
            }

            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
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
                device?.let { manager.updateThisDevice(it) }
            }
        }
    }
}