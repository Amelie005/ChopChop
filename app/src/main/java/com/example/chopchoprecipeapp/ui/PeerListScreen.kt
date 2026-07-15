package com.example.chopchoprecipeapp.ui

import android.net.wifi.p2p.WifiP2pDevice
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.chopchoprecipeapp.data.Recipe
import com.example.chopchoprecipeapp.wifidirect.TransferStatus

/**
 * Screen to select a peer device and send a recipe via WiFi Direct.
 *
 * @param recipe The recipe to send.
 * @param viewModel The view model to handle the WiFi Direct operations.
 * @param onBackClick Callback invoked when navigating back.
 * @param modifier The modifier to apply to this layout.
 * @author Amelie Dzierzawa
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeerListScreen(
    recipe: Recipe,
    viewModel: RecipeViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val availablePeers by viewModel.availablePeers.collectAsState()
    val isDiscovering by viewModel.isDiscovering.collectAsState()
    val transferStatus by viewModel.transferStatus.collectAsState()
    val transferError by viewModel.transferError.collectAsState()
    val thisDevice by viewModel.thisDevice.collectAsState()
    val peerIpAddress by viewModel.peerIpAddress.collectAsState()

    var selectedPeer by remember { mutableStateOf<WifiP2pDevice?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var isConnecting by remember { mutableStateOf(false) }

    // Start discovery on composition and stop on dispose
    DisposableEffect(Unit) {
        viewModel.startDiscovery()
        onDispose {
            viewModel.stopDiscovery()
        }
    }

    // Filter out the current device and invalid peers
    val otherPeers = availablePeers.filter { peer ->
        val isOwnDevice = peer.deviceAddress == thisDevice?.deviceAddress

        // Allow devices that are available, already connected, or currently invited
        val isValidStatus = peer.status in listOf(
            WifiP2pDevice.AVAILABLE,
            WifiP2pDevice.CONNECTED,
            WifiP2pDevice.INVITED
        )

        !isOwnDevice && isValidStatus && !peer.deviceName.isNullOrBlank()
    }

    // Trigger sending once handshake establishes target peer IP
    LaunchedEffect(peerIpAddress, isConnecting, selectedPeer) {
        val activeIp = peerIpAddress
        if (isConnecting && !activeIp.isNullOrEmpty() && selectedPeer != null) {
            isConnecting = false
            Log.d("PeerList", "Sending to: ${selectedPeer?.deviceName} at $activeIp")
            viewModel.sendRecipeToPeer(recipe, activeIp)
        }
    }

    // Reset connection state upon error
    LaunchedEffect(transferError) {
        if (transferError != null) {
            isConnecting = false
        }
    }

    // Navigate back upon successful transfer
    LaunchedEffect(transferStatus) {
        if (transferStatus == TransferStatus.SentSuccess) {
            onBackClick()
        }
    }

    if (showConfirmDialog && selectedPeer != null) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Send Recipe") },
            text = { Text("Send \"${recipe.name}\" to ${selectedPeer?.deviceName ?: "Unknown"}?") },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        Log.d(
                            "PeerList",
                            "Connecting to: ${selectedPeer?.deviceName} (${selectedPeer?.deviceAddress})"
                        )
                        viewModel.connectToPeer(selectedPeer!!)
                        isConnecting = true
                    }
                ) {
                    Text("Send")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Share Recipe") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            if (transferError != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Error: ${transferError ?: ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = { viewModel.clearError() },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Dismiss")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            when {
                isConnecting -> {
                    Text(
                        text = "Connecting to device and exchanging IP...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                transferStatus == TransferStatus.Idle -> {
                    Text(
                        text = if (isDiscovering) "Searching for devices..." else "Available Devices",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                transferStatus == TransferStatus.Sending -> {
                    Text(
                        text = "Sending...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                transferStatus == TransferStatus.SentSuccess -> {
                    Text(
                        text = "Recipe sent successfully!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (otherPeers.isEmpty() && !isDiscovering) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No devices found", style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.startDiscovery() }) {
                            Text("Scan Again")
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(otherPeers) { peer ->
                        PeerCard(
                            peer = peer,
                            isSelected = selectedPeer?.deviceAddress == peer.deviceAddress,
                            onClick = {
                                selectedPeer = peer
                                showConfirmDialog = true
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * A composable function that displays a card for a peer device.
 *
 * @param peer The peer device to display.
 * @param isSelected Indicates whether the card is currently selected.
 * @param onClick The action to perform when the card is clicked.
 * @author Amelie Dzierzawa
 */
@Composable
fun PeerCard(
    peer: WifiP2pDevice,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = peer.deviceName ?: "Unknown Device",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Device Address: ${peer.deviceAddress ?: "Unknown"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}