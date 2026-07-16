package com.example.chopchoprecipeapp.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecipeDAOTest {

    private lateinit var database: AppDatabase
    private lateinit var recipeDAO: RecipeDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
        recipeDAO = database.recipeDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    //TC-01: Insert & Retrieve
    @Test
    fun testInsertAndRetrieveRecipe() = runBlocking {
        val recipe = Recipe(
            id = 1,
            name = "Pasta Carbonara",
            rating = 5,
            tags = listOf("Italian", "Quick"),
            instructions = "Mix and cook",
            ingredients = listOf("200g Pasta", "100g Bacon"),
            imageUri = null
        )

        recipeDAO.insertRecipe(recipe)
        val retrieved = recipeDAO.getRecipeById(1)

        assertNotNull(retrieved)
        assertEquals("Pasta Carbonara", retrieved?.name)
        assertEquals(5, retrieved?.rating)
    }

    @Test
    fun testInsertMultipleRecipes() = runBlocking {
        val recipe1 = Recipe(1, "Pasta", 5, listOf("Italian"), "Cook", listOf("Pasta"), null)
        val recipe2 = Recipe(2, "Pizza", 4, listOf("Italian"), "Bake", listOf("Dough", "Tomato"), null)

        recipeDAO.insertRecipe(recipe1)
        recipeDAO.insertRecipe(recipe2)
        val allRecipes = recipeDAO.getAllRecipes().first()

        assertEquals(2, allRecipes.size)
    }

    //TC-02
    @Test
    fun testTagsConversion() = runBlocking {
        val tags = listOf("Vegetarian", "Quick", "Healthy")
        val recipe = Recipe(
            id = 1,
            name = "Salad",
            rating = 3,
            tags = tags,
            instructions = "Mix",
            ingredients = listOf("Lettuce", "Tomato"),
            imageUri = null
        )

        recipeDAO.insertRecipe(recipe)
        val retrieved = recipeDAO.getRecipeById(1)

        assertEquals(tags, retrieved?.tags)
        assertEquals(3, retrieved?.tags?.size)
    }

    @Test
    fun testDeleteRecipe() = runBlocking {
        val recipe = Recipe(1, "Pasta", 5, listOf("Italian"), "Cook", listOf("Pasta"), null)

        recipeDAO.insertRecipe(recipe)
        recipeDAO.deleteRecipe(recipe)
        val retrieved = recipeDAO.getRecipeById(1)

        assertNull(retrieved)
    }
}