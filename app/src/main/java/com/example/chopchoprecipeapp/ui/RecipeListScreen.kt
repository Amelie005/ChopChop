package com.example.chopchoprecipeapp.ui

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.chopchoprecipeapp.data.Recipe

/**
 * A composable function that displays recipes in a list.
 * @param recipes the list of recipes to display
 * @param onRecipeClick a lambda function that is called when a recipe is clicked
 * @author Amelie Dzierzawa
 */
@Composable
fun RecipeListScreen(
    recipes: List<Recipe>,
    onRecipeClick: (Recipe) -> Unit = {}
) {
    LazyColumn {
        items(recipes) { recipe ->
            Text(text = recipe.name)
        }
    }
}