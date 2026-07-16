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

    //Wifi Direct API
    private val wifiP2pManager: WifiP2pManager =
        context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager

    //Channel to communicate with the WiFi Direct API
    private lateinit var channel: Channel

    //Handler for asynchronous operations
    private val handler = Handler(Looper.getMainLooper())

    //Flow for available WiFi Direct peers
    private val _availablePeers = MutableStateFlow<List<WifiP2pDevice>>(emptyList())

    //State flow for available WiFi Direct peers
    val availablePeers: StateFlow<List<WifiP2pDevice>> = _availablePeers.asStateFlow()

    //Flow for the current WiFi Direct connection status
    private val _isDiscovering = MutableStateFlow(false)

    //State flow for the current WiFi Direct connection status
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    //Flow for the current WiFi Direct connection information
    private val _connectionInfo = MutableStateFlow<WifiP2pInfo?>(null)

    //State flow for the current WiFi Direct connection information
    val connectionInfo: StateFlow<WifiP2pInfo?> = _connectionInfo.asStateFlow()

    //Flow for the current WiFi Direct device information
    private val _isConnected = MutableStateFlow(false)

    //State flow for the current WiFi Direct device information
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    //Flow for the current WiFi Direct device information
    private val _thisDevice = MutableStateFlow<WifiP2pDevice?>(null)

    //State flow for the current WiFi Direct device information
    val thisDevice: StateFlow<WifiP2pDevice?> = _thisDevice.asStateFlow()

    //Broadcast receiver for WiFi Direct events
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

    //Runnable for periodic discovery
    private var discoveryRunnable: Runnable? = null

    /**
     * Initializes the WiFi Direct manager.
     */
    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { //if API >= 33
            context.registerReceiver(broadcastReceiver, intentFilter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(broadcastReceiver, intentFilter)
        }
        channel = wifiP2pManager.initialize(context, Looper.getMainLooper(), null)
        Log.d(TAG, "WiFiDirectManager was initialized")
    }

    /**
     * Starts the peer discovery process.
     */
    fun discoverPeers() {

        Log.d(TAG, "discoverPeers() was called")

        stopDiscovery() //removes groups before discovering new ones

        if (ActivityCompat.checkSelfPermission( //check permissions
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "Missing LOCATION permission")
            return
        }

        _isDiscovering.value = true //start discovery
        Log.d(TAG, "Starting peer discovery!")

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
                    override fun onSuccess() {
                        Log.e(TAG, "Periodic discovery successful!")
                    }
                    override fun onFailure(reasonCode: Int) {
                        Log.e(TAG, "Periodic discovery failed: $reasonCode")
                    }
                })
                handler.postDelayed(discoveryRunnable!!, 15000) //restart every 15 seconds
            }
        }
        handler.postDelayed(discoveryRunnable!!, 15000)
    }

    /**
     * Stops the peer discovery process.
     */
    fun stopDiscovery() {
        _isDiscovering.value = false //stop discovery
        discoveryRunnable?.let { handler.removeCallbacks(it) } //stop periodic discovery

        if (ActivityCompat.checkSelfPermission( //check permissions
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        wifiP2pManager.stopPeerDiscovery(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "Discovery was stopped")
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
        if (ActivityCompat.checkSelfPermission( //check permissions
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "Missing LOCATION permission for connect!")
            return
        }

        val config = WifiP2pConfig().apply { //create config
            deviceAddress = device.deviceAddress
        }

        Log.d(TAG, "Attempting connection to ${device.deviceName} (${device.deviceAddress})")

        wifiP2pManager.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "Connect request sent successfully")
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
        if (ActivityCompat.checkSelfPermission( //check permissions
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        //leave group
        wifiP2pManager.removeGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "Disconnected")

                _isConnected.value = false
                _connectionInfo.value = null
                _availablePeers.value = emptyList()

                discoverPeers() //restart discovery
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
        _availablePeers.value = peers.deviceList.toList() //update list
        Log.d(TAG, "Peer list updated: ${peers.deviceList.size} devices")
        peers.deviceList.forEach { device -> //print list
            Log.d(TAG, "  - ${device.deviceName} (${device.deviceAddress}) Status=${device.status}")
        }
    }

    /**
     * Updates the connection information.
     * @param info The connection information.
     */
    internal fun updateConnectionInfo(info: WifiP2pInfo) {
        _connectionInfo.value = info //update connection
        _isConnected.value = info.groupFormed //update connection status
        Log.d(
            TAG,
            "Connection: groupFormed=${info.groupFormed}, GO=${info.isGroupOwner}, IP=${info.groupOwnerAddress?.hostAddress}"
        )
    }

    /**
     * Updates the device information.
     * @param device The device information.
     */
    internal fun updateThisDevice(device: WifiP2pDevice) {
        _thisDevice.value = device //update device
        Log.d(
            TAG,
            "This device: ${device.deviceName} (${device.deviceAddress}) Status=${device.status}"
        )
    }

    /**
     * Cleans up resources.
     */
    fun cleanup() {
        disconnect() //disconnect from group
        stopDiscovery() //stop discovery

        try {
            context.unregisterReceiver(broadcastReceiver)
        } catch (e: Exception) {
            //already unregistered
        }
    }

    /**
     * Requests a list of available WiFi Direct peers.
     */
    fun requestPeers() {
        if (ActivityCompat.checkSelfPermission( //check permissions
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        wifiP2pManager.requestPeers(channel) { peers -> //request peers
            updatePeerList(peers) //update list
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
        when (intent?.action) { //check intent
            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> { //wifi state changed
                val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                Log.d(TAG, "WiFi P2P State: $state")
            }

            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> { //connection changed
                val connInfoExtra = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent?.getParcelableExtra( //get connection info
                        WifiP2pManager.EXTRA_WIFI_P2P_INFO,
                        WifiP2pInfo::class.java
                    )
                } else {
                    @Suppress("DEPRECATION") //Suppress deprecation warning
                    intent?.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO)
                }
                connInfoExtra?.let { manager.updateConnectionInfo(it) } //update connection
            }

            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> { //peers changed
                val peersExtra = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent?.getParcelableExtra( //get peer list
                        WifiP2pManager.EXTRA_P2P_DEVICE_LIST,
                        WifiP2pDeviceList::class.java
                    )
                } else {
                    @Suppress("DEPRECATION")
                    intent?.getParcelableExtra(WifiP2pManager.EXTRA_P2P_DEVICE_LIST)
                }
                manager.requestPeers() //get new peer list
            }

            WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> { //device changed
                val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent?.getParcelableExtra( //get device info
                        WifiP2pManager.EXTRA_WIFI_P2P_DEVICE,
                        WifiP2pDevice::class.java
                    )
                } else {
                    @Suppress("DEPRECATION")
                    intent?.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE)
                }
                device?.let { manager.updateThisDevice(it) } //update device
            }
        }
    }
}