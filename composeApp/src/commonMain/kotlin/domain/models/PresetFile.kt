package domain.models

import kotlinx.serialization.Serializable

/**
 * 预设文件格式
 * 版本: v1
 */
@Serializable
data class PresetFile(
    val version: String = "v1",
    val name: String,
    val description: String? = null,
    val createdAt: Long,
    val hsl: HslAdjustment,
    val curves: CurveConfig
)
