package com.example.chopchoprecipeapp

import android.app.Application
import androidx.room.Room
import com.example.chopchoprecipeapp.data.AppDatabase
import com.example.chopchoprecipeapp.data.RecipeRepository

/**
 * Initializes the app database and repository.
 * @author Amelie Dzierzawa
 */
class MainApplication : Application() {

    //database only gets initialized when it's needed (lazy)
    val database by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "recipe_database").build()
    }
    //repository gets DAO from database
    val repository by lazy {
        RecipeRepository(database.recipeDao())
    }
}