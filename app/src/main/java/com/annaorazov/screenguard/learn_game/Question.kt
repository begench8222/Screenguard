package com.annaorazov.screenguard.learn_game

import android.content.Context

data class Question(
    val id: Int,
    val questionText: String,
    val image: String?,  // Now just contains filename (e.g., "english_11_1.png")
    val options: List<String>,
    val correctAnswerIndex: Int
) {
    fun hasImage(context: Context, classLevel: Int): Boolean {
        if (image.isNullOrEmpty()) return false
        return try {
            val path = "class_$classLevel/images/$image"
            context.assets.open(path).close()
            true
        } catch (e: Exception) {
            false
        }
    }
}