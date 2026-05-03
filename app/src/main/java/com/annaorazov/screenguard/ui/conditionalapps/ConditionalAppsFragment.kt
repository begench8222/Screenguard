package com.annaorazov.screenguard.ui.conditionalapps

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.annaorazov.screenguard.AppTrackingService
import com.annaorazov.screenguard.R
import com.annaorazov.screenguard.databinding.FragmentConditionalAppsBinding
import com.annaorazov.screenguard.utils.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.annaorazov.screenguard.data.AppDatabase
import com.annaorazov.screenguard.data.ConditionalAppEntity
import java.util.Calendar

class ConditionalAppsFragment : Fragment() {

    private var _binding: FragmentConditionalAppsBinding? = null
    private val binding get() = _binding!!
    private lateinit var conditionalAppsAdapter: ConditionalAppsAdapter
    private val sharedPreferencesKey = "dailyTimeLimit"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConditionalAppsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize RecyclerView
        conditionalAppsAdapter = ConditionalAppsAdapter(
            requireContext().packageManager,
            { conditionalApp ->
                removeAppFromConditionalList(conditionalApp)
            },
            true
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = conditionalAppsAdapter

        // Load conditional apps
        updateConditionalAppsList()

        // Set up the time picker button
        binding.timePickerButton.setOnClickListener {
            showTimePicker()
        }

        // Load and display the set time from SharedPreferences
        loadSetTime()

        // Setup remaining time display
        setupRemainingTimeDisplay()
    }

    private fun setupRemainingTimeDisplay() {
        TimeUtils.setupTimeDisplay(requireContext(), binding.totalUsageTime, includePrefix = true)
    }

    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(
            requireContext(),
            { _, selectedHour, selectedMinute ->
                saveSetTime(selectedHour, selectedMinute)
                updateSetTimeDisplay(selectedHour, selectedMinute)
            },
            hour,
            minute,
            true
        )

        timePickerDialog.show()
    }

    private fun saveSetTime(hour: Int, minute: Int) {
        val sharedPreferences = requireContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val timeInMillis = (hour.toLong() * 60 * 60 * 1000) + (minute.toLong() * 60 * 1000)
        editor.putLong(sharedPreferencesKey, timeInMillis)
        editor.apply()

        // Notify AppTrackingService
        val intent = Intent(requireContext(), AppTrackingService::class.java).apply {
            action = "com.annaorazov.screenguard.ACTION_SET_TIME_CHANGED"
        }
        requireContext().startService(intent)

        // Update remaining time display after changing the set time
        setupRemainingTimeDisplay()
    }

    private fun loadSetTime() {
        val sharedPreferences = requireContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val timeInMillis = sharedPreferences.getLong(sharedPreferencesKey, 0)
        if (timeInMillis > 0) {
            val hour = (timeInMillis / (60 * 60 * 1000)) % 24
            val minute = (timeInMillis % (60 * 60 * 1000)) / (60 * 1000)
            updateSetTimeDisplay(hour.toInt(), minute.toInt())
        }
    }

    private fun updateSetTimeDisplay(hour: Int, minute: Int) {
        val timeString = if (hour > 0) {
            String.format("%02ds %02d min", hour, minute)
        } else {
            String.format("%d min", minute)
        }
        binding.setTimeDisplay.text = getString(R.string.set_time_show, timeString)
    }

    override fun onResume() {
        super.onResume()
        updateConditionalAppsList()
        setupRemainingTimeDisplay()
    }

    private fun removeAppFromConditionalList(conditionalApp: ConditionalAppEntity) {
        val conditionalAppDao = AppDatabase.getInstance(requireContext()).conditionalAppDao()
        CoroutineScope(Dispatchers.IO).launch {
            conditionalAppDao.deleteAppByPackageName(conditionalApp.packageName)
            withContext(Dispatchers.Main) {
                updateConditionalAppsList()
            }
        }
    }

    fun updateConditionalAppsList() {
        val conditionalAppDao = AppDatabase.getInstance(requireContext()).conditionalAppDao()
        CoroutineScope(Dispatchers.IO).launch {
            val conditionalApps = conditionalAppDao.getAll()
            withContext(Dispatchers.Main) {
                conditionalAppsAdapter.updateData(conditionalApps)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}