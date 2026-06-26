package com.example.chopchoprecipeapp.ui

import com.example.chopchoprecipeapp.MainDispatcherRule
import com.example.chopchoprecipeapp.data.Recipe
import com.example.chopchoprecipeapp.data.RecipeRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class RecipeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun testAddRecipeCallsRepository() = runTest {
        val repository = mockk<RecipeRepository>(relaxed = true)
        val viewModel = RecipeViewModel(repository)
        val testRecipe = Recipe(
            name = "cake",
            rating = 5,
            tags = listOf(),
            instructions = "",
            ingredients = listOf(),
            imageUri = null
        )

        viewModel.addRecipe(testRecipe)

        coVerify { repository.insert(testRecipe) }
    }
}