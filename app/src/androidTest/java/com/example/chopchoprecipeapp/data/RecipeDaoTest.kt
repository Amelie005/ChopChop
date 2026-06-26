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
class RecipeDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: RecipeDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        //in memory db, that gets deleted after the test
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.recipeDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndGetRecipe() = runBlocking {
        val recipe = Recipe(
            id = 1,
            name = "test recipe",
            rating = 5,
            tags = listOf("veggie"),
            instructions = "cook",
            ingredients = listOf("ingredient1"),
            imageUri = null
        )

        dao.insertRecipe(recipe)

        //get data from the Flow
        val allRecipes = dao.getAllRecipes().first()

        Assert.assertEquals(1, allRecipes.size)
        Assert.assertEquals("test recipe", allRecipes[0].name)
        Assert.assertEquals(
            listOf("veggie"),
            allRecipes[0].tags
        ) //also test here if Converters class is functioning
    }
}