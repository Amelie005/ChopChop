package com.example.chopchoprecipeapp.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecipeDatabaseTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: RecipeDao

    @Before
    fun createDb() { //in memory db again
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.recipeDao()
    }

    @After
    fun closeDb() {
        //only gets closed if db was actually initialized
        if (::db.isInitialized) {
            db.close()
        }
    }

    @Test
    fun insertAndReadRecipe() = runBlocking {
        val recipe = Recipe(
            id = 1,
            name = "pasta",
            rating = 5,
            tags = listOf("italian", "quick"),
            instructions = "cook",
            ingredients = listOf("pasta", "salt"),
            imageUri = null
        )

        dao.insertRecipe(recipe)

        val list = dao.getAllRecipes().first()
        Assert.assertEquals(list[0].name, "pasta")
    }
}