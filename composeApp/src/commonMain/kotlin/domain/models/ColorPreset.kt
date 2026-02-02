package domain.models

import kotlinx.serialization.Serializable

@Serializable
data class ColorPreset(
    val id: String,
    val name: String,
    val description: String? = null,
    val createdAt: Long,
    val hsl: HslAdjustment,
    val curves: CurveConfig,
    val thumbnail: String? = null  // Base64 encoded preview image
)