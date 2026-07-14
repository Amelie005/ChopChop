package com.example.chopchoprecipeapp.data

import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.io.File

/**
 * Helper class for serializing/deserializing recipes for WiFi Direct transfer.
 * @author Amelie Dzierzawa
 */
object RecipeTransfer {

    /**
     * Converts a Recipe to JSON string with Base64-encoded image.
     */
    fun recipeToJson(recipe: Recipe): String {
        val imageBase64 = if (!recipe.imageUri.isNullOrEmpty() && File(recipe.imageUri).exists()) {
            val imageBytes = File(recipe.imageUri).readBytes()
            Base64.encodeToString(imageBytes, Base64.DEFAULT)
        } else {
            null
        }

        val jsonObject = JsonObject().apply {
            addProperty("id", recipe.id)
            addProperty("name", recipe.name)
            addProperty("rating", recipe.rating)
            add("tags", Gson().toJsonTree(recipe.tags))
            addProperty("instructions", recipe.instructions)
            add("ingredients", Gson().toJsonTree(recipe.ingredients))
            if (imageBase64 != null) {
                addProperty("imageBase64", imageBase64)
            }
        }

        return jsonObject.toString()
    }

    /**
     * Converts JSON string back to Recipe, saves Base64 image if present.
     */
    fun jsonToRecipe(json: String, context: android.content.Context): Recipe? {
        return try {
            val jsonObject = Gson().fromJson(json, JsonObject::class.java)

            var imageUri: String? = null
            if (jsonObject.has("imageBase64")) {
                val imageBase64 = jsonObject.get("imageBase64").asString
                val imageBytes = Base64.decode(imageBase64, Base64.DEFAULT)

                val fileName = "recipe_received_${System.currentTimeMillis()}.jpg"
                val imageDir = File(context.filesDir, "images")
                imageDir.mkdirs()
                val imageFile = File(imageDir, fileName)

                imageFile.writeBytes(imageBytes)
                imageUri = imageFile.absolutePath
            }

            Recipe(
                id = 0,
                name = jsonObject.get("name").asString,
                rating = jsonObject.get("rating").asInt,
                tags = Gson().fromJson(jsonObject.get("tags"), Array<String>::class.java).toList(),
                instructions = jsonObject.get("instructions").asString,
                ingredients = Gson().fromJson(
                    jsonObject.get("ingredients"),
                    Array<String>::class.java
                ).toList(),
                imageUri = imageUri
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}