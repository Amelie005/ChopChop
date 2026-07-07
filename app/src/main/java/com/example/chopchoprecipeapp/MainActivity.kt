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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.chopchoprecipeapp.ui.AddRecipeScreen
import com.example.chopchoprecipeapp.ui.RecipeListScreen
import com.example.chopchoprecipeapp.ui.RecipeViewModel
import com.example.chopchoprecipeapp.ui.theme.ChopChopRecipeAppTheme

//definition of available screens
enum class Screen {
    LIST, ADD
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
    val recipeViewModel: RecipeViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as MainApplication
                return RecipeViewModel(application.repository) as T
            }
        }
    )

    //which screen is currently shown
    var currentScreen by remember { mutableStateOf(Screen.LIST) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
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
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentScreen) {
                Screen.LIST -> {
                    RecipeListScreen(
                        viewModel = recipeViewModel,
                        onRecipeClick = { selectedRecipe ->
                            //TODO: Navigate to RecipeDetailScreen
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
            }
        }
    }
}