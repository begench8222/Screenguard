package com.annaorazov.screenguard

import android.content.Context
import android.graphics.*
import android.util.DisplayMetrics
import com.github.mikephil.charting.animation.ChartAnimator
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.interfaces.datasets.IDataSet
import com.github.mikephil.charting.renderer.PieChartRenderer
import com.github.mikephil.charting.utils.ViewPortHandler

class IconPieChartRenderer(
    chart: PieChart,
    animator: ChartAnimator,
    viewPortHandler: ViewPortHandler,
    private val context: Context // Add context to access resources
) : PieChartRenderer(chart, animator, viewPortHandler) {

    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val centerCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.FILL
        strokeWidth = 5f
        setShadowLayer(10f, 0f, 0f, Color.GRAY)
    }

    // Function to calculate icon size based on screen density
    private fun getIconSize(): Float {
        val displayMetrics = context.resources.displayMetrics
        // Base size in dp (e.g., 24dp)
        val baseSizeInDp = 44f
        // Convert dp to pixels based on screen density
        return baseSizeInDp * (displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
    }

    // Function to calculate circle radius based on screen density
    private fun getCircleRadius(): Float {
        val displayMetrics = context.resources.displayMetrics
        // Base radius in dp (e.g., 80% of hole radius)
        val baseRadius = mChart.holeRadius * 1.7f
        // Adjust radius based on screen density
        return baseRadius * (displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
    }

    override fun drawEntryLabel(canvas: Canvas, label: String, x: Float, y: Float) {
        val dataSet = mChart.data.getDataSet()
        val entry = findEntryByLabel(dataSet, label) as? IconPieEntry

        if (entry != null && entry.icon != null) {
            val icon = entry.icon
            val iconSize = getIconSize() // Get dynamically calculated icon size

            // Calculate the position for the icon (above the label)
            val iconX = x - iconSize / 2
            val iconY = y - iconSize - convertDpToPixel(10f, context) // Adjust the vertical offset

            // Scale the icon bitmap
            val scaledIcon = Bitmap.createScaledBitmap(icon, iconSize.toInt(), iconSize.toInt(), true)

            // Draw the icon
            val iconRect = RectF(iconX, iconY, iconX + iconSize, iconY + iconSize)
            canvas.drawBitmap(scaledIcon, null, iconRect, iconPaint)
        }

        // Draw the text label below the icon
        super.drawEntryLabel(canvas, label, x, y)
    }

    override fun drawExtras(canvas: Canvas) {
        super.drawExtras(canvas)

        // Draw a custom circle in the center of the donut chart
        val center = mChart.centerOffsets
        val radius = getCircleRadius() // Get dynamically calculated circle radius

        canvas.drawCircle(center.x, center.y, radius, centerCirclePaint)
    }

    private fun findEntryByLabel(dataSet: IDataSet<out Entry>, label: String): PieEntry? {
        for (i in 0 until dataSet.entryCount) {
            val entry = dataSet.getEntryForIndex(i)
            if (entry is PieEntry && entry.label == label) {
                return entry
            }
        }
        return null
    }

    private fun convertDpToPixel(dp: Float, context: Context): Float {
        return dp * (context.resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
    }
}