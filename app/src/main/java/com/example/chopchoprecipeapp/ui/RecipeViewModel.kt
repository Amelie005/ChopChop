package com.example.chopchoprecipeapp.ui

import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chopchoprecipeapp.data.Recipe
import com.example.chopchoprecipeapp.data.RecipeRepository
import com.example.chopchoprecipeapp.wifidirect.RecipeTransferService
import com.example.chopchoprecipeapp.wifidirect.TransferStatus
import com.example.chopchoprecipeapp.wifidirect.WiFiDirectManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for managing recipes and WiFi Direct transfers.
 * @param repository The repository for accessing recipe data.
 * @param wifiDirectManager The manager for managing WiFi Direct connections.
 * @param transferService The service for handling recipe transfers
 * @author Amelie Dzierzawa
 */
class RecipeViewModel(
    private val repository: RecipeRepository,
    private val wifiDirectManager: WiFiDirectManager,
    private val transferService: RecipeTransferService
) : ViewModel() {

    val allRecipes: StateFlow<List<Recipe>> = repository.allRecipes
        .stateIn(
            viewModelScope,
            kotlinx.coroutines.flow.SharingStarted.Lazily,
            emptyList()
        )

    private val _selectedRecipe = MutableStateFlow<Recipe?>(null)
    val selectedRecipe: StateFlow<Recipe?> = _selectedRecipe.asStateFlow()

    val availablePeers: StateFlow<List<WifiP2pDevice>> = wifiDirectManager.availablePeers
    val isDiscovering: StateFlow<Boolean> = wifiDirectManager.isDiscovering
    val transferStatus: StateFlow<TransferStatus> = transferService.transferStatus
    val transferError: StateFlow<String?> = transferService.transferError
    val receivedRecipe: StateFlow<Recipe?> = transferService.receivedRecipe
    val peerIpAddress: StateFlow<String?> = transferService.peerIpAddress
    val connectionInfo: StateFlow<WifiP2pInfo?> = wifiDirectManager.connectionInfo
    val thisDevice: StateFlow<WifiP2pDevice?> = wifiDirectManager.thisDevice

    init {
        //Start server and begin listening
        startReceivingServer()

        //handle received recipes automatically
        viewModelScope.launch {
            transferService.receivedRecipe.collect { recipe ->
                if (recipe != null) {
                    repository.insert(recipe)
                    Log.d("RecipeVM", "Received recipe database insert triggered: ${recipe.name}")
                }
            }
        }

        //Handle automatic handshake and IP exchange
        viewModelScope.launch {
            wifiDirectManager.connectionInfo.collect { info ->
                if (info != null && info.groupFormed) {
                    if (info.isGroupOwner) {
                        Log.d("RecipeVM", "We are Group Owner. Waiting for Client's Handshake...")
                    } else {
                        val ownerAddress = info.groupOwnerAddress?.hostAddress
                        if (ownerAddress != null) {
                            Log.d(
                                "RecipeVM",
                                "We are Client. GO IP is: $ownerAddress. Initiating Handshake..."
                            )
                            transferService.setPeerIpAddress(ownerAddress)
                            //Let the group owner know our IP address
                            transferService.sendHandshake(ownerAddress)
                        }
                    }
                } else {
                    //Reset peer IP if disconnected
                    transferService.setPeerIpAddress(null)
                }
            }
        }
    }

    /**
     * Loads a recipe by its ID.
     * @param id The ID of the recipe to load.
     */
    fun loadRecipeById(id: Int) {
        viewModelScope.launch {
            _selectedRecipe.value = repository.getRecipeById(id)
        }
    }

    /**
     * Clears the selected recipe.
     *
     */
    fun clearSelectedRecipe() {
        _selectedRecipe.value = null
    }

    /**
     * Adds a new recipe to the repository.
     * @param recipe The recipe to add.
     */
    fun addRecipe(recipe: Recipe) {
        viewModelScope.launch {
            repository.insert(recipe)
        }
    }

    /**
     * Deletes a recipe from the repository.
     * @param recipe The recipe to delete.
     */
    fun deleteRecipe(recipe: Recipe) {
        viewModelScope.launch {
            repository.delete(recipe)
        }
    }

    /**
     * Starts the discovery process for available WiFi Direct peers.
     */
    fun startDiscovery() {
        wifiDirectManager.discoverPeers()
    }

    /**
     * Stops the discovery process for available WiFi Direct peers.
     */
    fun stopDiscovery() {
        wifiDirectManager.stopDiscovery()
    }

    /**
     * Connects to the specified WiFi Direct peer.
     * @param device The device to connect to.
     */
    fun connectToPeer(device: WifiP2pDevice) {
        wifiDirectManager.connectToPeer(device)
    }

    /**
     * Sends the specified recipe to the active connected peer.
     * @param recipe The recipe to send.
     */
    fun sendRecipeToActivePeer(recipe: Recipe) {
        val targetIp = peerIpAddress.value
        if (!targetIp.isNullOrEmpty()) {
            transferService.sendRecipe(recipe, targetIp)
        } else {
            Log.e("RecipeVM", "No active peer IP connected to send recipe!")
        }
    }

    /**
     * Sends the specified recipe to the specified host address.
     * @param recipe The recipe to send.
     * @param hostAddress The host address to send the recipe to.
     */
    fun sendRecipeToPeer(recipe: Recipe, hostAddress: String) {
        transferService.sendRecipe(recipe, hostAddress)
    }

    /**
     * Starts the server for receiving recipes.
     */
    fun startReceivingServer() {
        transferService.startServer()
    }

    /**
     * Clears the error state in the transfer service.
     */
    fun clearError() {
        transferService.clearError()
    }

    /**
     * Cleans up resources when the ViewModel is cleared.
     */
    override fun onCleared() {
        super.onCleared()
        wifiDirectManager.cleanup()
        transferService.cleanup()
    }

    /**
     * Edits an existing recipe in the repository.
     * @param recipe The recipe to edit.
     */
    fun editRecipe(recipe: Recipe) {
        viewModelScope.launch {
            repository.insert(recipe) // Overwrites the existing recipe
            _selectedRecipe.value = recipe
        }
    }
}
