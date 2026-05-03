package com.annaorazov.screenguard.learn_game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.annaorazov.screenguard.R
import kotlin.math.ceil

class QuestionMapView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    val points = mutableListOf<PointF>()
    private val correctProgress = mutableSetOf<Int>()
    private val incorrectProgress = mutableSetOf<Int>()
    private var isPremiumUnlocked: Boolean = false
    private val PREMIUM_QUESTION_LIMIT = 5
    private val paintText = Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        textSize = 88f
    }
    private val paintBackground = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    private var onPointClick: ((Int) -> Unit)? = null
    private val pointRadius = 88f
    private val padding = 16f
    private var columns = 5

    fun setPoints(newPoints: List<PointF>, correct: Set<Int>, incorrect: Set<Int>, premiumUnlocked: Boolean) {
        points.clear()
        points.addAll(newPoints)
        correctProgress.clear()
        correctProgress.addAll(correct)
        incorrectProgress.clear()
        incorrectProgress.addAll(incorrect)
        isPremiumUnlocked = premiumUnlocked
        requestLayout()
        invalidate()
    }

    fun setOnPointClickListener(listener: (Int) -> Unit) {
        onPointClick = listener
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val rows = ceil(points.size.toFloat() / columns).toInt()
        val height = (rows * (pointRadius * 2 + padding)).toInt() + padding.toInt()
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (points.isEmpty()) return

        val itemWidth = width.toFloat() / columns
        val itemHeight = pointRadius * 2 + padding

        points.forEachIndexed { index, _ ->
            val row = index / columns
            val col = index % columns
            val x = col * itemWidth + itemWidth / 2
            val y = row * itemHeight + itemHeight / 2

            // Determine color based on answer status and premium status
            val (bgRes, textRes) = when {
                index >= PREMIUM_QUESTION_LIMIT && !isPremiumUnlocked -> {
                    Pair(R.color.locked_question_background, R.color.locked_question_text)
                }
                correctProgress.contains(index) -> {
                    Pair(R.color.completed_question_background, R.color.completed_question_text)
                }
                incorrectProgress.contains(index) -> {
                    Pair(R.color.incorrect_question_background, R.color.incorrect_question_text)
                }
                else -> {
                    Pair(R.color.question_number_background, R.color.question_number_text)
                }
            }

            // Draw circular background
            paintBackground.color = ContextCompat.getColor(context, bgRes)
            canvas.drawCircle(x, y, pointRadius, paintBackground)

            // Draw question number
            paintText.color = ContextCompat.getColor(context, textRes)
            canvas.drawText((index + 1).toString(), x, y + paintText.textSize / 3, paintText)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val itemWidth = width.toFloat() / columns
            val itemHeight = pointRadius * 2 + padding

            points.forEachIndexed { index, _ ->
                val row = index / columns
                val col = index % columns
                val x = col * itemWidth + itemWidth / 2
                val y = row * itemHeight + itemHeight / 2

                val dx = event.x - x
                val dy = event.y - y
                if (dx * dx + dy * dy <= pointRadius * pointRadius) {
                    onPointClick?.invoke(index)
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }
}