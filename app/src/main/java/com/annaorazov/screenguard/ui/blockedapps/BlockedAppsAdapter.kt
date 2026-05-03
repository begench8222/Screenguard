package com.annaorazov.screenguard.ui.blockedapps

import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.annaorazov.screenguard.data.BlockedAppEntity
import com.annaorazov.screenguard.databinding.ItemBlockedAppBinding

class BlockedAppsAdapter(
    private val packageManager: PackageManager,
    private val onRemoveClick: (BlockedAppEntity) -> Unit // Callback for remove action
) : RecyclerView.Adapter<BlockedAppsAdapter.BlockedAppViewHolder>() {

    private var blockedApps: List<BlockedAppEntity> = emptyList()

    fun updateData(newBlockedApps: List<BlockedAppEntity>) {
        blockedApps = newBlockedApps
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BlockedAppViewHolder {
        val binding = ItemBlockedAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BlockedAppViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BlockedAppViewHolder, position: Int) {
        val blockedApp = blockedApps[position]
        val appName = blockedApp.appName
        val appIcon = try {
            val appInfo = packageManager.getApplicationInfo(blockedApp.packageName, 0)
            appInfo.loadIcon(packageManager)
        } catch (e: PackageManager.NameNotFoundException) {
            packageManager.defaultActivityIcon
        }

        holder.binding.appName.text = appName
        holder.binding.appIcon.setImageDrawable(appIcon)


        // Set click listener on the remove button
        holder.binding.appMenu.setOnClickListener {
            onRemoveClick(blockedApp) // Trigger the callback
        }
    }

    override fun getItemCount(): Int = blockedApps.size


    inner class BlockedAppViewHolder(val binding: ItemBlockedAppBinding) : RecyclerView.ViewHolder(binding.root)
}