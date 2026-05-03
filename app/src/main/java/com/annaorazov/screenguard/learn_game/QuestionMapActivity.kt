package com.annaorazov.screenguard.learn_game

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.annaorazov.screenguard.databinding.ActivityQuestionMapBinding
import com.annaorazov.screenguard.utils.TimeUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import android.graphics.PointF
import android.util.Log
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.annaorazov.screenguard.R
import com.annaorazov.screenguard.SwitchLanguageHelper
import com.google.gson.JsonObject
import java.io.IOException

class QuestionMapActivity : AppCompatActivity() {
    private lateinit var binding: ActivityQuestionMapBinding
    private lateinit var subject: String
    private lateinit var questions: List<Question>
    private var classLevel: Int = 1
    private var correctProgress: MutableSet<Int> = mutableSetOf()
    private var incorrectProgress: MutableSet<Int> = mutableSetOf()
    private var isPremiumUnlocked: Boolean = false
    private lateinit var validPromoCodes: List<String>
    private val PREMIUM_QUESTION_LIMIT = 5
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(SwitchLanguageHelper.applyLanguage(newBase))
    }
    private var subjectKey: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuestionMapBinding.inflate(layoutInflater)
        setContentView(binding.root)


        subject = intent.getStringExtra("subject") ?: run {
            Toast.makeText(this, "Subject not specified", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        subjectKey = intent.getStringExtra("subjectKey") ?: subject.toLowerCase().replace(" ", "_")
        classLevel = intent.getIntExtra("classLevel", 1)
        binding.subjectTitle.text = subject

        loadProgress()
        loadPromoCodes()
        checkPremiumStatus()
        loadQuestions(classLevel)

        // Setup crown icon click listener
        binding.crownIcon.setOnClickListener {
            showPromoCodeDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        TimeUtils.setupTimeDisplay(
            this,
            binding.timeDisplay.remainingTimeText,
        )
    }

    private fun loadPromoCodes() {
        try {
            val jsonString = assets.open("promocodes.json").bufferedReader().use { it.readText() }
            val jsonObject = Gson().fromJson(jsonString, JsonObject::class.java)
            val promoCodesArray = jsonObject.getAsJsonArray("promocodes")
            validPromoCodes = Gson().fromJson(promoCodesArray, object : TypeToken<List<String>>() {}.type)
        } catch (e: IOException) {
            Log.e("QuestionMap", "Error loading promo codes: ${e.message}")
            Toast.makeText(this, "Error loading promo codes", Toast.LENGTH_SHORT).show()
            validPromoCodes = emptyList()
        }
    }

    private fun checkPremiumStatus() {
        val sharedPreferences = getSharedPreferences("QuestionProgress", MODE_PRIVATE)
        isPremiumUnlocked = sharedPreferences.getBoolean("premium_${subject}_$classLevel", false)
    }

    private fun savePremiumStatus() {
        val sharedPreferences = getSharedPreferences("QuestionProgress", MODE_PRIVATE)
        sharedPreferences.edit()
            .putBoolean("premium_${subject}_$classLevel", isPremiumUnlocked)
            .apply()
    }

    private fun showPromoCodeDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        val input = EditText(this).apply {
            hint = context.getString(R.string.promokod_enter)
        }

        val helperText = TextView(this).apply {
            text = context.getString(R.string.promocod_desc)
            setTextColor(Color.GRAY)
            textSize = 14f
            setPadding(0, 10, 0, 0)
        }

        layout.addView(input)
        layout.addView(helperText)

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.promocod_title))
            .setView(layout)
            .setPositiveButton("Giriz") { _, _ ->
                val code = input.text.toString().trim()
                if (validPromoCodes.contains(code)) {
                    isPremiumUnlocked = true
                    savePremiumStatus()
                    setupQuestionMap()
                    Toast.makeText(this, getString(R.string.premium_success), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, getString(R.string.promokod_failed), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Yza", null)
            .show()
    }


    private fun loadQuestions(classLevel: Int) {
        try {
            val safeSubject = subject.replace(" ", "_")
            val fileName = "class_$classLevel/${safeSubject}_$classLevel.json"
            Log.d("QuestionMap", "Loading file: $fileName")
            val jsonString = assets.open(fileName).bufferedReader().use { it.readText() }
            val type = object : TypeToken<List<Question>>() {}.type
            questions = Gson().fromJson(jsonString, type)
            if (questions.isEmpty()) {
                Log.d("QuestionMap", "No questions loaded for $subject")
                Toast.makeText(this, "No questions loaded for $subject", Toast.LENGTH_SHORT).show()
                finish()
                return
            }
            Log.d("QuestionMap", "Questions loaded: ${questions.size}")
            setupQuestionMap()
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("QuestionMap", "Error loading questions: ${e.message}")
            Toast.makeText(this, "Error loading questions: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupQuestionMap() {
        val points = questions.mapIndexed { index, _ ->
            PointF(index.toFloat(), 0f)
        }

        binding.questionMapView.setPoints(points, correctProgress, incorrectProgress, isPremiumUnlocked)
        binding.questionMapView.setOnPointClickListener { questionIndex ->
            if (questionIndex >= PREMIUM_QUESTION_LIMIT && !isPremiumUnlocked) {
                Toast.makeText(this, getString(R.string.get_premium), Toast.LENGTH_SHORT).show()
                showPromoCodeDialog()
            } else {
                startQuestionActivity(questionIndex)
            }
        }
    }

    private fun startQuestionActivity(questionIndex: Int) {
        val questionJson = Gson().toJson(questions[questionIndex])
        val intent = Intent(this, QuestionActivity::class.java).apply {
            putExtra("question", questionJson)
            putExtra("totalQuestions", questions.size)
            putExtra("currentQuestion", questionIndex + 1)
            putExtra("classLevel", classLevel)
            putExtra("subject", subject)

            val isAnswered = correctProgress.contains(questionIndex) || incorrectProgress.contains(questionIndex)
            putExtra("isAlreadyAnswered", isAnswered)
            putExtra("wasCorrect", correctProgress.contains(questionIndex))

            if (isAnswered && incorrectProgress.contains(questionIndex)) {
                putExtra("selectedAnswer", getSelectedAnswer(questionIndex))
            }

            if (questionIndex + 1 < questions.size) {
                putExtra("nextQuestion_${questionIndex + 1}", Gson().toJson(questions[questionIndex + 1]))
            }
        }
        startActivityForResult(intent, REQUEST_CODE_QUESTION)
    }

    private fun getSelectedAnswer(questionIndex: Int): Int {
        return -1 // Placeholder for actual logic
    }

    private fun loadProgress() {
        val sharedPreferences = getSharedPreferences("QuestionProgress", MODE_PRIVATE)
        val progressKey = "progress_${subject}_$classLevel"
        val incorrectKey = "incorrect_${subject}_$classLevel"

        correctProgress = sharedPreferences.getStringSet(progressKey, emptySet())?.map { it.toInt() }?.toMutableSet() ?: mutableSetOf()
        incorrectProgress = sharedPreferences.getStringSet(incorrectKey, emptySet())?.map { it.toInt() }?.toMutableSet() ?: mutableSetOf()
    }

    private fun saveProgress() {
        val sharedPreferences = getSharedPreferences("QuestionProgress", MODE_PRIVATE)
        val progressKey = "progress_${subject}_$classLevel"
        val incorrectKey = "incorrect_${subject}_$classLevel"

        sharedPreferences.edit()
            .putStringSet(progressKey, correctProgress.map { it.toString() }.toSet())
            .putStringSet(incorrectKey, incorrectProgress.map { it.toString() }.toSet())
            .apply()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_QUESTION && resultCode == RESULT_OK) {
            data?.let {
                val questionIndex = it.getIntExtra("questionIndex", -1)
                val isCorrect = it.getBooleanExtra("isCorrect", false)
                val selectedAnswer = it.getIntExtra("selectedAnswer", -1)

                if (questionIndex >= 0) {
                    if (isCorrect) {
                        correctProgress.add(questionIndex)
                        incorrectProgress.remove(questionIndex)
                    } else {
                        incorrectProgress.add(questionIndex)
                        correctProgress.remove(questionIndex)
                    }
                    saveProgress()
                    setupQuestionMap()
                }
            }
        }
    }

    companion object {
        const val REQUEST_CODE_QUESTION = 100
    }
}