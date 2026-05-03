package com.annaorazov.screenguard.ui.apps

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.annaorazov.screenguard.databinding.ItemAppBinding

class AppListAdapter(
    private var appsList: List<ApplicationInfo>,
    private val packageManager: PackageManager,
    private val onMoreVertClick: (ApplicationInfo) -> Unit, // Callback for ic_more_vert
    private val onCondClick: (ApplicationInfo) -> Unit // Callback for ic_cond
) : RecyclerView.Adapter<AppListAdapter.AppViewHolder>() {

    fun updateData(newAppsList: List<ApplicationInfo>) {
        appsList = newAppsList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val binding = ItemAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AppViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = appsList[position]
        val appName = app.loadLabel(packageManager).toString()
        val appIcon = app.loadIcon(packageManager)

        holder.binding.appName.text = appName
        holder.binding.appIcon.setImageDrawable(appIcon)

        holder.binding.appMenu.setOnClickListener {
            onMoreVertClick(app)
        }

        // Set click listener for ic_cond (move to ConditionalApps with time picker)
        holder.binding.icCond.setOnClickListener {
            onCondClick(app)
        }
    }

    override fun getItemCount(): Int = appsList.size

    inner class AppViewHolder(val binding: ItemAppBinding) : RecyclerView.ViewHolder(binding.root)
}