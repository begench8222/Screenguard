package com.annaorazov.screenguard.learn_game

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "question_progress")
data class QuestionProgressEntity(
    @PrimaryKey
    val id: String, // Формат: "subject_classLevel_questionIndex"
    val subject: String,
    val classLevel: Int,
    val questionIndex: Int,
    val isCorrect: Boolean,
    val selectedAnswer: Int
)