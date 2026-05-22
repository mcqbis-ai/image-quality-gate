package io.mcqbis.ai.imagequalitygate

data class QualityMetricWeights(

    val blur: Float = 1f,

    val exposure: Float = 1f,

    val noise: Float = 0.5f,

    val contrast: Float = 0.5f
)