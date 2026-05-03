package com.annaorazov.screenguard.ui.blockedapps

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.annaorazov.screenguard.databinding.FragmentBlockedappsBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.annaorazov.screenguard.data.AppDatabase
import com.annaorazov.screenguard.data.BlockedAppEntity
import com.annaorazov.screenguard.ui.apps.AppsFragment

class BlockedAppsFragment : Fragment() {

    private var _binding: FragmentBlockedappsBinding? = null
    private val binding get() = _binding!!
    private lateinit var blockedAppsAdapter: BlockedAppsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBlockedappsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!::blockedAppsAdapter.isInitialized) {
            blockedAppsAdapter = BlockedAppsAdapter(requireContext().packageManager) { blockedApp ->
                removeAppFromBlockedList(blockedApp) // Обработка действия удаления
            }
            binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
            binding.recyclerView.adapter = blockedAppsAdapter
        }

        updateBlockedAppsList()
    }

    override fun onResume() {
        super.onResume()
        updateBlockedAppsList() // Refresh the list when the fragment becomes visible
    }

    private fun removeAppFromBlockedList(blockedApp: BlockedAppEntity) {
        val blockedAppDao = AppDatabase.getInstance(requireContext()).blockedAppDao()
        CoroutineScope(Dispatchers.IO).launch {
            blockedAppDao.deleteAppByPackageName(blockedApp.packageName)
            withContext(Dispatchers.Main) {
                updateBlockedAppsList() // Refresh the list after removal
                (parentFragmentManager.findFragmentByTag("f0") as? AppsFragment)?.loadAppsData()
            }
        }
    }
    fun updateBlockedAppsList() {
        val blockedAppDao = AppDatabase.getInstance(requireContext()).blockedAppDao()
        CoroutineScope(Dispatchers.IO).launch {
            val blockedApps = blockedAppDao.getAll()
                .filter { it.packageName != "com.android.settings" } // Исключаем приложение "Настройки"
            withContext(Dispatchers.Main) {
                blockedAppsAdapter.updateData(blockedApps)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}