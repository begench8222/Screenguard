package com.annaorazov.screenguard

import android.app.AppOpsManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.annaorazov.screenguard.databinding.ActivityMainBinding
import com.annaorazov.screenguard.ui.apps.AppsFragment
import com.annaorazov.screenguard.utils.PasswordManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var passwordManager: PasswordManager
    private var permissionsRequested = false
    private val REQUEST_CODE_NOTIFICATIONS = 1003

    private val startForUsageStats = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (hasUsageStatsPermission()) {
            proceedAfterPermissions()
        } else {
            finish()
        }
    }

    private val startForDeviceAdmin = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            Log.d("DeviceAdminPermission", "Device admin permission granted.")
            proceedAfterPermissions()
        } else {
            Log.d("DeviceAdminPermission", "Device admin permission denied.")
            finish()
        }
    }

    private val startForOverlay = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (Settings.canDrawOverlays(this)) {
            startTrackingService()
        } else {
            finish()
        }
    }
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(SwitchLanguageHelper.applyLanguage(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. First check and request permissions
        if (!hasAllPermissions()) {
            requestPermissions()
            return
        }

        // 2. Check password
        passwordManager = PasswordManager(this)
        val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val isPasswordVerified = sharedPreferences.getBoolean("password_verified", false)

        if (!passwordManager.isPasswordSet()) {
            startActivity(Intent(this, SetPasswordActivity::class.java))
            finish()
            return
        } else if (!isPasswordVerified) {
            startActivity(Intent(this, PasswordEntryActivity::class.java))
            finish()
            return
        }

        // 3. Clear the verification flag
        sharedPreferences.edit().putBoolean("password_verified", false).apply()

        // 4. Initialize main UI
        initMainUI()
    }

    private fun hasAllPermissions(): Boolean {
        return hasUsageStatsPermission() &&
                isAccessibilityServiceEnabled() &&
                (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                        ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
                isDeviceAdminEnabled()
    }

    private fun isDeviceAdminEnabled(): Boolean {
        val devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val componentName = ComponentName(this, AdminReceiver::class.java)
        return devicePolicyManager.isAdminActive(componentName)
    }

    private fun proceedAfterPermissions() {
        if (hasAllPermissions()) {
            recreate()
        } else {
            finish()
        }
    }

    private fun initMainUI() {
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (intent?.getBooleanExtra("request_device_admin_only", false) == true) {
            requestDeviceAdmin()
            return
        }

        if (intent?.getBooleanExtra("request_notifications_only", false) == true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_CODE_NOTIFICATIONS
                )
            }
            return
        }
        setSupportActionBar(binding.toolbar)
        createNotificationChannel()
        startTrackingService()
        setupViewPager()
        setupBottomNavigationView()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "tracking_channel",
            "App Tracking Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "ScreenGuard"
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun setupViewPager() {
        val viewPager = binding.viewPager
        val adapter = ViewPagerAdapter(this)
        viewPager.adapter = adapter

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                when (position) {
                    1 -> showToolbar()
                    else -> hideToolbar()
                }
            }
        })
    }

    private fun setupBottomNavigationView() {
        val bottomNavView = binding.navView
        val viewPager = binding.viewPager

        bottomNavView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_apps -> viewPager.setCurrentItem(1, true)
                R.id.navigation_statistics -> viewPager.setCurrentItem(0, true)
                R.id.navigation_blocked_apps -> viewPager.setCurrentItem(3, true)
                R.id.navigation_conditional_apps -> viewPager.setCurrentItem(2, true)
            }
            true
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                when (position) {
                    1 -> bottomNavView.selectedItemId = R.id.navigation_apps
                    0 -> bottomNavView.selectedItemId = R.id.navigation_statistics
                    3 -> bottomNavView.selectedItemId = R.id.navigation_blocked_apps
                    2 -> bottomNavView.selectedItemId = R.id.navigation_conditional_apps
                }
            }
        })
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expectedComponentName = ComponentName(this, MyAccessibilityService::class.java)
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        return enabledServices.split(":").any {
            ComponentName.unflattenFromString(it) == expectedComponentName
        }
    }

    private fun requestPermissions() {
        if (permissionsRequested) return
        permissionsRequested = true

        if (!hasUsageStatsPermission()) {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            startForUsageStats.launch(intent)
        }

        if (!isAccessibilityServiceEnabled()) {
            requestAccessibilityPermission()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_CODE_NOTIFICATIONS
                )
            }
        }

        if (!isDeviceAdminEnabled()) {
            requestDeviceAdmin()
        }
    }

    private fun requestAccessibilityPermission() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun startTrackingService() {
        if (Settings.canDrawOverlays(this)) {
            val serviceIntent = Intent(this, AppTrackingService::class.java)
            ContextCompat.startForegroundService(this, serviceIntent)
        } else {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startForOverlay.launch(intent)
        }
    }

    private fun requestDeviceAdmin() {
        val devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val componentName = ComponentName(this, AdminReceiver::class.java)

        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
        intent.putExtra(
            DevicePolicyManager.EXTRA_ADD_EXPLANATION,
            "Please enable device admin to enforce app usage limits."
        )
        startForDeviceAdmin.launch(intent)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_NOTIFICATIONS) {
            proceedAfterPermissions()
        }
    }

    private fun showToolbar() {
        binding.toolbar.visibility = View.VISIBLE
    }

    private fun hideToolbar() {
        binding.toolbar.visibility = View.GONE
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = true

            override fun onQueryTextChange(newText: String?): Boolean {
                val currentFragment = supportFragmentManager.findFragmentByTag("f${binding.viewPager.currentItem}")
                if (currentFragment is AppsFragment) {
                    currentFragment.filterApps(newText)
                }
                return true
            }
        })

        return true
    }

    companion object {
        fun hasAllPermissions(context: Context): Boolean {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val usageStatsMode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )

            val accessibilityEnabled = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )?.contains(MyAccessibilityService::class.java.name) ?: false

            val notificationsGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

            val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val componentName = ComponentName(context, AdminReceiver::class.java)
            val deviceAdminEnabled = devicePolicyManager.isAdminActive(componentName)

            return usageStatsMode == AppOpsManager.MODE_ALLOWED &&
                    accessibilityEnabled &&
                    notificationsGranted &&
                    deviceAdminEnabled
        }
    }
}