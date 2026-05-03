package com.annaorazov.screenguard.ui.statistics

import android.app.AlertDialog
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.annaorazov.screenguard.AppUsageAdapter
import com.annaorazov.screenguard.IconPieChartRenderer
import com.annaorazov.screenguard.IconPieEntry
import com.annaorazov.screenguard.MainActivity
import com.annaorazov.screenguard.R
import com.annaorazov.screenguard.SwitchLanguageHelper
import com.annaorazov.screenguard.data.AppDatabase
import com.annaorazov.screenguard.data.BlockedAppEntity
import com.annaorazov.screenguard.databinding.FragmentStatisticsBinding
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class StatisticsFragment : Fragment() {

    private lateinit var binding: FragmentStatisticsBinding
    private var appUsageMap: Map<String, Long> = emptyMap() // Cache for app usage data
    private var settingsDialog: AlertDialog? = null // Переменная для хранения диалога

    private val brightColors = listOf(
        Color.rgb(76, 175, 80),  // Bright Green
        Color.rgb(33, 150, 243), // Bright Blue
        Color.rgb(156, 39, 176), // Bright Purple
        Color.rgb(244, 67, 54),  // Bright Red
        Color.rgb(181, 101, 29)  // Bright Orange
    )
    private fun updateButtonIcon(button: ImageButton) {
        val lang = SwitchLanguageHelper.getLanguage(requireContext())
        val iconRes = when (lang) {
            "en" -> R.drawable.en
            "ru" -> R.drawable.ru
            "tk" -> R.drawable.tk
            else -> R.drawable.en
        }
        button.setBackgroundResource(iconRes)
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val btnLang = view.findViewById<ImageButton>(R.id.switchlang)
        updateButtonIcon(btnLang)

        // Обработчик для кнопки "Настройки"
        binding.settingsButton.setOnClickListener {
            showSettingsDialog()
        }

        // Загружаем данные
        loadAppUsageData()
        btnLang.setOnClickListener {
            val currentLang = SwitchLanguageHelper.getLanguage(requireContext())
            val newLang = when (currentLang) {
                "tk" -> "en"
                "en" -> "ru"
                else -> "tk"
            }
            SwitchLanguageHelper.saveLanguage(requireContext(), newLang)
            val pm = requireContext().packageManager
            val intent = pm.getLaunchIntentForPackage(requireContext().packageName)
            intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            if (intent != null) {
                startActivity(intent)
            }
            requireActivity().finish()
        }

    }

    private fun showSettingsDialog() {
        if (settingsDialog?.isShowing == true) {
            return
        }

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_settings, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setTitle(getString(R.string.settings_title))
            .setCancelable(false)
            .create()

        settingsDialog = dialog

        val sharedPreferences = requireContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)

        // Инициализация переключателей
        val blockSettingsSwitch = dialogView.findViewById<SwitchCompat>(R.id.dialog_block_settings_switch)
        val showGamesSwitch = dialogView.findViewById<SwitchCompat>(R.id.dialog_show_games_switch)
        val inactivityDeductionSwitch = dialogView.findViewById<SwitchCompat>(R.id.dialog_inactivity_deduction_switch) // NEW
        val classSpinner = dialogView.findViewById<Spinner>(R.id.classSpinner)

        // Установка текущих значений переключателей
        blockSettingsSwitch.isChecked = sharedPreferences.getBoolean("block_settings_switch", false)
        showGamesSwitch.isChecked = sharedPreferences.getBoolean("show_games_switch", false)
        inactivityDeductionSwitch.isChecked = sharedPreferences.getBoolean("inactivity_deduction_switch", true) // NEW - default to true

        // Настройка Spinner для выбора класса
        val classes = (1..12).toList()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, classes.map { it.toString() })
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        classSpinner.adapter = adapter
        val selectedClass = sharedPreferences.getInt("selected_class", 0)
        if (selectedClass > 0) {
            classSpinner.setSelection(selectedClass - 1)
        }

        // Обработчик для кнопки "ОК"
        dialogView.findViewById<Button>(R.id.dialog_ok_button).setOnClickListener {
            val newSelectedClass = classSpinner.selectedItem.toString().toInt()

            if (newSelectedClass == 0) {
                Toast.makeText(requireContext(),
                    getString(R.string.select_class_toast), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Сохранение состояния переключателей, выбранного класса и флага
            sharedPreferences.edit().apply {
                putBoolean("block_settings_switch", blockSettingsSwitch.isChecked)
                putBoolean("show_games_switch", showGamesSwitch.isChecked)
                putBoolean("inactivity_deduction_switch", inactivityDeductionSwitch.isChecked) // NEW
                putInt("selected_class", newSelectedClass)
                putBoolean("has_shown_initial_dialog", true)
                apply()
            }

            // Применение изменений
            if (blockSettingsSwitch.isChecked) {
                blockSettingsApp()
            } else {
                unblockSettingsApp()
            }

            dialog.dismiss()
            settingsDialog = null
            Log.d("Settings", "Inactivity Deduction: ${inactivityDeductionSwitch.isChecked}")
        }

        // Обработчик для кнопки "Отмена"
        dialogView.findViewById<Button>(R.id.dialog_cancel_button).setOnClickListener {
            val selectedClass = sharedPreferences.getInt("selected_class", 0)
            if (selectedClass == 0) {
                Toast.makeText(requireContext(), getString(R.string.select_class_toast), Toast.LENGTH_SHORT).show()
            } else {
                dialog.dismiss()
                settingsDialog = null
            }
        }

        dialog.setOnDismissListener {
            settingsDialog = null
        }

        dialog.show()
    }

    private fun blockSettingsApp() {
        val settingsPackageName = "com.android.settings"
        val blockedAppDao = AppDatabase.getInstance(requireContext()).blockedAppDao()
        CoroutineScope(Dispatchers.IO).launch {
            val blockedAppEntity = BlockedAppEntity(
                packageName = settingsPackageName,
                appName = "Настройки"
            )
            blockedAppDao.insertBlockedApp(blockedAppEntity)
        }
    }

    private fun unblockSettingsApp() {
        val settingsPackageName = "com.android.settings"
        val blockedAppDao = AppDatabase.getInstance(requireContext()).blockedAppDao()
        CoroutineScope(Dispatchers.IO).launch {
            blockedAppDao.deleteAppByPackageName(settingsPackageName)
        }
    }

    override fun onResume() {
        super.onResume()
        resetUsageStatsAtMidnight()
        binding.pieChart.animateY(1000, Easing.EaseInOutCubic)

        val sharedPreferences = requireContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val selectedClass = sharedPreferences.getInt("selected_class", 0)
        val hasShownInitialDialog = sharedPreferences.getBoolean("has_shown_initial_dialog", false)

        // Показываем диалог только если класс не выбран и диалог еще не показывался
        if (selectedClass == 0 && !hasShownInitialDialog) {
            showSettingsDialog()
        } else if (selectedClass != 0 && settingsDialog?.isShowing == true) {
            // Закрываем диалог, если класс выбран и диалог открыт
            settingsDialog?.dismiss()
            settingsDialog = null
        }
    }

    private fun loadAppUsageData() {
        binding.loadingAnimation.visibility = View.VISIBLE
        binding.loadingAnimation.playAnimation()
        binding.root.isClickable = false

        CoroutineScope(Dispatchers.IO).launch {
            appUsageMap = getAppUsageStats()

            withContext(Dispatchers.Main) {
                binding.loadingAnimation.visibility = View.GONE
                binding.loadingAnimation.cancelAnimation()
                binding.root.isClickable = true

                val nonZeroAppUsageList = appUsageMap.filter { it.value > 0 }
                if (nonZeroAppUsageList.isNotEmpty()) {
                    setupPieChart(nonZeroAppUsageList)
                    setupRecyclerView(nonZeroAppUsageList)
                } else {
                    binding.pieChart.clear()
                    binding.pieChart.setNoDataText(getString(R.string.statistics_is_not_available))
                    binding.pieChart.setNoDataTextColor(Color.WHITE)
                }
            }
        }
    }

    private fun getAppUsageStats(): Map<String, Long> {
        val usageStatsManager = requireContext().getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        val usageStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )

        val appUsageMap = mutableMapOf<String, Long>()
        for (usageStat in usageStats) {
            val packageName = usageStat.packageName
            if (!isSystemApp(packageName) && packageName != "com.annaorazov.screenguard") {
                val usageTime = usageStat.totalTimeInForeground
                if (usageTime > 0) {
                    appUsageMap[packageName] = (appUsageMap[packageName] ?: 0) + usageTime
                }
            }
        }

        return appUsageMap
    }

    private fun isSystemApp(packageName: String): Boolean {
        return try {
            val appInfo = requireContext().packageManager.getApplicationInfo(packageName, 0)
            appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun resetUsageStatsAtMidnight() {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)

        if (currentHour == 0 && currentMinute == 0) {
            resetAppUsageData()
            loadAppUsageData()
        }
    }

    private fun resetAppUsageData() {
        CoroutineScope(Dispatchers.IO).launch {
            val conditionalAppDao = AppDatabase.getInstance(requireContext()).conditionalAppDao()
            val conditionalApps = conditionalAppDao.getAll()
            conditionalApps.forEach { app ->
                app.usageTime = 0
                conditionalAppDao.updateConditionalApp(app)
            }
        }
    }

    private fun setupPieChart(appUsageList: Map<String, Long>) {
        val sortedAppUsageList = appUsageList.map { (packageName, usageTime) ->
            val appName = getAppNameFromPackage(packageName)
            Pair(appName, usageTime)
        }.sortedByDescending { it.second }

        val mostUsedAppTime = sortedAppUsageList.firstOrNull()?.second ?: 0L
        val filteredAppUsageList = sortedAppUsageList.filter { (_, usageTime) ->
            usageTime >= mostUsedAppTime / 6
        }

        val maxAppsToShow = 5
        val topAppUsageList = if (filteredAppUsageList.size > maxAppsToShow) {
            filteredAppUsageList.take(maxAppsToShow)
        } else {
            filteredAppUsageList
        }

        if (topAppUsageList.isEmpty()) {
            binding.pieChart.clear()
            binding.pieChart.setNoDataText(getString(R.string.error_statistics_chart))
            binding.pieChart.setNoDataTextColor(Color.WHITE)
            return
        }

        val entries = mutableListOf<IconPieEntry>()
        val colors = mutableListOf<Int>()
        for ((appName, usageTime) in topAppUsageList) {
            val formattedTime = formatUsageTime(usageTime)
            val packageName = getPackageNameFromAppName(appName)
            val icon = if (packageName != null) {
                getAppIconFromPackage(packageName)
            } else {
                null
            }

            entries.add(IconPieEntry(usageTime.toFloat(), "$appName $formattedTime", icon))
            val color = brightColors[colors.size % brightColors.size]
            colors.add(color)
        }

        val dataSet = PieDataSet(entries as List<PieEntry>, "App Usage")
        dataSet.colors = colors
        dataSet.valueTextSize = 14f
        dataSet.valueTextColor = Color.WHITE
        dataSet.sliceSpace = 2f
        dataSet.selectionShift = 5f
        dataSet.setDrawValues(false)

        val pieData = PieData(dataSet)
        pieData.setValueTextSize(12f)

        binding.pieChart.data = pieData
        binding.pieChart.description.isEnabled = false
        binding.pieChart.setEntryLabelColor(Color.WHITE)
        binding.pieChart.setEntryLabelTextSize(12f)
        binding.pieChart.setEntryLabelTypeface(Typeface.DEFAULT_BOLD)
        binding.pieChart.setHoleColor(Color.BLACK)
        binding.pieChart.setTransparentCircleAlpha(0)
        binding.pieChart.legend.isEnabled = false
        binding.pieChart.setDrawEntryLabels(true)
        binding.pieChart.setDrawRoundedSlices(true)

        binding.pieChart.renderer = IconPieChartRenderer(
            binding.pieChart,
            binding.pieChart.animator,
            binding.pieChart.viewPortHandler,
            requireContext()
        )

        binding.pieChart.invalidate()
    }

    private fun setupRecyclerView(appUsageList: Map<String, Long>) {
        val sortedAppUsageList = appUsageList.toList().sortedByDescending { it.second }
        val adapter = AppUsageAdapter(sortedAppUsageList, requireContext().packageManager)
        binding.appUsageRecyclerView.adapter = adapter
        binding.appUsageRecyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun getAppNameFromPackage(packageName: String): String {
        return try {
            val appInfo = requireContext().packageManager.getApplicationInfo(packageName, 0)
            requireContext().packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }

    private fun getPackageNameFromAppName(appName: String): String? {
        val packageManager = requireContext().packageManager
        val apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        for (app in apps) {
            if (app.loadLabel(packageManager).toString() == appName) {
                return app.packageName
            }
        }
        return null
    }

    private fun getAppIconFromPackage(packageName: String): Bitmap {
        return try {
            val appInfo = requireContext().packageManager.getApplicationInfo(packageName, 0)
            val drawable = requireContext().packageManager.getApplicationIcon(appInfo)
            val bitmap = Bitmap.createBitmap(
                drawable.intrinsicWidth,
                drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
            val canvas = android.graphics.Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        } catch (e: PackageManager.NameNotFoundException) {
            BitmapFactory.decodeResource(resources, R.drawable.ic_sort)
        }
    }

    private fun formatUsageTime(usageTimeInMillis: Long): String {
        val minutes = (usageTimeInMillis / (1000 * 60)) % 60
        val hours = (usageTimeInMillis / (1000 * 60 * 60)) % 24
        return if (hours > 0) {
            "${hours}" + getString(R.string.hour) + " ${minutes} " + getString(R.string.minute)
        } else {
            "${minutes}m"
        }
    }
}