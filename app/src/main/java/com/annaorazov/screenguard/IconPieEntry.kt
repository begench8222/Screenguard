package com.annaorazov.screenguard

import android.graphics.Bitmap
import com.github.mikephil.charting.data.PieEntry

class IconPieEntry(
    value: Float,
    label: String,
    val icon: Bitmap? // Add an icon field
) : PieEntry(value, label)