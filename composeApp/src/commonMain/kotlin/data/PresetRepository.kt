package data

import domain.models.ColorPreset

interface PresetRepository {
    suspend fun savePreset(preset: ColorPreset)
    suspend fun loadPresets(): List<ColorPreset>
    suspend fun deletePreset(id: String)
    suspend fun getPreset(id: String): ColorPreset?
}