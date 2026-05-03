package com.annaorazov.screenguard.learn_game

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.annaorazov.screenguard.R
import com.annaorazov.screenguard.SwitchLanguageHelper
import com.annaorazov.screenguard.databinding.ActivityLearnGameBinding
import com.annaorazov.screenguard.utils.TimeUtils

class LearnGameActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLearnGameBinding

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(SwitchLanguageHelper.applyLanguage(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLearnGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val selectedClass = sharedPreferences.getInt("selected_class", 1)

        // Parse subjects with keys
        val subjects = parseSubjects(getSubjectsArrayResId(selectedClass))

        val adapter = SubjectAdapter(subjects, this, selectedClass)
        binding.subjectsRecyclerView.adapter = adapter
        binding.subjectsRecyclerView.layoutManager = LinearLayoutManager(this)

        binding.button.setOnClickListener { finish() }
    }

    private fun parseSubjects(arrayResId: Int): List<Subject> {
        val subjectStrings = resources.getStringArray(arrayResId)
        return subjectStrings.map { subjectString ->
            val parts = subjectString.split("|")
            if (parts.size == 2) {
                Subject(parts[0], parts[1])
            } else {
                // Fallback for old format or malformed entries
                val key = subjectString.toLowerCase().replace(" ", "_")
                Subject(key, subjectString)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        TimeUtils.setupTimeDisplay(
            this,
            binding.timeDisplay.remainingTimeText,
        )
    }

    private fun getSubjectsArrayResId(selectedClass: Int): Int {
        return when (selectedClass) {
            1 -> R.array.class_1_subjects
            2 -> R.array.class_2_subjects
            3 -> R.array.class_3_subjects
            4 -> R.array.class_4_subjects
            5 -> R.array.class_5_subjects
            6 -> R.array.class_6_subjects
            7 -> R.array.class_7_subjects
            8 -> R.array.class_8_subjects
            9 -> R.array.class_9_subjects
            10 -> R.array.class_10_subjects
            11 -> R.array.class_11_subjects
            12 -> R.array.class_12_subjects
            else -> R.array.class_1_subjects
        }
    }
}