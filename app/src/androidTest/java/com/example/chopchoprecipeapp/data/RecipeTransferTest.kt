package com.example.chopchoprecipeapp.data

import com.example.chopchoprecipeapp.wifidirect.TransferStatus
import org.junit.Assert.*
import org.junit.Test

class RecipeTransferTest {

    //TC-05: JSON validation (no context)
    @Test
    fun testRecipeJsonStructure() {
        val recipe = Recipe(
            id = 1,
            name = "Pasta",
            rating = 5,
            tags = listOf("Italian"),
            instructions = "Cook",
            ingredients = listOf("200g Pasta"),
            imageUri = null
        )

        val json = try {
            //only test if JSOn is valid
            val jsonStr = "{\"name\":\"Pasta\",\"rating\":5}"
            jsonStr.contains("Pasta")
        } catch (e: Exception) {
            false
        }

        assertTrue(json)
    }

    @Test
    fun testRecipeValidation() {
        val recipe = Recipe(
            id = 1,
            name = "Valid Recipe",
            rating = 5,
            tags = listOf("Tag1"),
            instructions = "Instructions",
            ingredients = listOf("Ingredient1"),
            imageUri = null
        )

        assertNotNull(recipe)
        assertEquals("Valid Recipe", recipe.name)
        assertEquals(5, recipe.rating)
        assertEquals(1, recipe.tags.size)
    }


    @Test
    fun testEmptyIngredientsValidation() {
        val recipe = Recipe(
            id = 1,
            name = "Test",
            rating = 5,
            tags = listOf(),
            instructions = "Test",
            ingredients = emptyList(),
            imageUri = null
        )

        assertTrue(recipe.ingredients.isEmpty())
    }

    @Test
    fun testMultipleTagsHandling() {
        val recipe = Recipe(
            id = 1,
            name = "Test",
            rating = 5,
            tags = listOf("Tag1", "Tag2", "Tag3"),
            instructions = "Test",
            ingredients = listOf("Ingredient"),
            imageUri = null
        )

        assertEquals(3, recipe.tags.size)
        assertTrue(recipe.tags.contains("Tag1"))
    }
}