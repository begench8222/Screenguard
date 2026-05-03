package com.annaorazov.screenguard.utils

import android.content.Context
import android.widget.TextView
import com.annaorazov.screenguard.R
import com.annaorazov.screenguard.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object TimeUtils {
    private const val PREFS_NAME = "AppPrefs"
    private const val DAILY_TIME_LIMIT = "dailyTimeLimit"
    private const val BONUS_TIME = "bonusTime"

    fun setupTimeDisplay(context: Context, remainingTimeText: TextView, includePrefix: Boolean = false) {
        CoroutineScope(Dispatchers.IO).launch {
            val remainingTimeMillis = getRemainingTime(context)
            withContext(Dispatchers.Main) {
                updateTimeDisplay(context, remainingTimeText, remainingTimeMillis, includePrefix)
            }
        }
    }

    suspend fun getRemainingTime(context: Context): Long {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val setTime = sharedPreferences.getLong(DAILY_TIME_LIMIT, 0)
        val bonusTime = sharedPreferences.getLong(BONUS_TIME, 0)
        val db = AppDatabase.getInstance(context)
        val totalUsageTime = db.conditionalAppDao().getAll().sumOf { it.usageTime }

        return if (setTime > (totalUsageTime + bonusTime)) {
            setTime - (totalUsageTime + bonusTime)
        } else {
            0L
        }
    }

    fun updateTimeDisplay(context: Context, textView: TextView, remainingTimeMillis: Long, includePrefix: Boolean) {
        val hours = remainingTimeMillis / (1000 * 60 * 60)
        val minutes = (remainingTimeMillis / (1000 * 60)) % 60
        val seconds = (remainingTimeMillis / 1000) % 60

        val timeString = when {
            hours > 0 -> "${hours}h ${minutes}min ${seconds}s"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}sec"
        }

        textView.text = if (includePrefix) {
            context.getString(R.string.left_time, timeString)
        } else {
            timeString
        }
    }

    fun addBonusTime(context: Context, bonusMillis: Long) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentBonusTime = sharedPreferences.getLong(BONUS_TIME, 0)
        sharedPreferences.edit().putLong(BONUS_TIME, currentBonusTime + bonusMillis).apply()
    }
}