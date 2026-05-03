package com.annaorazov.screenguard

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.airbnb.lottie.LottieAnimationView

class ImageSlideFragment : Fragment(R.layout.fragment_image_slide) {

    companion object {
        private const val ARG_TITLE = "arg_title"
        private const val ARG_DESC = "arg_desc"
        private const val ARG_IMAGE = "arg_image"
        private const val ARG_BG_COLOR = "arg_bg_color"
        private const val ARG_LOTTIE_RES = "arg_lottie_res"

        fun newInstance(title: String, desc: String, imageRes: Int, bgColorRes: Int): ImageSlideFragment {
            return newInstance(title, desc, imageRes, bgColorRes, 0)
        }

        fun newInstance(title: String, desc: String, imageRes: Int, bgColorRes: Int, lottieRes: Int): ImageSlideFragment {
            val f = ImageSlideFragment()
            f.arguments = bundleOf(
                ARG_TITLE to title,
                ARG_DESC to desc,
                ARG_IMAGE to imageRes,
                ARG_BG_COLOR to bgColorRes,
                ARG_LOTTIE_RES to lottieRes
            )
            return f
        }
    }
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
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val title = arguments?.getString(ARG_TITLE) ?: ""
        val desc = arguments?.getString(ARG_DESC) ?: ""
        val imageRes = arguments?.getInt(ARG_IMAGE) ?: 0
        val bgColorRes = arguments?.getInt(ARG_BG_COLOR) ?: 0
        val lottieRes = arguments?.getInt(ARG_LOTTIE_RES) ?: 0

        view.findViewById<TextView>(R.id.slideTitle).text = title
        view.findViewById<TextView>(R.id.slideDesc).text = desc

        val imageView = view.findViewById<ImageView>(R.id.slideImage)
        val lottieView = view.findViewById<LottieAnimationView>(R.id.lottieAnimation)

        if (lottieRes != 0) {
            imageView.visibility = View.GONE
            lottieView.visibility = View.VISIBLE
            lottieView.setAnimation(lottieRes)
        } else {
            imageView.visibility = View.VISIBLE
            lottieView.visibility = View.GONE
            imageView.setImageResource(imageRes)
        }

        view.setBackgroundResource(bgColorRes)

        // --- Language switch button ---
        val btnLang = view.findViewById<ImageButton>(R.id.btnSwitchLanguage)
        updateButtonIcon(btnLang)

        btnLang.setOnClickListener {
            val currentLang = SwitchLanguageHelper.getLanguage(requireContext())
            val newLang = when (currentLang) {
                "tk" -> "en"
                "en" -> "ru"
                else -> "tk"
            }
            SwitchLanguageHelper.saveLanguage(requireContext(), newLang)

            val intent = requireActivity().intent
            requireActivity().finish()
            requireActivity().overridePendingTransition(0, 0) // no animation
            startActivity(intent)
        }
    }
}