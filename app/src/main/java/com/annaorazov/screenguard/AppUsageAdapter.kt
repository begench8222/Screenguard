package com.annaorazov.screenguard

import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.annaorazov.screenguard.databinding.ItemAppUsageBinding
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry

class AppUsageAdapter(
    private val appUsageList: List<Pair<String, Long>>, // Pair of packageName and usageTime
    private val packageManager: PackageManager
) : RecyclerView.Adapter<AppUsageAdapter.AppUsageViewHolder>() {

    private val maxUsageTime: Long = appUsageList.maxOfOrNull { it.second } ?: 1L

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppUsageViewHolder {
        val binding = ItemAppUsageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AppUsageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppUsageViewHolder, position: Int) {
        val (packageName, usageTime) = appUsageList[position]
        val appName = getAppNameFromPackage(packageName)
        val appIcon = getAppIconFromPackage(packageName)

        holder.binding.appName.text = appName
        holder.binding.appIcon.setImageDrawable(appIcon)
        holder.binding.usageTime.text = formatUsageTime(usageTime)

        // Set progress based on the most used app
        val progress = (usageTime.toFloat() / maxUsageTime * 100).toInt()
        holder.binding.usageProgress.progress = progress
    }

    override fun getItemCount(): Int = appUsageList.size

    private fun getAppNameFromPackage(packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }

    private fun getAppIconFromPackage(packageName: String): Drawable {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationIcon(appInfo)
        } catch (e: PackageManager.NameNotFoundException) {
            packageManager.defaultActivityIcon
        }
    }

    private fun formatUsageTime(usageTimeInMillis: Long): String {
        val minutes = (usageTimeInMillis / (1000 * 60)) % 60
        val hours = (usageTimeInMillis / (1000 * 60 * 60)) % 24

        return if (hours > 0) {
            "${hours}:${minutes}min"
        } else {
            "${minutes}min"
        }
    }

    inner class AppUsageViewHolder(val binding: ItemAppUsageBinding) :
        RecyclerView.ViewHolder(binding.root)
}