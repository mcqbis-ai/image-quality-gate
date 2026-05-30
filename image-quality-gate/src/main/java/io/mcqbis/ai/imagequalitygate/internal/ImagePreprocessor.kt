package io.mcqbis.ai.imagequalitygate.internal

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale


internal object ImageProcessor {

    fun preprocess(input: Bitmap): Bitmap {

        val gray = toGrayscale(input)
        val resized = resize(gray, 256, 256)

        return resized
    }

    private fun resize(bitmap: Bitmap, w: Int, h: Int): Bitmap {
        return bitmap.scale(w, h)
    }

    private fun toGrayscale(src: Bitmap): Bitmap {

        val width = src.width
        val height = src.height

        val output = createBitmap(width, height)

        val canvas = Canvas(output)
        val paint = Paint()

        val matrix = ColorMatrix().apply {
            setSaturation(0f)
        }

        val filter = ColorMatrixColorFilter(matrix)
        paint.colorFilter = filter

        canvas.drawBitmap(src, 0f, 0f, paint)

        return output
    }
}