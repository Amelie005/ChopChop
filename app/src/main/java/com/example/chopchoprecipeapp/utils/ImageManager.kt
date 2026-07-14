package com.example.chopchoprecipeapp.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Utility for managing recipe images.
 * @author Amelie Dzierzawa
 */
object ImageManager {

    /**
     * Saves an image from a URI to the app's internal storage.
     * Returns the local file path as a string.
     */
    fun saveImageFromUri(context: Context, sourceUri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(sourceUri) ?: return null
            val fileName = "recipe_${UUID.randomUUID()}.jpg"
            val imageDir = File(context.filesDir, "images")
            imageDir.mkdirs()

            val imageFile = File(imageDir, fileName)
            inputStream.use { input ->
                FileOutputStream(imageFile).use { output ->
                    input.copyTo(output)
                }
            }
            imageFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Saves a bitmap to the app's internal storage.
     * Returns the local file path as a string.
     */
    fun saveBitmap(context: Context, bitmap: Bitmap): String? {
        return try {
            val fileName = "recipe_${UUID.randomUUID()}.jpg"
            val imageDir = File(context.filesDir, "images")
            imageDir.mkdirs()

            val imageFile = File(imageDir, fileName)
            FileOutputStream(imageFile).use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
            }
            imageFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Deletes an image file by path.
     */
    fun deleteImage(imagePath: String?) {
        if (!imagePath.isNullOrEmpty()) {
            try {
                File(imagePath).delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getImageUri(context: Context, imagePath: String): Uri {
        return FileProvider.getUriForFile(
            context,
            "com.example.chopchoprecipeapp.fileprovider",
            File(imagePath)
        )
    }
}