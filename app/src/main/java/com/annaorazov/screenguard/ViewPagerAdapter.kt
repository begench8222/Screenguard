package com.annaorazov.screenguard

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.annaorazov.screenguard.ui.apps.AppsFragment
import com.annaorazov.screenguard.ui.conditionalapps.ConditionalAppsFragment
import com.annaorazov.screenguard.ui.blockedapps.BlockedAppsFragment
import com.annaorazov.screenguard.ui.statistics.StatisticsFragment

class ViewPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 4 // Update the count to include the new fragment

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            1 -> AppsFragment()
            0 -> StatisticsFragment()
            2 -> ConditionalAppsFragment()
            3 -> BlockedAppsFragment()
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
    }
}