package com.annaorazov.screenguard

import android.accessibilityservice.AccessibilityService
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class MyAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private val checkInterval = 1000L // Интервал проверки бездействия (10 секунд)
    private var lastInteractionTime = System.currentTimeMillis()
    private var isProcessingEnabled = true // Флаг для управления обработкой событий
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(SwitchLanguageHelper.applyLanguage(newBase))
    }

    // BroadcastReceiver для управления паузой/возобновлением обработки
    private val accessibilityControlReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "com.annaorazov.ACCESSIBILITY_PAUSE" -> {
                    isProcessingEnabled = false
                    handler.removeCallbacks(interactionChecker) // Останавливаем проверку бездействия
                    Log.d("MyAccessibilityService", "Accessibility processing paused")
                }
                "com.annaorazov.ACCESSIBILITY_RESUME" -> {
                    isProcessingEnabled = true
                    handler.post(interactionChecker) // Возобновляем проверку бездействия
                    Log.d("MyAccessibilityService", "Accessibility processing resumed")
                }
            }
        }
    }

    // Runnable для проверки бездействия
    private val interactionChecker = object : Runnable {
        override fun run() {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastInteractionTime >= checkInterval) {
                Log.d("MyAccessibilityService", "10 seconds of inactivity detected")
                // Если экран не был нажат в течение 10 секунд, отправляем время последнего взаимодействия
                sendLastInteractionTime()
            }
            handler.postDelayed(this, checkInterval) // Повторяем проверку
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Регистрация BroadcastReceiver для управления паузой/возобновлением
        val filter = IntentFilter().apply {
            addAction("com.annaorazov.ACCESSIBILITY_PAUSE")
            addAction("com.annaorazov.ACCESSIBILITY_RESUME")
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(accessibilityControlReceiver, filter)

        handler.post(interactionChecker) // Запускаем проверку бездействия
        Log.d("MyAccessibilityService", "Service connected and receiver registered")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isProcessingEnabled) {
            Log.d("MyAccessibilityService", "Processing disabled, ignoring event")
            return
        }

        // Обновляем время последнего взаимодействия при каждом событии доступности
        lastInteractionTime = System.currentTimeMillis()
        Log.d("MyAccessibilityService", "Interaction detected at $lastInteractionTime")
        sendLastInteractionTime() // Отправляем время сразу после взаимодействия
    }

    override fun onInterrupt() {
        Log.d("MyAccessibilityService", "Service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(interactionChecker) // Останавливаем проверку
        LocalBroadcastManager.getInstance(this).unregisterReceiver(accessibilityControlReceiver)
        Log.d("MyAccessibilityService", "Service destroyed and receiver unregistered")
    }

    // Метод для отправки времени последнего взаимодействия через LocalBroadcastManager
    private fun sendLastInteractionTime() {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastTouch = currentTime - lastInteractionTime

        // Convert to readable format
        val seconds = timeSinceLastTouch / 1000
        val minutes = seconds / 60
        val hours = minutes / 60

        val readableTimeSinceLastTouch = when {
            hours > 0 -> "${hours}h ${minutes % 60}m ${seconds % 60}s"
            minutes > 0 -> "${minutes}m ${seconds % 60}s"
            else -> "${seconds}s"
        }
        val intent = Intent("com.annaorazov.UPDATE_INTERACTION_TIME").apply {
            putExtra("lastInteractionTime", lastInteractionTime)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().putLong("lastInteractionTime", lastInteractionTime).apply()
        Log.d("MyAccessibilityService", "past after last touch: $readableTimeSinceLastTouch")
    }
}