package com.example.chopchoprecipeapp.ui

import com.example.chopchoprecipeapp.data.Recipe
import org.junit.Assert.*
import org.junit.Test

class RecipeViewModelTest {

    //TC-03: Recipe Data validation
    @Test
    fun testRecipeCreation() {
        val recipe = Recipe(
            id = 1,
            name = "Pasta Carbonara",
            rating = 5,
            tags = listOf("Italian", "Quick"),
            instructions = "Cook and serve",
            ingredients = listOf("200g Pasta", "100g Bacon"),
            imageUri = null
        )

        assertEquals(1, recipe.id)
        assertEquals("Pasta Carbonara", recipe.name)
        assertEquals(5, recipe.rating)
        assertEquals(2, recipe.tags.size)
        assertEquals(2, recipe.ingredients.size)
    }

    @Test
    fun testRecipeWithAllFields() {
        val recipe = Recipe(
            id = 5,
            name = "Pizza Margherita",
            rating = 4,
            tags = listOf("Italian", "Vegetarian"),
            instructions = "Bake at 200°C",
            ingredients = listOf("Dough", "Tomato", "Mozzarella"),
            imageUri = "/path/to/image.jpg"
        )

        assertNotNull(recipe.imageUri)
        assertEquals("/path/to/image.jpg", recipe.imageUri)
        assertEquals(3, recipe.ingredients.size)
    }

    @Test
    fun testRecipeRatingValidation() {
        val validRatings = listOf(1, 2, 3, 4, 5)

        validRatings.forEach { rating ->
            val recipe = Recipe(
                id = 1,
                name = "Test",
                rating = rating,
                tags = listOf(),
                instructions = "Test",
                ingredients = listOf("Test"),
                imageUri = null
            )
            assertEquals(rating, recipe.rating)
            assertTrue(rating in 1..5)
        }
    }

    @Test
    fun testRecipeWithEmptyTags() {
        val recipe = Recipe(
            id = 1,
            name = "Simple Recipe",
            rating = 3,
            tags = emptyList(),
            instructions = "Cook",
            ingredients = listOf("Ingredient"),
            imageUri = null
        )

        assertTrue(recipe.tags.isEmpty())
    }

    @Test
    fun testRecipeWithMultipleIngredients() {
        val ingredients = listOf(
            "200g Pasta",
            "100g Tomato Sauce",
            "50g Cheese",
            "2 Garlic Cloves"
        )

        val recipe = Recipe(
            id = 1,
            name = "Pasta with Sauce",
            rating = 4,
            tags = listOf("Quick"),
            instructions = "Mix and cook",
            ingredients = ingredients,
            imageUri = null
        )

        assertEquals(4, recipe.ingredients.size)
        assertTrue(recipe.ingredients.contains("200g Pasta"))
    }

    @Test
    fun testRecipeComparison() {
        val recipe1 = Recipe(1, "Pasta", 5, listOf("Italian"), "Cook", listOf("Pasta"), null)
        val recipe2 = Recipe(1, "Pasta", 5, listOf("Italian"), "Cook", listOf("Pasta"), null)

        assertEquals(recipe1, recipe2)
    }

    @Test
    fun testRecipeCopy() {
        val original = Recipe(1, "Original", 3, listOf("Tag"), "Cook", listOf("Ingredient"), null)
        val modified = original.copy(name = "Modified", rating = 5)

        assertEquals("Original", original.name)
        assertEquals("Modified", modified.name)
        assertEquals(5, modified.rating)
    }

    @Test
    fun testTagNormalizationLogic() {
        val rawTags = listOf("Veggie", "veggie", "  Quick  ")
        val processed = rawTags.map { it.trim().lowercase() }.distinct()

        assertEquals(2, processed.size)
        assertTrue(processed.contains("veggie"))
        assertTrue(processed.contains("quick"))
    }
}