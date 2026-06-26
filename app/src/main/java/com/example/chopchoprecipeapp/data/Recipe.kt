package com.example.chopchoprecipeapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A data class representing a recipe.
 * @author Amelie Dzierzawa
 */
@Entity(tableName = "recipes")
data class Recipe(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val rating: Int, //1-5
    val tags: List<String>,
    val instructions: String,
    val ingredients: List<String>,
    val imageUri: String?
)