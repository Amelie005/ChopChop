package com.example.chopchoprecipeapp.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * A data access object for recipes.
 * @author Amelie Dzierzawa
 */
@Dao
interface RecipeDao {

    /**
     * Returns a flow of all recipes.
     */
    @Query("SELECT * FROM recipes")
    fun getAllRecipes(): Flow<List<Recipe>>

    /**
     * Returns a recipe by its ID.
     * @param recipeId the ID of the recipe
     */
    @Query("SELECT * FROM recipes WHERE id = :recipeId")
    suspend fun getRecipeById(recipeId: Int): Recipe?

    /**
     * Inserts a recipe.
     * @param recipe the recipe to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: Recipe)

    /**
     * Deletes a recipe.
     * @param recipe the recipe to delete
     */
    @Delete
    suspend fun deleteRecipe(recipe: Recipe)


}