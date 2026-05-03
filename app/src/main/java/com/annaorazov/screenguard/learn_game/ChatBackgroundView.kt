package com.annaorazov.screenguard.learn_game

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.annaorazov.screenguard.R
import kotlin.math.sqrt
import kotlin.random.Random

class ChatBackgroundView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val iconResIds = listOf(
        R.drawable.icon1,
        R.drawable.icon2,
        R.drawable.icon3,
        R.drawable.icon4,
        R.drawable.icon5,
        R.drawable.icon6,
        R.drawable.icon7,
        R.drawable.icon8,
    )

    private val iconPositions = mutableListOf<Triple<Float, Float, Float>>() // x, y, rotation
    private val iconSize = dpToPx(48f)
    private val iconScale = iconSize / 800f
    private val minSpacing = dpToPx(80f) // Увеличил расстояние для меньшей плотности

    init {
        setWillNotDraw(false)
        generateIconPositions()
    }

    private fun generateIconPositions() {
        iconPositions.clear()
        val width = resources.displayMetrics.widthPixels.toFloat()
        val height = resources.displayMetrics.heightPixels.toFloat() * 1.2f // Уменьшил высоту для плотности
        val iconCount = (width * height / (minSpacing * minSpacing) * 0.8f).toInt().coerceAtMost(50) // Меньше иконок

        repeat(iconCount) {
            var attempts = 0
            val maxAttempts = 50
            while (attempts < maxAttempts) {
                val x = Random.nextFloat() * width
                val y = Random.nextFloat() * height
                val rotation = Random.nextFloat() * 30f - 15f

                val isValidPosition = iconPositions.all { pos ->
                    val dx = pos.first - x
                    val dy = pos.second - y
                    sqrt(dx * dx + dy * dy) > minSpacing
                }

                if (isValidPosition) {
                    iconPositions.add(Triple(x, y, rotation))
                    break
                }
                attempts++
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        generateIconPositions()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Фон (берём из темы, а не фиксированный цвет)
        canvas.drawColor(
            ContextCompat.getColor(
                context,
                R.color.background_color_map
            )
        )

        // Цвет иконок
        val tintColor = ContextCompat.getColor(
            context,
            R.color.chat_icon_tint
        )

        iconPositions.forEachIndexed { index, (x, y, rotation) ->
            val iconResId = iconResIds[index % iconResIds.size]
            val drawable = ContextCompat.getDrawable(context, iconResId)?.mutate() ?: return@forEachIndexed

            drawable.setTint(tintColor)
            drawable.setTintMode(android.graphics.PorterDuff.Mode.SRC_IN)
            drawable.alpha = 80 // Увеличил прозрачность для видимости

            canvas.save()
            canvas.translate(x, y)
            canvas.rotate(rotation, iconSize / 2f, iconSize / 2f)
            canvas.scale(iconScale, iconScale, iconSize / 2f, iconSize / 2f)
            drawable.setBounds(0, 0, 800, 800)
            drawable.draw(canvas)
            canvas.restore()
        }
    }

    private fun isDarkTheme(): Boolean {
        return resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }

    private fun dpToPx(dp: Float): Float {
        return dp * resources.displayMetrics.density
    }
}