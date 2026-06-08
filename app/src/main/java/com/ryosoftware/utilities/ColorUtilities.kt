package com.ryosoftware.utilities

import android.content.Context

object ColorUtilities {

    fun blend(color1: Int, color2: Int, ratio: Float): Int {
        val r = ratio.coerceIn(0f, 1f)
        val inv = 1f - r
        val a = ((color1 shr 24 and 0xFF) * inv + (color2 shr 24 and 0xFF) * r).toInt()
        val red = ((color1 and 0xFF0000 shr 16) * inv + (color2 and 0xFF0000 shr 16) * r).toInt()
        val green = ((color1 and 0xFF00 shr 8) * inv + (color2 and 0xFF00 shr 8) * r).toInt()
        val blue = ((color1 and 0xFF) * inv + (color2 and 0xFF) * r).toInt()
        return a shl 24 or (red shl 16) or (green shl 8) or blue
    }

    fun decrease(color: Int, value: Int): Int {
        val a = color shr 24 and 0xFF
        val red = maxOf(0, (color and 0xFF0000 shr 16) - (value and 0xFF0000 shr 16))
        val green = maxOf(0, (color and 0xFF00 shr 8) - (value and 0xFF00 shr 8))
        val blue = maxOf(0, (color and 0xFF) - (value and 0xFF))
        return a shl 24 or (red shl 16) or (green shl 8) or blue
    }

    fun increase(color: Int, value: Int): Int {
        val a = color shr 24 and 0xFF
        val red = minOf(0xFF, (color and 0xFF0000 shr 16) + (value and 0xFF0000 shr 16))
        val green = minOf(0xFF, (color and 0xFF00 shr 8) + (value and 0xFF00 shr 8))
        val blue = minOf(0xFF, (color and 0xFF) + (value and 0xFF))
        return a shl 24 or (red shl 16) or (green shl 8) or blue
    }

    fun getColorFromResource(context: Context, resource: Int): Int {
        return context.resources.getColor(resource, null)
    }
}
