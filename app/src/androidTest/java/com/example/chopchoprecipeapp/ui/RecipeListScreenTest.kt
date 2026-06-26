package com.example.chopchoprecipeapp.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.chopchoprecipeapp.data.Recipe
import org.junit.Rule
import org.junit.Test

class RecipeListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun recipeList_displaysRecipesCorrectly() {
        //test data
        val testRecipes = listOf(
            Recipe(1, "pizza", 5, listOf("italian"), "bake it", listOf("dough"), null)
        )

        //set UI (call screen with the data)
        composeTestRule.setContent {
            RecipeListScreen(recipes = testRecipes)
        }

        //checking if the recipe is displayed
        composeTestRule.onNodeWithText("pizza").assertIsDisplayed()
    }
}