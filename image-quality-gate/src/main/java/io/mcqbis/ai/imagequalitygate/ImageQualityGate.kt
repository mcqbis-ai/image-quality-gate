package io.mcqbis.ai.imagequalitygate

import android.graphics.Bitmap
import io.mcqbis.ai.imagequalitygate.internal.BlurAnalyzer
import io.mcqbis.ai.imagequalitygate.internal.ContrastAnalyzer
import io.mcqbis.ai.imagequalitygate.internal.ExposureAnalyzer
import io.mcqbis.ai.imagequalitygate.internal.ImageProcessor
import io.mcqbis.ai.imagequalitygate.internal.NoiseAnalyzer

object ImageQualityGate {

    private inline fun <T> measureIf(
        enabled: Boolean,
        block: () -> T
    ): Pair<T, Long> {
        if (!enabled) return block() to 0L

        val start = System.nanoTime()
        val result = block()
        val timeMs = (System.nanoTime() - start) / 1_000_000
        return result to timeMs
    }

    fun analyze(
        bitmap: Bitmap,
        weights: QualityMetricWeights = QualityMetricWeights(),
        enableDebugInfo: Boolean = false
    ): ImageQualityResult {

        val (processed, preprocessingTime) =
            if (enableDebugInfo) {
                measureIf(true) {
                    ImageProcessor.preprocess(bitmap)
                }
            } else {
                ImageProcessor.preprocess(bitmap) to 0L
            }

        val (blurScore, blurTime) =
            if (weights.blur != 0f)
                measureIf(enableDebugInfo) { BlurAnalyzer.analyze(processed) }
            else 0f to 0L

        val (noiseScore, noiseTime) =
            if (weights.noise != 0f)
                measureIf(enableDebugInfo) { NoiseAnalyzer.analyze(processed) }
            else 0f to 0L

        val (exposureScore, exposureTime) =
            if (weights.exposure != 0f)
                measureIf(enableDebugInfo) { ExposureAnalyzer.analyze(processed) }
            else 0f to 0L

        val (contrastScore, contrastTime) =
            if (weights.contrast != 0f)
                measureIf(enableDebugInfo) { ContrastAnalyzer.analyze(processed) }
            else 0f to 0L

        val debugInfo = if (enableDebugInfo) {
            DebugInfo(
                preprocessingTimeMs = preprocessingTime,
                blurTimeMs = blurTime,
                noiseTimeMs = noiseTime,
                exposureTimeMs = exposureTime,
                contrastTimeMs = contrastTime,
                totalTimeMs =
                    preprocessingTime +
                            blurTime +
                            noiseTime +
                            exposureTime +
                            contrastTime,
                preprocessedBitmap = processed
            )
        } else null

        val summary = computeSummary(
            blurScore,
            noiseScore,
            exposureScore,
            contrastScore,
            weights
        )

        return ImageQualityResult(
            blurScore = blurScore,
            noiseScore = noiseScore,
            exposureScore = exposureScore,
            contrastScore = contrastScore,
            summaryScore = summary,
            debugInfo = debugInfo
        )
    }

    private fun computeSummary(
        blur: Float,
        noise: Float,
        exposure: Float,
        contrast: Float,
        weights: QualityMetricWeights
    ): Float {
        val totalWeight =
            weights.blur +
                    weights.noise +
                    weights.exposure +
                    weights.contrast

        if (totalWeight == 0f) return 0f

        return (
                blur * weights.blur +
                        noise * weights.noise +
                        exposure * weights.exposure +
                        contrast * weights.contrast
                ) / totalWeight
    }
}