package com.annaorazov.screenguard.ui.apps

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.annaorazov.screenguard.databinding.FragmentAppsBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.annaorazov.screenguard.data.AppDatabase
import com.annaorazov.screenguard.data.ConditionalAppEntity
import com.annaorazov.screenguard.data.BlockedAppEntity
import com.annaorazov.screenguard.ui.conditionalapps.ConditionalAppsFragment
import com.annaorazov.screenguard.ui.blockedapps.BlockedAppsFragment
import kotlinx.coroutines.withContext

class AppsFragment : Fragment() {

    private lateinit var binding: FragmentAppsBinding
    private lateinit var appsAdapter: AppListAdapter
    private var appsList: List<ApplicationInfo> = emptyList()
    private var filteredAppsList: List<ApplicationInfo> = emptyList()
    private var recyclerViewState: Parcelable? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAppsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load apps data
        loadAppsData()
    }
    override fun onResume() {
        super.onResume()
        recyclerViewState = binding.recyclerView.layoutManager?.onSaveInstanceState()
        loadAppsData()
    }
    fun loadAppsData() {
        val pm = requireContext().packageManager
        val allApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        val blockedAppDao = AppDatabase.getInstance(requireContext()).blockedAppDao()
        val conditionalAppDao = AppDatabase.getInstance(requireContext()).conditionalAppDao()

        CoroutineScope(Dispatchers.IO).launch {
            val blockedApps = blockedAppDao.getAll()
            val conditionalApps = conditionalAppDao.getAll()
            val blockedPackageName = blockedApps.map { it.packageName }
            val conditionalPackageNames = conditionalApps.map { it.packageName }

            appsList = allApps.filter { app ->
                (app.flags and ApplicationInfo.FLAG_SYSTEM) == 0 &&
                        !blockedPackageName.contains(app.packageName) &&
                        !conditionalPackageNames.contains(app.packageName) &&
                        app.packageName != requireContext().packageName
            }

            withContext(Dispatchers.Main) {
                filteredAppsList = appsList

                if (!::appsAdapter.isInitialized) {
                    appsAdapter = AppListAdapter(
                        filteredAppsList,
                        pm,
                        onMoreVertClick = { app -> moveAppToBlockedList(app) },
                        onCondClick = { app -> moveAppToConditionalList(app) }
                    )
                    binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
                    binding.recyclerView.adapter = appsAdapter
                } else {
                    appsAdapter.updateData(filteredAppsList)
                }

                // Сохраняем состояние прокрутки перед обновлением
                recyclerViewState = binding.recyclerView.layoutManager?.onSaveInstanceState()

                // Восстановление состояния прокрутки
                recyclerViewState?.let {
                    binding.recyclerView.layoutManager?.onRestoreInstanceState(it)
                }
            }
        }
    }

    private fun moveAppToBlockedList(app: ApplicationInfo) {
        val blockedAppEntity = BlockedAppEntity(
            packageName = app.packageName,
            appName = app.loadLabel(requireContext().packageManager).toString(),
        )

        // Insert into the database
        val blockedAppDao = AppDatabase.getInstance(requireContext()).blockedAppDao()
        CoroutineScope(Dispatchers.IO).launch {
            blockedAppDao.insertBlockedApp(blockedAppEntity)
            withContext(Dispatchers.Main) {
                loadAppsData() // Refresh the apps list
                (parentFragmentManager.findFragmentByTag("f2") as? BlockedAppsFragment)?.updateBlockedAppsList()
            }
        }
    }

    private fun moveAppToConditionalList(app: ApplicationInfo) {
        val conditionalAppEntity = ConditionalAppEntity(
            packageName = app.packageName,
            appName = app.loadLabel(requireContext().packageManager).toString(),
            condition = "Time Limit",
            limitTime = 0
        )

        Log.d("ConditionalAppEntity", "Inserting: $conditionalAppEntity")

        val conditionalAppDao = AppDatabase.getInstance(requireContext()).conditionalAppDao()
        CoroutineScope(Dispatchers.IO).launch {
            conditionalAppDao.insertConditionalApp(conditionalAppEntity)
            val insertedApp = conditionalAppDao.getAppByPackageName(app.packageName)
            Log.d("ConditionalAppEntity", "Inserted app: $insertedApp")

            withContext(Dispatchers.Main) {
                loadAppsData() // Refresh the apps list in AppsFragment

                // Notify ConditionalAppsFragment to refresh its list
                val conditionalAppsFragment = parentFragmentManager.findFragmentByTag("f3") as? ConditionalAppsFragment
                conditionalAppsFragment?.updateConditionalAppsList()
            }
        }
    }

    fun filterApps(query: String?) {
        // Фильтруем список приложений
        filteredAppsList = if (query.isNullOrEmpty()) {
            appsList
        } else {
            appsList.filter { app ->
                app.loadLabel(requireContext().packageManager).toString()
                    .contains(query, ignoreCase = true)
            }
        }

        // Обновляем данные в адаптере
        appsAdapter.updateData(filteredAppsList)
    }
}