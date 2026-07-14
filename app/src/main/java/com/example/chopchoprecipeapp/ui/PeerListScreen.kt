package com.example.chopchoprecipeapp.ui

import android.net.wifi.p2p.WifiP2pDevice
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
import androidx.compose.material.icons.filled.Refresh
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
    val connectionInfo by viewModel.connectionInfo.collectAsState()  // ✨ Neu

    var selectedPeer by remember { mutableStateOf<WifiP2pDevice?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var isConnecting by remember { mutableStateOf(false) }

    //auto-start discovery on enter
    LaunchedEffect(Unit) {
        viewModel.startDiscovery()
    }

    LaunchedEffect(connectionInfo, isConnecting) {
        if (isConnecting && connectionInfo?.groupFormed == true && selectedPeer != null) {
            isConnecting = false
            val hostAddress = connectionInfo?.groupOwnerAddress?.hostAddress ?: "192.168.49.1"
            viewModel.sendRecipeToPeer(recipe, hostAddress)
        }
    }

    //when transfer succeeds, go back
    LaunchedEffect(transferStatus) {
        if (transferStatus == TransferStatus.SentSuccess) {
            //cleanup
            viewModel.stopDiscovery()
            onBackClick()
        }
    }

    //cleanup on leave
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopDiscovery()
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
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.startDiscovery() },
                        enabled = !isDiscovering
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
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
            // Status Message
            when {
                isConnecting -> {
                    Text(
                        "Connecting to device...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                transferStatus == TransferStatus.Idle -> {
                    if (isDiscovering) {
                        Text(
                            "Searching for devices...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Text(
                            "Available Devices",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                transferStatus == TransferStatus.Sending -> {
                    Text(
                        "Sending...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                transferStatus == TransferStatus.SentSuccess -> {
                    Text(
                        "Recipe sent successfully!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                transferStatus == TransferStatus.SentFailed -> {
                    Text(
                        "Failed to send recipe",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                else -> {}
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (availablePeers.isEmpty() && !isDiscovering) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
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
                    items(availablePeers) { peer ->
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