package com.annaorazov.screenguard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.annaorazov.screenguard.MainActivity.Companion.hasAllPermissions
import com.annaorazov.screenguard.learn_game.LearnGameActivity
import com.annaorazov.screenguard.data.AppDatabase
import com.annaorazov.screenguard.data.ConditionalAppEntity
import com.annaorazov.screenguard.databinding.ActivityPasswordEntryBinding
import com.annaorazov.screenguard.ui.conditionalapps.ConditionalAppsAdapter
import com.annaorazov.screenguard.utils.PasswordManager
import com.annaorazov.screenguard.utils.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PasswordEntryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPasswordEntryBinding
    private lateinit var passwordManager: PasswordManager
    private lateinit var conditionalAppsAdapter: ConditionalAppsAdapter

    private val sharedPreferencesKey = "dailyTimeLimit"
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val isShowGamesEnabled = intent?.getBooleanExtra("show_games_switch", false) ?: false
            updateGameVisibility(isShowGamesEnabled)
        }
    }
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(SwitchLanguageHelper.applyLanguage(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!MainActivity.hasAllPermissions(this)) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }
        binding = ActivityPasswordEntryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ContextCompat.registerReceiver(
            this,
            receiver,
            IntentFilter("com.annaorazov.screenguard.UPDATE_GAME_VISIBILITY"),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val isShowGamesEnabled = sharedPreferences.getBoolean("show_games_switch", false)

        CoroutineScope(Dispatchers.IO).launch {
            val conditionalAppDao = AppDatabase.getInstance(this@PasswordEntryActivity).conditionalAppDao()
            val conditionalApps = conditionalAppDao.getAll()

            withContext(Dispatchers.Main) {
                if (conditionalApps.isNotEmpty()) {
                    updateGameVisibility(isShowGamesEnabled)
                }
            }
        }

        passwordManager = PasswordManager(this)

        binding.buttonSubmit.setOnClickListener {
            val enteredPassword = binding.editTextPassword.text.toString()
            if (passwordManager.validatePassword(enteredPassword)) {
                val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                sharedPreferences.edit().putBoolean("password_verified", true).apply()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, getString(R.string.wrong_password), Toast.LENGTH_SHORT).show()
            }
        }

        binding.buttonChangePassword.setOnClickListener {
            startActivity(Intent(this, SetPasswordActivity::class.java))
        }

        conditionalAppsAdapter = ConditionalAppsAdapter(packageManager, {}, false)
        binding.conditionalAppsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.conditionalAppsRecyclerView.adapter = conditionalAppsAdapter
        binding.constraintLayout.setOnClickListener {
            val intent = Intent(this, LearnGameActivity::class.java)
            startActivity(intent)
        }
        binding.learnGameIcon.setOnClickListener {
            val intent = Intent(this, LearnGameActivity::class.java)
            startActivity(intent)
        }
        updateConditionalAppsList()
        loadSetTime()
        setupRemainingTimeDisplay()
    }

    override fun onResume() {
        super.onResume()
        setupRemainingTimeDisplay()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }

    private fun setupRemainingTimeDisplay() {
        TimeUtils.setupTimeDisplay(this, binding.totalUsageTime, includePrefix = true)
    }

    private fun updateGameVisibility(isVisible: Boolean) {
        if (isVisible) {
            binding.learnGameIcon.visibility = View.VISIBLE
            binding.learnGameText.visibility = View.VISIBLE
            binding.constraintLayout.visibility = View.VISIBLE
        } else {
            binding.learnGameIcon.visibility = View.GONE
            binding.learnGameText.visibility = View.GONE
            binding.constraintLayout.visibility = View.GONE
        }
    }

    fun updateConditionalAppsList() {
        val conditionalAppDao = AppDatabase.getInstance(this).conditionalAppDao()
        CoroutineScope(Dispatchers.IO).launch {
            val conditionalApps = conditionalAppDao.getAll()
            withContext(Dispatchers.Main) {
                conditionalAppsAdapter.updateData(conditionalApps)
                updateVisibility(conditionalApps)
            }
        }
    }

    private fun updateVisibility(conditionalApps: List<ConditionalAppEntity>) {
        val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val timeInMillis = sharedPreferences.getLong(sharedPreferencesKey, 0)
        if (conditionalApps.isEmpty() || (timeInMillis <= 0)) {
            binding.conditionalContent.visibility = View.GONE
        } else {
            binding.conditionalContent.visibility = View.VISIBLE
        }
    }

    private fun loadSetTime() {
        val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val timeInMillis = sharedPreferences.getLong(sharedPreferencesKey, 0)
        if (timeInMillis > 0) {
            val hour = (timeInMillis / (60 * 60 * 1000)) % 24
            val minute = (timeInMillis % (60 * 60 * 1000)) / (60 * 1000)
            updateSetTimeDisplay(hour.toInt(), minute.toInt())
        } else {
            binding.setTimeDisplay.text = getString(R.string.set_time_havent_set_yet_passfr)
        }
    }

    private fun updateSetTimeDisplay(hour: Int, minute: Int) {
        val timeString = if (hour > 0) {
            String.format("%02ds %02d min", hour, minute)
        } else {
            String.format("%d min", minute)
        }
        binding.setTimeDisplay.text = getString(R.string.set_time_passfr) + " $timeString"
    }
}