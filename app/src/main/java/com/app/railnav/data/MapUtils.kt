package com.app.railnav.data

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColorInt

object MapUtils {
    private val iconCache = LruCache<String, Drawable>(50)

    fun getThemedIcon(context: Context, resId: Int, color: Int, sizeDp: Int): Drawable {
        val key = "${resId}_${color}_${sizeDp}"
        iconCache.get(key)?.let { return it }
        val drawable = ContextCompat.getDrawable(context, resId) ?: return Color.TRANSPARENT.toDrawable()
        val wrapped = DrawableCompat.wrap(drawable).mutate()
        DrawableCompat.setTint(wrapped, color)
        val sizePx = (sizeDp * context.resources.displayMetrics.density).toInt()
        val bitmap = wrapped.toBitmap(sizePx, sizePx).toDrawable(context.resources)
        iconCache.put(key, bitmap)
        return bitmap
    }

    fun getCachedMarker(context: Context, type: String): Drawable {
        iconCache.get(type)?.let { return it }
        val drawable = when (type) {
            "START" -> createStartMarkerIcon(context)
            "END" -> createEndMarkerIcon(context)
            else -> createGpsLocationIcon(context)
        }
        iconCache.put(type, drawable)
        return drawable
    }

    fun createStartMarkerIcon(context: Context): Drawable {
        val size = (48 * context.resources.displayMetrics.density).toInt()
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = "#4CAF50".toColorInt() }
        canvas.drawCircle(size/2f, size/2f, size/2.5f, paint)
        return bitmap.toDrawable(context.resources)
    }

    fun createEndMarkerIcon(context: Context): Drawable {
        val size = (48 * context.resources.displayMetrics.density).toInt()
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = "#F44336".toColorInt() }
        canvas.drawCircle(size/2f, size/2f, size/2.5f, paint)
        return bitmap.toDrawable(context.resources)
    }

    fun createGpsLocationIcon(context: Context): Drawable {
        val size = (24 * context.resources.displayMetrics.density).toInt()
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = "#2196F3".toColorInt() }
        canvas.drawCircle(size/2f, size/2f, size/3f, paint)
        return bitmap.toDrawable(context.resources)
    }
}