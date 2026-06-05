package com.annaorazov.screenguard.learn_game

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [QuestionProgressEntity::class],
    version = 1,
    exportSchema = false
)
abstract class QuestionProgressDatabase : RoomDatabase() {
    abstract fun questionProgressDao(): QuestionProgressDao

    companion object {
        @Volatile
        private var INSTANCE: QuestionProgressDatabase? = null

        fun getDatabase(context: Context): QuestionProgressDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    QuestionProgressDatabase::class.java,
                    "question_progress_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}