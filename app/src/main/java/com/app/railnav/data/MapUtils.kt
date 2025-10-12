package com.app.railnav.data

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import com.app.railnav.R
import androidx.core.graphics.scale
import androidx.core.graphics.drawable.toDrawable

object MapUtils {

    // Maps a node type string to a drawable resource ID
    fun getIconForNodeType(nodeType: String?): Int {
        return when (nodeType) {
            "ENTRY/EXIT" -> R.drawable.entry_exit
            "STAIRWAY_TOP", "STAIRWAY_BOT" -> R.drawable.stairway
            "LIFT_TOP", "LIFT_BOT" -> R.drawable.lift
            else -> org.osmdroid.library.R.drawable.ic_menu_mylocation
        }
    }

    fun createScaledIcon(context: Context, drawableId: Int, sizeDp: Int): Drawable {
        val density = context.resources.displayMetrics.density
        val sizePx = (sizeDp * density).toInt()
        val bitmap = BitmapFactory.decodeResource(context.resources, drawableId)
        val scaledBitmap = bitmap.scale(sizePx, sizePx)
        return scaledBitmap.toDrawable(context.resources)
    }

    /**
     * Creates a custom start marker with a green pin and "A" label
     */
    fun createStartMarkerIcon(context: Context): Drawable {
        val density = context.resources.displayMetrics.density
        val width = (48 * density).toInt()
        val height = (64 * density).toInt()

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw pin shadow
        val shadowPaint = Paint().apply {
            color = Color.argb(80, 0, 0, 0)
            style = Paint.Style.FILL
            maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawCircle(width / 2f, height - 8f, width / 4f, shadowPaint)

        // Draw main pin body
        val pinPaint = Paint().apply {
            color = Color.parseColor("#4CAF50") // Green
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val pinPath = Path().apply {
            moveTo(width / 2f, height - 10f)
            lineTo(width / 2f - width / 6f, height / 2f)
            arcTo(
                RectF(
                    width / 6f,
                    height / 6f,
                    width - width / 6f,
                    height / 2f
                ),
                180f,
                180f
            )
            lineTo(width / 2f, height - 10f)
            close()
        }
        canvas.drawPath(pinPath, pinPaint)

        // Draw white circle for letter
        val circlePaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(width / 2f, height / 3f, width / 5f, circlePaint)

        // Draw "A" letter
        val textPaint = Paint().apply {
            color = Color.parseColor("#4CAF50")
            textSize = width / 3f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val textBounds = Rect()
        textPaint.getTextBounds("A", 0, 1, textBounds)
        canvas.drawText(
            "A",
            width / 2f,
            height / 3f + textBounds.height() / 2f,
            textPaint
        )

        return bitmap.toDrawable(context.resources)
    }

    /**
     * Creates a custom end marker with a red pin and "B" label
     */
    fun createEndMarkerIcon(context: Context): Drawable {
        val density = context.resources.displayMetrics.density
        val width = (48 * density).toInt()
        val height = (64 * density).toInt()

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw pin shadow
        val shadowPaint = Paint().apply {
            color = Color.argb(80, 0, 0, 0)
            style = Paint.Style.FILL
            maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawCircle(width / 2f, height - 8f, width / 4f, shadowPaint)

        // Draw main pin body
        val pinPaint = Paint().apply {
            color = Color.parseColor("#F44336") // Red
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val pinPath = Path().apply {
            moveTo(width / 2f, height - 10f)
            lineTo(width / 2f - width / 6f, height / 2f)
            arcTo(
                RectF(
                    width / 6f,
                    height / 6f,
                    width - width / 6f,
                    height / 2f
                ),
                180f,
                180f
            )
            lineTo(width / 2f, height - 10f)
            close()
        }
        canvas.drawPath(pinPath, pinPaint)

        // Draw white circle for letter
        val circlePaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(width / 2f, height / 3f, width / 5f, circlePaint)

        // Draw "B" letter
        val textPaint = Paint().apply {
            color = Color.parseColor("#F44336")
            textSize = width / 3f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val textBounds = Rect()
        textPaint.getTextBounds("B", 0, 1, textBounds)
        canvas.drawText(
            "B",
            width / 2f,
            height / 3f + textBounds.height() / 2f,
            textPaint
        )

        return bitmap.toDrawable(context.resources)
    }

    /**
     * Creates a pulsing effect marker (can be used for selected location)
     */
    fun createPulseMarkerIcon(context: Context, color: Int): Drawable {
        val density = context.resources.displayMetrics.density
        val size = (32 * density).toInt()

        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Outer ring (pulse effect)
        val outerPaint = Paint().apply {
            this.color = Color.argb(60, Color.red(color), Color.green(color), Color.blue(color))
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4f, outerPaint)

        // Inner circle
        val innerPaint = Paint().apply {
            this.color = color
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 3f, innerPaint)

        // White center dot
        val centerPaint = Paint().apply {
            this.color = Color.WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 6f, centerPaint)

        return bitmap.toDrawable(context.resources)
    }

    /**
     * Creates a GPS location icon (small blue dot with white border)
     */
    fun createGpsLocationIcon(context: Context): Drawable {
        val density = context.resources.displayMetrics.density
        val size = (24 * density).toInt()

        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Outer light blue circle (pulse effect)
        val outerPaint = Paint().apply {
            color = Color.argb(100, 33, 150, 243)
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2f, outerPaint)

        // White border
        val borderPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 3.5f, borderPaint)

        // Blue center dot
        val centerPaint = Paint().apply {
            color = Color.parseColor("#2196F3") // Material Blue
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 4.5f, centerPaint)

        return bitmap.toDrawable(context.resources)
    }

    /**
     * Creates a navigation icon (user position snapped to route)
     * Arrow/circle hybrid to show direction
     */
    fun createNavigationIcon(context: Context): Drawable {
        val density = context.resources.displayMetrics.density
        val size = (40 * density).toInt()

        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Shadow
        val shadowPaint = Paint().apply {
            color = Color.argb(80, 0, 0, 0)
            style = Paint.Style.FILL
            maskFilter = BlurMaskFilter(6f, BlurMaskFilter.Blur.NORMAL)
            isAntiAlias = true
        }
        canvas.drawCircle(size / 2f, size / 2f + 2f, size / 2.5f, shadowPaint)

        // Outer green circle
        val outerPaint = Paint().apply {
            color = Color.parseColor("#4CAF50") // Green
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2.5f, outerPaint)

        // White border
        val borderPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 3f * density
            isAntiAlias = true
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2.5f, borderPaint)

        // Inner white circle
        val innerPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 4f, innerPaint)

        // Direction arrow (pointing up)
        val arrowPaint = Paint().apply {
            color = Color.parseColor("#4CAF50")
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val arrowPath = Path().apply {
            moveTo(size / 2f, size / 3f) // Top point
            lineTo(size / 2f - size / 8f, size / 2f) // Left point
            lineTo(size / 2f, size / 2.5f) // Middle indent
            lineTo(size / 2f + size / 8f, size / 2f) // Right point
            close()
        }
        canvas.drawPath(arrowPath, arrowPaint)

        return bitmap.toDrawable(context.resources)
    }
}