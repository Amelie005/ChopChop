package com.example.chopchoprecipeapp.data

import kotlinx.coroutines.flow.Flow

/**
 * A repository for recipes.
 * @author Amelie Dzierzawa
 */
class RecipeRepository(private val recipeDao: RecipeDao) {

    //exposes the list of recipes as a Flow
    val allRecipes: Flow<List<Recipe>> get() = recipeDao.getAllRecipes()

    //suspend function ensures it runs off the main thread
    /**
     * Inserts a recipe.
     * @param recipe the recipe to insert
     */
    suspend fun insert(recipe: Recipe) {
        recipeDao.insertRecipe(recipe)
    }

    /**
     * Deletes a recipe.
     * @param recipe the recipe to delete
     */
    suspend fun delete(recipe: Recipe) {
        recipeDao.deleteRecipe(recipe)
    }

    /**
     * Returns a recipe by its ID.
     * @param id the ID of the recipe
     */
    suspend fun getRecipeById(id: Int): Recipe? {
        return recipeDao.getRecipeById(id)
    }
}