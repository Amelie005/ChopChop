package com.example.chopchoprecipeapp.ui

import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
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

    //WiFi Direct States
    val availablePeers: StateFlow<List<WifiP2pDevice>> = wifiDirectManager.availablePeers
    val isDiscovering: StateFlow<Boolean> = wifiDirectManager.isDiscovering
    val transferStatus: StateFlow<TransferStatus> = transferService.transferStatus
    val receivedRecipe: StateFlow<Recipe?> = transferService.receivedRecipe
    val connectionInfo: StateFlow<WifiP2pInfo?> = wifiDirectManager.connectionInfo

    init {
        startReceivingServer()

        viewModelScope.launch {
            transferService.receivedRecipe.collect { recipe ->
                if (recipe != null) {
                    repository.insert(recipe)
                    startReceivingServer()
                }
            }
        }
    }

    fun loadRecipeById(id: Int) {
        viewModelScope.launch {
            val recipe = repository.getRecipeById(id)
            _selectedRecipe.value = recipe
        }
    }

    fun clearSelectedRecipe() {
        _selectedRecipe.value = null
    }

    fun addRecipe(recipe: Recipe) {
        viewModelScope.launch {
            repository.insert(recipe)
        }
    }

    fun editRecipe(recipe: Recipe) {
        viewModelScope.launch {
            repository.insert(recipe)
            // Reload the recipe to update the UI with the saved changes
            _selectedRecipe.value = recipe
        }
    }

    fun deleteRecipe(recipe: Recipe) {
        viewModelScope.launch {
            repository.delete(recipe)
        }
    }

    //WiFi Direct functions
    fun startDiscovery() {
        wifiDirectManager.discoverPeers()
    }

    fun stopDiscovery() {
        wifiDirectManager.stopDiscovery()
    }

    fun connectToPeer(device: WifiP2pDevice) {
        wifiDirectManager.connectToPeer(device)
    }

    fun sendRecipeToPeer(recipe: Recipe, hostAddress: String) {
        transferService.sendRecipe(recipe, hostAddress)
    }

    fun startReceivingServer() {
        transferService.startServer()
    }

    override fun onCleared() {
        super.onCleared()
        wifiDirectManager.cleanup()
        transferService.cleanup()
    }
}
