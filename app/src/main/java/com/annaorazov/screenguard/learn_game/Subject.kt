package com.annaorazov.screenguard.learn_game

import com.annaorazov.screenguard.R

data class Subject(
    val key: String,
    val displayName: String
) {
    fun getIconResId(): Int {
        return when (key) {
            "math" -> R.drawable.math
            "russian" -> R.drawable.russian
            "english" -> R.drawable.english
            "literature" -> R.drawable.literature
            "history" -> R.drawable.history
            "geography" -> R.drawable.geography
            "biology" -> R.drawable.biology
            "algebra" -> R.drawable.algebra
            "turkmen_history" -> R.drawable.turkmen_history
            "geometry" -> R.drawable.geometry
            "physics" -> R.drawable.physics
            "chemistry" -> R.drawable.chemistry
            "informatics" -> R.drawable.computer
            else -> R.drawable.defaul
        }
    }
}