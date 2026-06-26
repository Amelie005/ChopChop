package com.example.chopchoprecipeapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chopchoprecipeapp.data.Recipe
import com.example.chopchoprecipeapp.data.RecipeRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * A view model for recipes.
 * @author Amelie Dzierzawa
 */
class RecipeViewModel(private val repository: RecipeRepository) : ViewModel() {

    //exposes the list of recipes as a StateFlow
    val allRecipes: StateFlow<List<Recipe>> = repository.allRecipes
        .stateIn(
            viewModelScope,
            SharingStarted.Lazily, //starts the flow immediately
            emptyList()
        )

    /**
     * Adds a recipe.
     * @param recipe the recipe to add
     */
    fun addRecipe(recipe: Recipe) {
        viewModelScope.launch {
            repository.insert(recipe)
        }
    }
}