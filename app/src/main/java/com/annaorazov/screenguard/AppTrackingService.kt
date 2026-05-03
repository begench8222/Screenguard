package com.annaorazov.screenguard

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.annaorazov.screenguard.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

class AppTrackingService : Service() {
    private val scope = CoroutineScope(Dispatchers.IO)
    private lateinit var sharedPreferences: SharedPreferences
    private var lastCheckTime: Long = 0
    private var isTrackingEnabled = true
    private val lastAppCheckTime = mutableMapOf<String, Long>()
    private var isCurrentlyTrackingConditionalApp = false // Add this flag

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(SwitchLanguageHelper.applyLanguage(newBase))
    }

    private val screenStateReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    isTrackingEnabled = false
                    sendAccessibilityPauseSignal()
                    Log.d("AppTrackingService", "Screen off, tracking and accessibility paused")
                }
                Intent.ACTION_SCREEN_ON -> {
                    isTrackingEnabled = true
                    sendAccessibilityResumeSignal()
                    Log.d("AppTrackingService", "Screen on, tracking and accessibility resumed")
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)

        val screenFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenStateReceiver, screenFilter)

        Log.d("AppTrackingService", "Service created and receivers registered")
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(screenStateReceiver)
        Log.d("AppTrackingService", "Service destroyed and receivers unregistered")
    }

    private fun sendAccessibilityPauseSignal() {
        val intent = Intent("com.annaorazov.ACCESSIBILITY_PAUSE")
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun sendAccessibilityResumeSignal() {
        val intent = Intent("com.annaorazov.ACCESSIBILITY_RESUME")
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "com.annaorazov.screenguard.ACTION_SET_TIME_CHANGED") {
            Log.d("AppTrackingService", "Set Time changed, refreshing state...")
            refreshBlockingLogic()
        }

        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                1,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(
                1,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        }

        startTracking()
        debugTimingAccuracy()
        Log.d("AppTrackingService", "Service started and tracking initiated")

        return START_STICKY
    }

    private fun refreshBlockingLogic() {
        scope.launch {
            val setTime = sharedPreferences.getLong("dailyTimeLimit", 0)
            val conditionalAppDao = AppDatabase.getInstance(this@AppTrackingService).conditionalAppDao()
            val totalUsageTime = conditionalAppDao.getAll().sumOf { it.usageTime }

            if (totalUsageTime >= setTime) {
                Log.d("AppTrackingService", "Blocking paused due to Set Time change")
            } else {
                Log.d("AppTrackingService", "Blocking resumed due to Set Time change")
            }
        }
    }

    private fun createNotification(): android.app.Notification {
        val channelId = "tracking_channel"
        val channelName = "ScreenGuard Tracking"

        val channel = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            setShowBadge(false)
            lockscreenVisibility = android.app.Notification.VISIBILITY_SECRET
        }

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("ScreenGuard")
            .setContentText(getString(R.string.screenguard_is_working))
            .setSmallIcon(R.drawable.main_icon)
            .setOngoing(true)
            .setAutoCancel(false)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun shouldBlockSettings(packageName: String): Boolean {
        val settingsPackageName = "com.android.settings"
        if (packageName != settingsPackageName) return false
        return sharedPreferences.getBoolean("block_settings_switch", false)
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val accessibilityEnabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )?.contains(MyAccessibilityService::class.java.name) ?: false
        return accessibilityEnabled
    }

    private fun startTracking() {
        // Settings blocking loop - keep this separate
        scope.launch {
            while (true) {
                if (isTrackingEnabled && isAccessibilityServiceEnabled()) {
                    val foregroundApp = getForegroundApp()
                    if (foregroundApp == "com.android.settings" && shouldBlockSettings(foregroundApp)) {
                        Log.d("AppTrackingService", "Blocking Settings app")
                        showBlockingWindow(foregroundApp)
                    }
                }
                delay(1000) // Check every second
            }
        }

        // SINGLE tracking loop for conditional and blocked apps
        scope.launch {
            while (true) {
                if (isTrackingEnabled) {
                    resetUsageTimeIfNewDay()
                    val foregroundApp = getForegroundApp()
                    if (foregroundApp != null) {
                        Log.d("AppTrackingService", "Foreground app: $foregroundApp")

                        // Track conditional apps
                        if (isConditionalApp(foregroundApp)) {
                            trackConditionalAppUsage(foregroundApp)
                        }

                        // Check blocked apps
                        if (isBlockedApp(foregroundApp) && !isBlockingPaused()) {
                            Log.d("AppTrackingService", "Blocking app: $foregroundApp")
                            showBlockingWindow(foregroundApp)
                        }
                    }
                }
                delay(1000) // Wait exactly 1 second between checks
            }
        }
    }

    private suspend fun resetUsageTimeIfNewDay() {
        val lastResetDate = sharedPreferences.getLong("lastResetDate", 0)
        val calendar = Calendar.getInstance()
        val currentDay = calendar.get(Calendar.DAY_OF_YEAR)

        calendar.timeInMillis = lastResetDate
        val lastResetDay = calendar.get(Calendar.DAY_OF_YEAR)

        if (currentDay != lastResetDay) {
            val conditionalAppDao = AppDatabase.getInstance(this).conditionalAppDao()
            val conditionalApps = conditionalAppDao.getAll()
            conditionalApps.forEach { app ->
                app.usageTime = 0
                conditionalAppDao.updateConditionalApp(app)
            }
            sharedPreferences.edit().putBoolean("notificationSent", false).apply()
            sharedPreferences.edit().putLong("lastResetDate", System.currentTimeMillis()).apply()
            Log.d("AppTrackingService", "Usage time reset for new day")
            sharedPreferences.edit().putLong("bonusTime", 0L).apply()
        }
    }

    private suspend fun isBlockingPaused(): Boolean {
        val setTime = sharedPreferences.getLong("dailyTimeLimit", 0)
        val bonusTime = sharedPreferences.getLong("bonusTime", 0)
        val conditionalAppDao = AppDatabase.getInstance(this).conditionalAppDao()
        val totalUsageTime = conditionalAppDao.getAll().sumOf { it.usageTime }
        return (totalUsageTime + bonusTime) >= setTime
    }

    private fun getForegroundApp(): String? {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.HOUR, -1)
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            calendar.timeInMillis,
            System.currentTimeMillis()
        )
        return stats.maxByOrNull { it.lastTimeUsed }?.packageName
    }

    private suspend fun isConditionalApp(packageName: String): Boolean {
        val conditionalAppDao = AppDatabase.getInstance(this).conditionalAppDao()
        return conditionalAppDao.getAppByPackageName(packageName) != null
    }

    private suspend fun isBlockedApp(packageName: String): Boolean {
        val blockedAppDao = AppDatabase.getInstance(this).blockedAppDao()
        return blockedAppDao.getAppByPackageName(packageName) != null
    }

    private suspend fun trackConditionalAppUsage(packageName: String) {
        // Prevent multiple simultaneous tracking of the same app
        if (isCurrentlyTrackingConditionalApp) {
            return
        }

        isCurrentlyTrackingConditionalApp = true

        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            val isScreenOn = powerManager.isInteractive
            if (!isScreenOn || !isTrackingEnabled) {
                return
            }

            val currentTime = System.currentTimeMillis()
            val lastTime = lastAppCheckTime[packageName] ?: currentTime
            val timeDifference = currentTime - lastTime

            // Ensure time difference is reasonable (not too large due to app switching)
            if (timeDifference > 5000) { // If more than 5 seconds, it's probably app switching
                Log.d("AppTrackingService", "Large time gap detected, likely app switch: $timeDifference ms")
                lastAppCheckTime[packageName] = currentTime
                return
            }

            // Check if inactivity deduction is enabled
            val isInactivityDeductionEnabled = sharedPreferences.getBoolean("inactivity_deduction_switch", true)

            var shouldCountTime = timeDifference in 1..2000 // Increased range to 2 seconds

            if (isInactivityDeductionEnabled && shouldCountTime) {
                // Get last interaction time from SharedPreferences
                val lastInteractionTime = sharedPreferences.getLong("lastInteractionTime", currentTime)
                val timeSinceLastTouch = currentTime - lastInteractionTime

                // Only count time if user was active (touched screen within last 10 seconds)
                if (timeSinceLastTouch > 10000) {
                    shouldCountTime = false
                    Log.d("AppTrackingService", "Skipping time update for $packageName due to inactivity")
                }
            }

            if (shouldCountTime) {
                updateAppUsageTime(packageName, timeDifference)
            }

            lastAppCheckTime[packageName] = currentTime

        } finally {
            isCurrentlyTrackingConditionalApp = false
        }
    }

    // Helper method to update app usage time
    private suspend fun updateAppUsageTime(packageName: String, timeDifference: Long) {
        val conditionalAppDao = AppDatabase.getInstance(this).conditionalAppDao()
        val conditionalApp = conditionalAppDao.getAppByPackageName(packageName)
        if (conditionalApp != null) {
            val previousUsageTime = conditionalApp.usageTime
            conditionalApp.usageTime += timeDifference
            conditionalAppDao.updateConditionalApp(conditionalApp)

            // Log the exact time being added for debugging
            Log.d("AppTrackingService",
                "Updated $packageName: +${timeDifference}ms (${timeDifference/1000.0}sec), " +
                        "Total: ${conditionalApp.usageTime}ms (${conditionalApp.usageTime/1000.0}sec)")

            val setTime = sharedPreferences.getLong("dailyTimeLimit", 0)
            if (conditionalApp.usageTime >= setTime && !sharedPreferences.getBoolean("notificationSent", false)) {
                sendNotification()
                sharedPreferences.edit().putBoolean("notificationSent", true).apply()
            }
        }
    }

    private fun debugTimingAccuracy() {
        scope.launch {
            var realTimeStart = System.currentTimeMillis()
            var lastLoggedTime = realTimeStart

            while (true) {
                delay(30000) // Check every 30 seconds for more frequent feedback

                val currentTime = System.currentTimeMillis()
                val realTimePassed = currentTime - realTimeStart

                // Get current total usage time from database
                val conditionalAppDao = AppDatabase.getInstance(this@AppTrackingService).conditionalAppDao()
                val totalUsageTime = conditionalAppDao.getAll().sumOf { it.usageTime }

                val accuracyRatio = if (realTimePassed > 0) {
                    totalUsageTime.toDouble() / realTimePassed.toDouble()
                } else {
                    0.0
                }

                Log.d("TimingDebug",
                    "Real time: ${realTimePassed/1000}s, " +
                            "Tracked time: ${totalUsageTime/1000}s, " +
                            "Ratio: ${"%.3f".format(accuracyRatio)}")

                if (accuracyRatio > 1.1) {
                    Log.w("TimingDebug", "TIMING TOO FAST - Multiple tracking loops detected!")
                } else if (accuracyRatio < 0.9) {
                    Log.w("TimingDebug", "TIMING TOO SLOW - Tracking may be missing time!")
                }

                lastLoggedTime = currentTime
            }
        }
    }

    private fun sendNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            "tracking_channel",
            "Tracking Notifications",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.notify_desc)
            enableLights(true)
            enableVibration(true)
        }
        notificationManager.createNotificationChannel(channel)
        sharedPreferences.edit().putBoolean("notificationSent", true).apply()
        val notification = NotificationCompat.Builder(this, "tracking_channel")
            .setContentTitle("ScreenGuard")
            .setContentText(getString(R.string.blocked_apps_are_available_for_today))
            .setSmallIcon(R.drawable.ic_menu_more)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()
        notificationManager.notify(1, notification)
        Log.d("AppTrackingService", "Notification sent for app usage limit")
    }

    private fun showBlockingWindow(packageName: String) {
        val intent = Intent(this, BlockingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("packageName", packageName)
        }
        startActivity(intent)
        Log.d("AppTrackingService", "Blocking window shown for app: $packageName")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}