package com.annaorazov.screenguard.learn_game

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.annaorazov.screenguard.R
import com.annaorazov.screenguard.SwitchLanguageHelper
import com.annaorazov.screenguard.databinding.ActivityQuestionWithImageBinding
import com.annaorazov.screenguard.databinding.ActivityQuestionNoImageBinding
import com.annaorazov.screenguard.utils.TimeUtils
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class QuestionActivity : AppCompatActivity() {
    private var withImageBinding: ActivityQuestionWithImageBinding? = null
    private var noImageBinding: ActivityQuestionNoImageBinding? = null
    private var classLevel: Int = 1
    private var currentQuestion: Int = 1
    private var totalQuestions: Int = 1
    private lateinit var question: Question
    private lateinit var subject: String
    private var isAnswered: Boolean = false
    private var selectedAnswer: Int = -1
    private var isAnswerCorrect: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
    }
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(SwitchLanguageHelper.applyLanguage(newBase))
    }

    override fun onBackPressed() {
        // Если пользователь ответил на вопрос, сохраняем состояние
        if (isAnswered) {
            saveAnswerState()
        }
        super.onBackPressed()
    }

    private fun saveAnswerState() {
        setResult(RESULT_OK, Intent().apply {
            putExtra("questionIndex", currentQuestion - 1)
            putExtra("isCorrect", isAnswerCorrect)
            putExtra("selectedAnswer", selectedAnswer)
        })
    }

    private fun handleIntent(intent: Intent) {
        val questionJson = intent.getStringExtra("question") ?: run {
            showErrorAndFinish("Question data not found")
            return
        }
        currentQuestion = intent.getIntExtra("currentQuestion", 1)
        totalQuestions = intent.getIntExtra("totalQuestions", 1)
        classLevel = intent.getIntExtra("classLevel", 1)
        subject = intent.getStringExtra("subject") ?: ""

        // Сбрасываем состояние ответа при создании нового вопроса
        isAnswered = false
        selectedAnswer = -1
        isAnswerCorrect = false

        try {
            question = Gson().fromJson(questionJson, Question::class.java)
            if (question.hasImage(this, classLevel)) {
                withImageBinding = ActivityQuestionWithImageBinding.inflate(layoutInflater)
                setContentView(withImageBinding!!.root)
                setupQuestionUIWithImage(question, currentQuestion, totalQuestions)
                withImageBinding?.questionImage?.post {
                    loadQuestionImage(question.image)
                }
            } else {
                noImageBinding = ActivityQuestionNoImageBinding.inflate(layoutInflater)
                setContentView(noImageBinding!!.root)
                setupQuestionUINoImage(question, currentQuestion, totalQuestions)
            }
            setupTimeDisplay()

            // Проверяем, был ли вопрос уже отвечен
            val isQuestionAnswered = intent.getBooleanExtra("isAlreadyAnswered", false)
            if (isQuestionAnswered) {
                isAnswered = true
                isAnswerCorrect = intent.getBooleanExtra("wasCorrect", false)
                selectedAnswer = intent.getIntExtra("selectedAnswer", -1)
                setupAnswerButtons(question, withImageBinding?.answersLayout ?: noImageBinding?.answersLayout!!)
            }
        } catch (e: Exception) {
            showErrorAndFinish("Error parsing question data: ${e.message}")
        }
    }

    private fun setupTimeDisplay() {
        when {
            withImageBinding != null -> {
                TimeUtils.setupTimeDisplay(
                    this,
                    withImageBinding!!.timeDisplay.remainingTimeText,
                    includePrefix = false
                )
            }
            noImageBinding != null -> {
                TimeUtils.setupTimeDisplay(
                    this,
                    noImageBinding!!.timeDisplay.remainingTimeText,
                    includePrefix = false
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setupTimeDisplay()
    }

    private fun showBonusAnimation() {
        val bonusTimeText = withImageBinding?.bonusTimeText ?: noImageBinding?.bonusTimeText
        bonusTimeText?.let {
            it.text = "+10s"
            it.visibility = View.VISIBLE

            val animation = AnimationUtils.loadAnimation(this, R.anim.bonus_time_animation)
            animation.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}
                override fun onAnimationRepeat(animation: Animation?) {}
                override fun onAnimationEnd(animation: Animation?) {
                    it.visibility = View.GONE
                    setupTimeDisplay()
                }
            })
            it.startAnimation(animation)
        }
    }

    private fun setupQuestionUIWithImage(question: Question, currentQuestion: Int, totalQuestions: Int) {
        withImageBinding?.let { binding ->
            binding.questionProgress.text = getString(R.string.question_progress, currentQuestion, totalQuestions)
            binding.questionText.text = question.questionText
            setupAnswerButtons(question, binding.answersLayout)
        }
    }

    private fun setupQuestionUINoImage(question: Question, currentQuestion: Int, totalQuestions: Int) {
        noImageBinding?.let { binding ->
            binding.questionProgress.text = getString(R.string.question_progress, currentQuestion, totalQuestions)
            binding.questionText.text = question.questionText
            setupAnswerButtons(question, binding.answersLayout)
        }
    }

    private fun loadQuestionImage(imageName: String?) {
        withImageBinding?.questionImage?.let { imageView ->
            try {
                if (!imageName.isNullOrEmpty()) {
                    imageView.setImageResource(R.drawable.simple_placeholder)
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val fullPath = "class_$classLevel/images/$imageName"
                            val inputStream = assets.open(fullPath)
                            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                            BitmapFactory.decodeStream(inputStream, null, options)
                            inputStream.close()

                            val scale = calculateSampleSize(options.outWidth, options.outHeight, imageView.width, imageView.height)
                            val decodeOptions = BitmapFactory.Options().apply {
                                inSampleSize = scale
                                inPreferredConfig = Bitmap.Config.RGB_565
                            }

                            val scaledBitmap = assets.open(fullPath).use { stream ->
                                BitmapFactory.decodeStream(stream, null, decodeOptions)
                            }

                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                scaledBitmap?.let { imageView.setImageBitmap(it) }
                                    ?: imageView.setImageResource(R.drawable.simple_placeholder)
                            }
                        } catch (e: Exception) {
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                imageView.setImageResource(R.drawable.simple_placeholder)
                            }
                        }
                    }
                } else {
                    imageView.setImageResource(R.drawable.simple_placeholder)
                }
            } catch (e: Exception) {
                imageView.setImageResource(R.drawable.simple_placeholder)
            }
        }
    }

    private fun calculateSampleSize(srcWidth: Int, srcHeight: Int, reqWidth: Int, reqHeight: Int): Int {
        var inSampleSize = 1
        if (srcHeight > reqHeight || srcWidth > reqWidth) {
            val halfHeight = srcHeight / 2
            val halfWidth = srcWidth / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun setupAnswerButtons(question: Question, answersLayout: LinearLayout) {
        answersLayout.removeAllViews()

        val isQuestionAnswered = intent.getBooleanExtra("isAlreadyAnswered", false)
        val wasCorrect = intent.getBooleanExtra("wasCorrect", false)

        question.options.forEachIndexed { index, answerText ->
            val button = Button(this).apply {
                text = answerText
                setBackgroundResource(R.drawable.btn_answer_selector)
                setTextColor(ContextCompat.getColor(context, R.color.button_text_color))

                if (isQuestionAnswered) {
                    isEnabled = false
                    if (index == question.correctAnswerIndex) {
                        setBackgroundColor(ContextCompat.getColor(context, R.color.completed_question_background))
                        setTextColor(ContextCompat.getColor(context, R.color.white))
                    }
                    if (!wasCorrect && index == intent.getIntExtra("selectedAnswer", -1)) {
                        setBackgroundColor(ContextCompat.getColor(context, R.color.incorrect_question_background))
                        setTextColor(ContextCompat.getColor(context, R.color.white))
                    }
                } else {
                    setOnClickListener {
                        if (!isAnswered) {
                            selectedAnswer = index
                            checkAnswer(index == question.correctAnswerIndex, index)
                        }
                    }
                }

                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 8.dpToPx(), 0, 8.dpToPx()) }
            }
            answersLayout.addView(button)
        }
    }

    private fun checkAnswer(isCorrect: Boolean, selectedIndex: Int) {
        isAnswered = true
        isAnswerCorrect = isCorrect
        selectedAnswer = selectedIndex
        if (isCorrect) {
            TimeUtils.addBonusTime(this, 10000)
            showBonusAnimation()
        }

        val answersLayout = withImageBinding?.answersLayout ?: noImageBinding?.answersLayout
        answersLayout?.let { layout ->
            for (i in 0 until layout.childCount) {
                val button = layout.getChildAt(i) as Button
                button.isEnabled = false
                if (i == question.correctAnswerIndex) {
                    button.setBackgroundColor(ContextCompat.getColor(this, R.color.completed_question_background))
                    button.setTextColor(ContextCompat.getColor(this, R.color.white))
                }
                if (!isCorrect && i == selectedIndex) {
                    button.setBackgroundColor(ContextCompat.getColor(this, R.color.incorrect_question_background))
                    button.setTextColor(ContextCompat.getColor(this, R.color.white))
                }
            }
        }

        Toast.makeText(
            this,
            if (isCorrect) getString(R.string.answer_correct) else getString(R.string.answer_incorrect),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun showErrorAndFinish(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        finish()
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}