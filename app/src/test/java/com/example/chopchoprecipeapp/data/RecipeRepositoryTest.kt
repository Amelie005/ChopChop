package com.example.chopchoprecipeapp.data

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class RecipeRepositoryTest {
    private lateinit var recipeDao: RecipeDao
    private lateinit var repository: RecipeRepository

    @Before
    fun setup() {
        //create mock
        recipeDao = mockk()

        //set behaviour before repository gets created
        coEvery { recipeDao.getAllRecipes() } returns flowOf(emptyList())

        //initialize repository
        repository = RecipeRepository(recipeDao)
    }

    @Test
    fun `test getAllRecipes calls dao`() = runBlocking {
        val mockRecipes = listOf(
            Recipe(
                id = 1,
                name = "carbonara",
                rating = 5,
                tags = listOf("italian", "quick"),
                instructions = "cook pasta, add sauce, cook bacon",
                ingredients = listOf("pasta", "egg", "sauce", "bacon"),
                imageUri = null
            ),
            Recipe(
                id = 2,
                name = "salad",
                rating = 3,
                tags = listOf("healthy", "veggie"),
                instructions = "cut it all up",
                ingredients = listOf("salad", "tomato", "cucumber"),
                imageUri = null
            )
        )

        //set behaviour before calling repository
        coEvery { recipeDao.getAllRecipes() } returns flowOf(mockRecipes)

        //use repository
        val result = repository.allRecipes.first()

        assertEquals(mockRecipes, result)
        coVerify { recipeDao.getAllRecipes() }
    }
}