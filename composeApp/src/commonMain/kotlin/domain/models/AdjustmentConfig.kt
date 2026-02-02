package domain.models

import kotlinx.serialization.Serializable

@Serializable
data class AdjustmentConfig(
    val hsl: HslAdjustment = HslAdjustment(),
    val curves: CurveConfig = CurveConfig()
)

@Serializable
data class HslAdjustment(
    val hue: Float = 0f,        // -180 to 180
    val saturation: Float = 0f,  // -100 to 100
    val lightness: Float = 0f    // -100 to 100
)

@Serializable
data class CurveConfig(
    val rgb: List<CurvePoint> = listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f)),
    val r: List<CurvePoint> = listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f)),
    val g: List<CurvePoint> = listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f)),
    val b: List<CurvePoint> = listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f))
)

@Serializable
data class CurvePoint(
    val x: Float,  // 0-1
    val y: Float   // 0-1
)