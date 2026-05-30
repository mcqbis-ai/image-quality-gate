package io.mcqbis.ai.imagequalitygate

import android.graphics.Bitmap

data class DebugInfo(
    val preprocessingTimeMs: Long,
    val blurTimeMs: Long,
    val noiseTimeMs: Long,
    val exposureTimeMs: Long,
    val contrastTimeMs: Long,
    val totalTimeMs: Long,
    val preprocessedBitmap: Bitmap?,
)
