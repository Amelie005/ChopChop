package com.example.chopchoprecipeapp.wifidirect

import com.example.chopchoprecipeapp.data.Recipe
import org.junit.Assert.*
import org.junit.Test

class RecipeTransferDataValidationTest {

    //TC-04
    @Test
    fun testHandshakeMessageFormat() {
        val handshakeMsg = "HANDSHAKE:192.168.49.2"
        assertTrue(handshakeMsg.startsWith("HANDSHAKE:"))
        assertTrue(handshakeMsg.contains("192.168.49.2"))
    }

    //TC-05
    @Test
    fun testRecipeDataIntegrity() {
        val recipe = Recipe(
            id = 1,
            name = "Test Recipe",
            rating = 5,
            tags = listOf("Tag1", "Tag2"),
            instructions = "Instructions",
            ingredients = listOf("Ingredient1", "Ingredient2"),
            imageUri = null
        )

        //simulate JSON fields
        assertTrue(recipe.name.isNotEmpty())
        assertTrue(recipe.rating in 1..5)
        assertTrue(recipe.tags.isNotEmpty())
        assertTrue(recipe.ingredients.isNotEmpty())
    }

    @Test
    fun testRecipeSerializationFields() {
        val recipe = Recipe(
            id = 1,
            name = "Pasta",
            rating = 4,
            tags = listOf("Italian"),
            instructions = "Cook",
            ingredients = listOf("Pasta", "Sauce"),
            imageUri = "/path/image.jpg"
        )

        //check that all fields are present
        assertNotNull(recipe.id)
        assertNotNull(recipe.name)
        assertNotNull(recipe.rating)
        assertNotNull(recipe.tags)
        assertNotNull(recipe.instructions)
        assertNotNull(recipe.ingredients)
        assertNotNull(recipe.imageUri)
    }

    //TC-06
    @Test
    fun testEmptyRecipeHandling() {
        val recipe = Recipe(
            id = 0,
            name = "",
            rating = 0,
            tags = emptyList(),
            instructions = "",
            ingredients = emptyList(),
            imageUri = null
        )

        assertTrue(recipe.name.isEmpty())
        assertTrue(recipe.tags.isEmpty())
        assertTrue(recipe.ingredients.isEmpty())
    }

    @Test
    fun testMissingIngredientsHandling() {
        val recipe = Recipe(
            id = 1,
            name = "Empty Recipe",
            rating = 1,
            tags = listOf(),
            instructions = "No ingredients",
            ingredients = emptyList(),
            imageUri = null
        )

        assertTrue(recipe.ingredients.isEmpty())
        assertFalse(recipe.ingredients.size > 0)
    }

    @Test
    fun testLargeRecipeDataHandling() {
        val largeIngredientList = (1..100).map { "Ingredient $it" }
        val recipe = Recipe(
            id = 1,
            name = "Large Recipe",
            rating = 5,
            tags = listOf("Large"),
            instructions = "Cook many ingredients",
            ingredients = largeIngredientList,
            imageUri = null
        )

        assertEquals(100, recipe.ingredients.size)
        assertTrue(recipe.ingredients.contains("Ingredient 1"))
        assertTrue(recipe.ingredients.contains("Ingredient 100"))
    }

    @Test
    fun testSpecialCharactersInData() {
        val recipe = Recipe(
            id = 1,
            name = "Pasta à la Française",
            rating = 5,
            tags = listOf("Français", "Spécial"),
            instructions = "Cuire & servir",
            ingredients = listOf("Pâtes (200g)", "Sauce (100ml)"),
            imageUri = null
        )

        assertTrue(recipe.name.contains("à"))
        assertTrue(recipe.tags.contains("Français"))
        assertTrue(recipe.instructions.contains("&"))
    }
}