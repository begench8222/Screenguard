package com.annaorazov.screenguard.learn_game

import androidx.room.*

@Dao
interface QuestionProgressDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(progress: QuestionProgressEntity)

    @Query("SELECT * FROM question_progress WHERE subject = :subject AND classLevel = :classLevel")
    suspend fun getProgressForSubject(subject: String, classLevel: Int): List<QuestionProgressEntity>

    @Query("SELECT * FROM question_progress WHERE subject = :subject AND classLevel = :classLevel AND questionIndex = :questionIndex")
    suspend fun getProgressForQuestion(subject: String, classLevel: Int, questionIndex: Int): QuestionProgressEntity?

    @Query("DELETE FROM question_progress WHERE subject = :subject AND classLevel = :classLevel")
    suspend fun clearProgressForSubject(subject: String, classLevel: Int)
}