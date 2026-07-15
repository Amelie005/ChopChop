package com.example.chopchoprecipeapp.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * For serializing and deserializing recipes with images using Base64.
 * @author Amelie Dzierzawa
 */
object RecipeTransfer {
    private const val TAG = "RecipeTransfer"

    /**
     * Converts a Recipe object into a JSON string.
     * @param context the context of the application
     * @param recipe the recipe to convert
     */
    fun recipeToJson(context: Context, recipe: Recipe): String {
        val json = JSONObject().apply {
            put("name", recipe.name)
            put("rating", recipe.rating)
            put("instructions", recipe.instructions)

            val tagsArray = JSONArray()
            recipe.tags.forEach { tagsArray.put(it) }
            put("tags", tagsArray)

            val ingredientsArray = JSONArray()
            recipe.ingredients.forEach { ingredientsArray.put(it) }
            put("ingredients", ingredientsArray)

            val imageBase64 = encodeImageToBase64(context, recipe.imageUri)
            if (imageBase64 != null) {
                put("imageBase64", imageBase64)
            }
        }
        return json.toString()
    }

    /**
     * Reconstructs a Recipe object from a JSON string.
     * @param jsonData the JSON string to convert
     * @param context the context of the application
     */
    fun jsonToRecipe(jsonData: String, context: Context): Recipe? {
        return try {
            val json = JSONObject(jsonData)
            val name = json.getString("name")
            val rating = json.getInt("rating")
            val instructions = json.getString("instructions")

            val tagsList = mutableListOf<String>()
            val tagsArray = json.getJSONArray("tags")
            for (i in 0 until tagsArray.length()) {
                tagsList.add(tagsArray.getString(i))
            }

            val ingredientsList = mutableListOf<String>()
            val ingredientsArray = json.getJSONArray("ingredients")
            for (i in 0 until ingredientsArray.length()) {
                ingredientsList.add(ingredientsArray.getString(i))
            }

            val imageBase64 = if (json.has("imageBase64")) json.getString("imageBase64") else null
            val localImageUri = decodeBase64ToImage(context, imageBase64)

            Recipe(
                id = 0,
                name = name,
                rating = rating,
                tags = tagsList,
                instructions = instructions,
                ingredients = ingredientsList,
                imageUri = localImageUri
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse recipe JSON: ${e.message}")
            null
        }
    }

    /**
     * Encodes a local image URI or path to a compressed Base64 string.
     * @param context the context of the application
     * @param uriString the local image URI or path
     */
    private fun encodeImageToBase64(context: Context, uriString: String?): String? {
        if (uriString.isNullOrEmpty()) return null
        return try {
            val inputStream = try {
                context.contentResolver.openInputStream(Uri.parse(uriString))
            } catch (e: Exception) {
                val file = File(uriString)
                if (file.exists()) file.inputStream() else null
            } ?: return null

            val bytes = inputStream.readBytes()
            inputStream.close()

            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
            val outputStream = ByteArrayOutputStream()

            //limit resolution to 1024px maximum for performance and to prevent memory issues
            val scaledBitmap = if (bitmap.width > 1024 || bitmap.height > 1024) {
                val aspectRatio = bitmap.width.toDouble() / bitmap.height.toDouble()
                val width = if (aspectRatio > 1) 1024 else (1024 * aspectRatio).toInt()
                val height = if (aspectRatio > 1) (1024 / aspectRatio).toInt() else 1024
                Bitmap.createScaledBitmap(bitmap, width, height, true)
            } else {
                bitmap //no need to scale
            }

            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val compressedBytes = outputStream.toByteArray()
            Base64.encodeToString(compressedBytes, Base64.DEFAULT)
        } catch (e: Exception) {
            Log.e(TAG, "Error encoding image: ${e.message}")
            null
        }
    }

    /**
     * Decodes a Base64 string back into a saved local image file.
     * @param context the context of the application
     * @param base64Str the Base64 string to decode
     */
    private fun decodeBase64ToImage(context: Context, base64Str: String?): String? {
        if (base64Str.isNullOrEmpty()) return null
        return try {
            val bytes = Base64.decode(base64Str, Base64.DEFAULT)
            val directory = File(context.filesDir, "recipe_images").apply { mkdirs() }
            val file = File(directory, "p2p_${System.currentTimeMillis()}.jpg")
            file.writeBytes(bytes)
            Uri.fromFile(file).toString()
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding image: ${e.message}")
            null
        }
    }
}