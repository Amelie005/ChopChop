package com.example.chopchoprecipeapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.chopchoprecipeapp.ui.AddRecipeScreen
import com.example.chopchoprecipeapp.ui.PeerListScreen
import com.example.chopchoprecipeapp.ui.RecipeDetailScreen
import com.example.chopchoprecipeapp.ui.RecipeListScreen
import com.example.chopchoprecipeapp.ui.RecipeViewModel
import com.example.chopchoprecipeapp.ui.theme.ChopChopRecipeAppTheme
import com.example.chopchoprecipeapp.wifidirect.RecipeTransferService
import com.example.chopchoprecipeapp.wifidirect.WiFiDirectManager

//definition of available screens
enum class Screen {
    LIST, ADD, DETAIL, PEER_LIST
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChopChopRecipeAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    RecipeAppNavigation(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun RecipeAppNavigation(modifier: Modifier) {
    val context = LocalContext.current

    RequestWiFiDirectPermissions()

    val wifiDirectManager = remember { WiFiDirectManager(context) }
    val transferService = remember { RecipeTransferService(context) }

    LaunchedEffect(Unit) {
        wifiDirectManager.discoverPeers()
    }

    val recipeViewModel: RecipeViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application =
                    checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as MainApplication
                return RecipeViewModel(
                    application.repository,
                    wifiDirectManager,
                    transferService
                ) as T
            }
        }
    )

    //which screen is currently shown
    var currentScreen by remember { mutableStateOf(Screen.LIST) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            //only show bottom bar when not on detail or peer list screen
            if (currentScreen != Screen.DETAIL && currentScreen != Screen.PEER_LIST) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentScreen == Screen.LIST,
                        onClick = { currentScreen = Screen.LIST },
                        icon = { Icon(Icons.Default.List, contentDescription = "List") },
                        label = { Text("Recipes") }
                    )
                    NavigationBarItem(
                        selected = currentScreen == Screen.ADD,
                        onClick = { currentScreen = Screen.ADD },
                        icon = { Icon(Icons.Default.Add, contentDescription = "Add") },
                        label = { Text("New") }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentScreen) {
                Screen.LIST -> {
                    RecipeListScreen(
                        viewModel = recipeViewModel,
                        onRecipeClick = { selectedRecipe ->
                            recipeViewModel.loadRecipeById(selectedRecipe.id)
                            currentScreen = Screen.DETAIL
                        }
                    )
                }

                Screen.ADD -> {
                    AddRecipeScreen(
                        viewModel = recipeViewModel,
                        onRecipeSaved = {
                            //if saved, automatically jump back to list
                            currentScreen = Screen.LIST
                        }
                    )
                }

                Screen.DETAIL -> {
                    val selectedRecipe by recipeViewModel.selectedRecipe.collectAsState()
                    if (selectedRecipe != null) {
                        RecipeDetailScreen(
                            recipe = selectedRecipe!!,
                            onBackClick = {
                                currentScreen = Screen.LIST
                                recipeViewModel.clearSelectedRecipe()
                            },
                            onDeleteClick = { recipe ->
                                recipeViewModel.deleteRecipe(recipe)
                                currentScreen = Screen.LIST
                                recipeViewModel.clearSelectedRecipe()
                            },
                            onShareClick = {
                                currentScreen = Screen.PEER_LIST
                            }
                        )
                    }
                }

                Screen.PEER_LIST -> {
                    val selectedRecipe by recipeViewModel.selectedRecipe.collectAsState()
                    if (selectedRecipe != null) {
                        PeerListScreen(
                            recipe = selectedRecipe!!,
                            viewModel = recipeViewModel,
                            onBackClick = {
                                currentScreen = Screen.DETAIL
                            }
                        )
                    }
                }
            }
        }
    }
}