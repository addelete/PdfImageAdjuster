package platform

import domain.models.ColorPreset

expect class PresetStorage() {
    /**
     * 加载所有预设
     */
    fun loadAllPresets(): List<ColorPreset>

    /**
     * 保存预设
     * @param preset 要保存的预设
     * @return 是否保存成功
     */
    fun savePreset(preset: ColorPreset): Boolean

    /**
     * 删除预设
     * @param presetId 预设ID（文件名，不含扩展名）
     * @return 是否删除成功
     */
    fun deletePreset(presetId: String): Boolean

    /**
     * 清理文件名中的非法字符
     */
    fun sanitizeFileName(name: String): String
}
