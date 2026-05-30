package io.mcqbis.ai.imagequalitygate.internal

import android.graphics.Bitmap

internal object BlurAnalyzer {

    fun analyze(bitmap: Bitmap): Float {
        val width = bitmap.width
        val height = bitmap.height

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // grayscale buffer
        val gray = FloatArray(width * height)

        for (i in pixels.indices) {
            val c = pixels[i]
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF

            gray[i] = (0.299f * r + 0.587f * g + 0.114f * b)
        }

        var sum = 0.0
        var sumSq = 0.0
        var count = 0

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {

                val i = y * width + x

                val center = gray[i]

                val top = gray[(y - 1) * width + x]
                val bottom = gray[(y + 1) * width + x]
                val left = gray[y * width + (x - 1)]
                val right = gray[y * width + (x + 1)]

                val value =
                    4 * center -
                            top - bottom - left - right


                sum += value
                sumSq += value * value
                count++
            }
        }

        if (count == 0) return 0f

        val mean = sum / count
        val variance = (sumSq / count) - (mean * mean)

        // normalize to 0..100
        val normalized = kotlin.math.ln(1 + variance)

        val score = (normalized / 10.0 * 100.0)
            .coerceIn(1.0, 100.0)
            .toFloat()

        return score
    }
}