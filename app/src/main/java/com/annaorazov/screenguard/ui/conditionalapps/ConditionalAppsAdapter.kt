package com.annaorazov.screenguard.ui.conditionalapps

import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.annaorazov.screenguard.databinding.ItemConditionalAppBinding
import com.annaorazov.screenguard.data.ConditionalAppEntity

class ConditionalAppsAdapter(
    private val packageManager: PackageManager,
    private val onRemoveClick: (ConditionalAppEntity) -> Unit, // Callback for remove action
    private val showRemoveButton: Boolean = true // Added parameter for visibility
) : RecyclerView.Adapter<ConditionalAppsAdapter.ConditionalAppViewHolder>() {

    private var conditionalApps: List<ConditionalAppEntity> = emptyList()

    fun updateData(newConditionalApps: List<ConditionalAppEntity>) {
        conditionalApps = newConditionalApps
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConditionalAppViewHolder {
        val binding = ItemConditionalAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ConditionalAppViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ConditionalAppViewHolder, position: Int) {
        val conditionalApp = conditionalApps[position]
        val appName = conditionalApp.appName
        val appIcon = try {
            val appInfo = packageManager.getApplicationInfo(conditionalApp.packageName, 0)
            appInfo.loadIcon(packageManager)
        } catch (e: PackageManager.NameNotFoundException) {
            packageManager.defaultActivityIcon
        }

        holder.binding.appName.text = appName
        holder.binding.appIcon.setImageDrawable(appIcon)

        // Set visibility of remove button based on showRemoveButton parameter
        if (showRemoveButton) {
            holder.binding.icRemove.visibility = View.VISIBLE
            // Set click listener on the remove button
            holder.binding.icRemove.setOnClickListener {
                onRemoveClick(conditionalApp) // Trigger the callback
            }
        } else {
            holder.binding.icRemove.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = conditionalApps.size

    inner class ConditionalAppViewHolder(val binding: ItemConditionalAppBinding) : RecyclerView.ViewHolder(binding.root)
}