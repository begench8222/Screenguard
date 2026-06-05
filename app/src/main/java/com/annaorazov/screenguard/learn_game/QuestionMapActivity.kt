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
import kotlinx.coroutines.*
import java.io.IOException

class QuestionMapActivity : AppCompatActivity() {
    private lateinit var binding: ActivityQuestionMapBinding
    private lateinit var subject: String
    private lateinit var questions: List<Question>
    private var classLevel: Int = 1
    private val selectedAnswers = mutableMapOf<Int, Int>()
    private var correctProgress: MutableSet<Int> = mutableSetOf()
    private var incorrectProgress: MutableSet<Int> = mutableSetOf()
    private var isPremiumUnlocked: Boolean = false
    private lateinit var validPromoCodes: List<String>
    private val PREMIUM_QUESTION_LIMIT = 5
    private lateinit var database: QuestionProgressDatabase
    private val mainScope = CoroutineScope(Dispatchers.Main)

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(SwitchLanguageHelper.applyLanguage(newBase))
    }
    private var subjectKey: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuestionMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = QuestionProgressDatabase.getDatabase(this)

        subject = intent.getStringExtra("subject") ?: run {
            Toast.makeText(this, "Subject not specified", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        subjectKey = intent.getStringExtra("subjectKey") ?: subject.lowercase().replace(" ", "_")
        classLevel = intent.getIntExtra("classLevel", 1)
        binding.subjectTitle.text = subject

        loadPromoCodes()
        checkPremiumStatus()
        loadQuestions(classLevel)

        binding.crownIcon.setOnClickListener {
            showPromoCodeDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        TimeUtils.setupTimeDisplay(this, binding.timeDisplay.remainingTimeText)
        // Перезагружаем прогресс из БД при возврате
        CoroutineScope(Dispatchers.Main).launch {
            loadProgressFromDatabase()
            if (::questions.isInitialized && questions.isNotEmpty()) {
                updateMapDisplay()
            }
        }
    }

    private suspend fun loadProgressFromDatabase() {
        withContext(Dispatchers.IO) {
            try {
                val progressList = database.questionProgressDao().getProgressForSubject(subject, classLevel)
                correctProgress.clear()
                incorrectProgress.clear()
                selectedAnswers.clear()

                for (progress in progressList) {
                    selectedAnswers[progress.questionIndex] = progress.selectedAnswer
                    if (progress.isCorrect) {
                        correctProgress.add(progress.questionIndex)
                    } else {
                        incorrectProgress.add(progress.questionIndex)
                    }
                }

                Log.d("QuestionMap", "Loaded from DB - Correct: ${correctProgress.size}, Incorrect: ${incorrectProgress.size}")
            } catch (e: Exception) {
                Log.e("QuestionMap", "Error loading from DB: ${e.message}")
            }
        }
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
                    updateMapDisplay()
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

            mainScope.launch {
                loadProgressFromDatabase()
                setupQuestionMap()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("QuestionMap", "Error loading questions: ${e.message}")
            Toast.makeText(this, "Error loading questions: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupQuestionMap() {
        if (!::questions.isInitialized || questions.isEmpty()) return

        updateMapDisplay()

        binding.questionMapView.setOnPointClickListener { questionIndex ->
            Log.d("QuestionMap", "Question clicked: $questionIndex")
            if (questionIndex >= PREMIUM_QUESTION_LIMIT && !isPremiumUnlocked) {
                Toast.makeText(this, getString(R.string.get_premium), Toast.LENGTH_SHORT).show()
                showPromoCodeDialog()
            } else {
                val isAlreadyAnswered = correctProgress.contains(questionIndex) || incorrectProgress.contains(questionIndex)
                Log.d("QuestionMap", "Question $questionIndex - Already answered: $isAlreadyAnswered")
                if (!isAlreadyAnswered) {
                    startQuestionActivity(questionIndex)
                }
            }
        }
    }

    private fun updateMapDisplay() {
        if (!::questions.isInitialized) return
        val points = questions.mapIndexed { index, _ ->
            PointF(index.toFloat(), 0f)
        }
        Log.d("QuestionMap", "Updating map - Correct: ${correctProgress.size}, Incorrect: ${incorrectProgress.size}")
        Log.d("QuestionMap", "Correct indices: $correctProgress")
        Log.d("QuestionMap", "Incorrect indices: $incorrectProgress")
        binding.questionMapView.setPoints(points, correctProgress, incorrectProgress, isPremiumUnlocked)
    }

    private fun startQuestionActivity(questionIndex: Int) {
        Log.d("QuestionMap", "startQuestionActivity - Index: $questionIndex")
        Log.d("QuestionMap", "Current correctProgress: $correctProgress")
        Log.d("QuestionMap", "Current incorrectProgress: $incorrectProgress")

        val isAnswered = correctProgress.contains(questionIndex) || incorrectProgress.contains(questionIndex)
        Log.d("QuestionMap", "isAnswered: $isAnswered")

        if (isAnswered) {
            Toast.makeText(this, "Вы уже ответили на этот вопрос", Toast.LENGTH_SHORT).show()
            return
        }

        val questionJson = Gson().toJson(questions[questionIndex])
        val intent = Intent(this, QuestionActivity::class.java).apply {
            putExtra("question", questionJson)
            putExtra("totalQuestions", questions.size)
            putExtra("currentQuestion", questionIndex + 1)
            putExtra("classLevel", classLevel)
            putExtra("subject", subject)
            putExtra("questionIndex", questionIndex)
            putExtra("isAlreadyAnswered", false) // Всегда false при новом вопросе
        }
        startActivityForResult(intent, REQUEST_CODE_QUESTION)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d("QuestionMap", "onActivityResult called - requestCode: $requestCode, resultCode: $resultCode")

        if (requestCode == REQUEST_CODE_QUESTION && resultCode == RESULT_OK) {
            data?.let {
                val questionIndex = it.getIntExtra("questionIndex", -1)
                val isCorrect = it.getBooleanExtra("isCorrect", false)
                val selectedAnswer = it.getIntExtra("selectedAnswer", -1)

                Log.d("QuestionMap", "Activity result - Index: $questionIndex, Correct: $isCorrect, Answer: $selectedAnswer")

                if (questionIndex >= 0 && selectedAnswer != -1) {
                    // Обновляем локальные данные
                    selectedAnswers[questionIndex] = selectedAnswer
                    if (isCorrect) {
                        correctProgress.add(questionIndex)
                        incorrectProgress.remove(questionIndex)
                        Log.d("QuestionMap", "Added to correct: $questionIndex")
                    } else {
                        incorrectProgress.add(questionIndex)
                        correctProgress.remove(questionIndex)
                        Log.d("QuestionMap", "Added to incorrect: $questionIndex")
                    }

                    Log.d("QuestionMap", "After update - Correct: $correctProgress, Incorrect: $incorrectProgress")

                    // Обновляем UI
                    runOnUiThread {
                        updateMapDisplay()
                    }

                    // Сохраняем в БД
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val entity = QuestionProgressEntity(
                                id = "${subject}_${classLevel}_$questionIndex",
                                subject = subject,
                                classLevel = classLevel,
                                questionIndex = questionIndex,
                                isCorrect = isCorrect,
                                selectedAnswer = selectedAnswer
                            )
                            database.questionProgressDao().insert(entity)
                            Log.d("QuestionMap", "Successfully saved to DB - Index: $questionIndex")

                            // Проверяем, что сохранилось
                            val check = database.questionProgressDao().getProgressForSubject(subject, classLevel)
                            Log.d("QuestionMap", "DB now has ${check.size} records")
                        } catch (e: Exception) {
                            Log.e("QuestionMap", "Error saving to DB: ${e.message}", e)
                        }
                    }
                } else {
                    Log.d("QuestionMap", "Invalid data - questionIndex: $questionIndex, selectedAnswer: $selectedAnswer")
                }
            }
        } else {
            Log.d("QuestionMap", "Result not handled - requestCode: $requestCode, resultCode: $resultCode")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mainScope.cancel()
    }

    companion object {
        const val REQUEST_CODE_QUESTION = 100
    }
}