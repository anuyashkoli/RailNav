package com.app.railnav.data

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable

object MapUtils {
    private val iconCache = LruCache<String, Drawable>(100)

    /**
     * Core function to tint and resize any drawable resource
     */
    private fun getThemedIcon(context: Context, resId: Int, color: Int, sizeDp: Int): Drawable {
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

    /**
     * Handles the big map pins (Start, End, Live GPS)
     */
    fun getSystemMarker(context: Context, type: String): Drawable? {
        iconCache.get(type)?.let { return it }

        val drawable = when (type) {
            "START" -> getThemedIcon(context, com.app.railnav.R.drawable.location, Color.parseColor("#4CAF50"), 42)
            "END" -> getThemedIcon(context, com.app.railnav.R.drawable.location, Color.parseColor("#F44336"), 42)
            "GPS" -> getThemedIcon(context, com.app.railnav.R.drawable.gps_on, Color.parseColor("#2196F3"), 28)
            else -> return null
        }

        iconCache.put(type, drawable)
        return drawable
    }

    /**
     * Handles all station facility nodes and generic pathways
     * FIX: Now correctly accepts the pre-combined searchStr from MapView
     */
    fun getNodeIcon(context: Context, searchStr: String, themeColor: Int): Drawable {
        return when {
            searchStr.contains("ENTRY") || searchStr.contains("EXIT") ->
                getThemedIcon(context, com.app.railnav.R.drawable.entryexit, themeColor, 28)

            searchStr.contains("STAIR") ->
                getThemedIcon(context, com.app.railnav.R.drawable.stairs, themeColor, 28)

            searchStr.contains("LIFT") || searchStr.contains("ELEVATOR") ->
                getThemedIcon(context, com.app.railnav.R.drawable.elevator, themeColor, 28)

            searchStr.contains("ESCALATOR") ->
                getThemedIcon(context, com.app.railnav.R.drawable.escalator, themeColor, 28)

            else ->
                // Generic pathway nodes are kept tiny (8dp) so they don't clutter the map
                getThemedIcon(context, org.osmdroid.library.R.drawable.ic_menu_mylocation, Color.GRAY, 8)
        }
    }
}