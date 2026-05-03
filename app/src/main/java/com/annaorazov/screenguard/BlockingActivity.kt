package com.annaorazov.screenguard

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import com.annaorazov.screenguard.databinding.ActivityBlockingBinding

class BlockingActivity : Activity() {

    private lateinit var binding: ActivityBlockingBinding
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(SwitchLanguageHelper.applyLanguage(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        binding = ActivityBlockingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Убираем флаги, если требуется интерактивность
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)

        val packageName = intent.getStringExtra("packageName") ?: ""
        if (packageName.isNotEmpty()) {
            val packageManager = packageManager
            val appName = packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
            val appIcon = packageManager.getApplicationIcon(packageName)

            binding.appName.text = appName
            binding.appIcon.setImageDrawable(appIcon)
        } else {
            binding.appName.text = "Unknown App"
        }

    }
}