package com.annaorazov.screenguard

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.github.appintro.AppIntro2
import com.github.appintro.AppIntroBase
import com.github.appintro.AppIntroFragment

class IntroActivity : AppIntro2() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(SwitchLanguageHelper.applyLanguage(newBase))
    }

    // Лаунчер для overlay (appear on top)
    private val overlayLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    // Лаунчер для device admin (ACTION_ADD_DEVICE_ADMIN возвращает resultCode)
    private val deviceAdminLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Toast.makeText(this, getString(R.string.device_admin_granted), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, getString(R.string.device_admin_denied), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (MainActivity.hasAllPermissions(this)) {
            return startActivity(Intent(this, MainActivity::class.java))
        }
        setIndicatorColor(
            selectedIndicatorColor = getColor(R.color.ic_launcher_background),
            unselectedIndicatorColor = getColor(R.color.white)
        )
        addSlide(ImageSlideFragment.newInstance(
            title = "Screenguard",
            desc = getString(R.string.use_your_child_entertainment),
            imageRes = R.drawable.main_icon,
            bgColorRes = R.color.background_color_map
        ))


        // Слайды разрешений — каждый как отдельный фрагмент-Policy
        addSlide(PermissionSlideFragment.newInstance(PermissionSlideFragment.TYPE_USAGE))
        addSlide(PermissionSlideFragment.newInstance(PermissionSlideFragment.TYPE_ACCESSIBILITY))
        addSlide(PermissionSlideFragment.newInstance(PermissionSlideFragment.TYPE_DEVICE_ADMIN))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            addSlide(PermissionSlideFragment.newInstance(PermissionSlideFragment.TYPE_NOTIFICATIONS))
        }
        addSlide(PermissionSlideFragment.newInstance(PermissionSlideFragment.TYPE_OVERLAY))

        addSlide(ImageSlideFragment.newInstance(
            title = getString(R.string.success),
            desc = getString(R.string.to_start_app_touch_below_right_done_button),
            imageRes = R.drawable.settings_icon,
            bgColorRes = R.color.background_color,
            lottieRes = R.raw.done
        ))

        // Wizard mode (back ← вместо skip) — и блокируем system back
        isWizardMode = true
        isSystemBackButtonLocked = true

        // По умолчанию показываем кнопки — SlidePolicy будет блокировать переход когда нужно
        isButtonsEnabled = true
    }

    // --- Методы, которые фрагменты будут вызывать для запуска запроса ---
    fun requestUsageStatsPermission() {
        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }

    fun requestAccessibilityPermission() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    fun requestDeviceAdmin() {
        val devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val componentName = ComponentName(this, AdminReceiver::class.java)

        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Enable device admin to enforce limits")
        }
        deviceAdminLauncher.launch(intent)
    }

    fun requestNotificationPermission() {
        // для Android 13+ (TIRAMISU) — стандартный запрос runtime
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1003)
        } else {
            Toast.makeText(this, getString(R.string.notification_required), Toast.LENGTH_SHORT).show()
        }
    }

    fun requestOverlayPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
        overlayLauncher.launch(intent)
    }

    // --- Функции проверки, которые SlidePolicy использует ---
    fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        val mode = appOps.checkOpNoThrow(
            android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(), packageName
        )
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }

    fun isAccessibilityServiceEnabled(): Boolean {
        val expected = ComponentName(this, MyAccessibilityService::class.java)
        val enabledServices = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        return enabledServices.split(":").any { ComponentName.unflattenFromString(it) == expected }
    }

    fun isDeviceAdminEnabled(): Boolean {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val componentName = ComponentName(this, AdminReceiver::class.java)
        return dpm.isAdminActive(componentName)
    }

    fun areNotificationsGranted(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
    override fun onDonePressed(currentFragment: Fragment?) {
        super.onDonePressed(currentFragment)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
