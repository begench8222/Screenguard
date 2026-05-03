package com.annaorazov.screenguard

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.airbnb.lottie.LottieAnimationView
import com.github.appintro.SlidePolicy

class PermissionSlideFragment : Fragment(R.layout.fragment_permission), SlidePolicy {

    companion object {
        private const val ARG_TYPE = "arg_type"
        const val TYPE_USAGE = "usage"
        const val TYPE_ACCESSIBILITY = "accessibility"
        const val TYPE_DEVICE_ADMIN = "device_admin"
        const val TYPE_NOTIFICATIONS = "notifications"
        const val TYPE_OVERLAY = "overlay"

        fun newInstance(type: String): PermissionSlideFragment {
            val f = PermissionSlideFragment()
            f.arguments = bundleOf(ARG_TYPE to type)
            return f
        }
    }

    private val type: String by lazy { arguments?.getString(ARG_TYPE) ?: "" }

    override val isPolicyRespected: Boolean
        get() = when (type) {
            TYPE_USAGE -> (activity as? IntroActivity)?.hasUsageStatsPermission() == true
            TYPE_ACCESSIBILITY -> (activity as? IntroActivity)?.isAccessibilityServiceEnabled() == true
            TYPE_DEVICE_ADMIN -> (activity as? IntroActivity)?.isDeviceAdminEnabled() == true
            TYPE_NOTIFICATIONS -> (activity as? IntroActivity)?.areNotificationsGranted() == true
            TYPE_OVERLAY -> android.provider.Settings.canDrawOverlays(requireContext())
            else -> true
        }

    override fun onUserIllegallyRequestedNextPage() {
        Toast.makeText(requireContext(),
            getString(R.string.permit_all_permissions), Toast.LENGTH_SHORT).show()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val title = view.findViewById<TextView>(R.id.slideTitle)
        val desc = view.findViewById<TextView>(R.id.slideDesc)
        val btn = view.findViewById<Button>(R.id.btnGrant)
        val lottieAnimation = view.findViewById<LottieAnimationView>(R.id.lottieAnimation)

        when (type) {
            TYPE_USAGE -> {
                title.text = getString(R.string.title_statistics_perm)
                desc.text = getString(R.string.statistics_perm_descrp)
                btn.text = getString(R.string.statistics_perm_button)
                btn.setOnClickListener { (activity as? IntroActivity)?.requestUsageStatsPermission() }
                lottieAnimation.setAnimation(R.raw.statistics)
            }
            TYPE_ACCESSIBILITY -> {
                title.text = getString(R.string.accessibility_service_perm_title)
                desc.text = getString(R.string.accessibility_service_descrp)
                btn.text = getString(R.string.accessibility_service_button)
                btn.setOnClickListener { (activity as? IntroActivity)?.requestAccessibilityPermission() }
                lottieAnimation.setAnimation(R.raw.accessibility)
            }
            TYPE_DEVICE_ADMIN -> {
                title.text = getString(R.string.device_admin_title)
                desc.text = getString(R.string.device_admin_perm_descrp)
                btn.text = getString(R.string.device_admin_perm_button)
                btn.setOnClickListener { (activity as? IntroActivity)?.requestDeviceAdmin() }
                lottieAnimation.setAnimation(R.raw.deviceadmin)
            }
            TYPE_NOTIFICATIONS -> {
                title.text = getString(R.string.notification)
                desc.text = getString(R.string.notification_perm_descrp)
                btn.text = getString(R.string.notification_perm_button)
                btn.setOnClickListener { (activity as? IntroActivity)?.requestNotificationPermission() }
                lottieAnimation.setAnimation(R.raw.notification)
            }
            TYPE_OVERLAY -> {
                title.text = getString(R.string.overlay_perm_title)
                desc.text = getString(R.string.overlay_perm_descrp)
                btn.text = getString(R.string.overlay_perm_button)
                btn.setOnClickListener { (activity as? IntroActivity)?.requestOverlayPermission() }
                lottieAnimation.setAnimation(R.raw.appearontop)
            }
        }

        // Start the animation
        lottieAnimation.playAnimation()
    }
}