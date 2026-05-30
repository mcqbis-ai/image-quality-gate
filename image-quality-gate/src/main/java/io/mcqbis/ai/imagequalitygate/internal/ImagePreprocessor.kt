package io.mcqbis.ai.imagequalitygate.internal

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.util.Log


internal object ImageProcessor {

    fun preprocess(input: Bitmap): ProcessResult {

        val start = System.nanoTime()

        val gray = toGrayscale(input)
        val resized = resize(gray, 256, 256)

        val end = System.nanoTime()

        val timeMs = (end - start) / 1_000_000

        Log.d("ImageProcessor", "Preprocess time: ${timeMs}ms")

        return ProcessResult(
            bitmap = resized,
            timeMs = timeMs
        )
    }

    private fun resize(bitmap: Bitmap, w: Int, h: Int): Bitmap {
        return Bitmap.createScaledBitmap(bitmap, w, h, true)
    }

    private fun toGrayscale(src: Bitmap): Bitmap {

        val width = src.width
        val height = src.height

        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

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