package io.mcqbis.ai.imagequalitygate

data class ImageQualityResult(

    val blurScore: Float?,

    val exposureScore: Float?,

    val noiseScore: Float?,

    val contrastScore: Float?,

    val summaryScore: Float,

    val debugInfo: DebugInfo?,
) {

    fun isTooBlurry(
        threshold: Float = 40f
    ): Boolean {

        return requireScore(blurScore) < threshold
    }

    fun isExposureTooLow(
        threshold: Float = 30f
    ): Boolean {

        return requireScore(exposureScore) < threshold
    }

    fun isTooNoisy(
        threshold: Float = 40f
    ): Boolean {

        return requireScore(noiseScore) < threshold
    }

    fun isContrastTooLow(
        threshold: Float = 35f
    ): Boolean {

        return requireScore(contrastScore) < threshold
    }

    fun isSummaryScoreTooLow(
        threshold: Float = 50f
    ): Boolean {

        return summaryScore < threshold
    }

    private fun requireScore(
        score: Float?
    ): Float {

        return score ?: throw IllegalStateException(
            "Metric was not analyzed."
        )
    }
}