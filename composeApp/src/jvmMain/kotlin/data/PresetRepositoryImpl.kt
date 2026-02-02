package data

import domain.models.ColorPreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.io.File

class PresetRepositoryImpl(
    private val presetsDir: String = System.getProperty("user.home") + "/.pdf-color-adjuster"
) : PresetRepository {

    private val presetsFile = File(presetsDir, "presets.json")
    private val json = Json { prettyPrint = true }

    init {
        // 确保目录存在
        File(presetsDir).mkdirs()
    }

    override suspend fun savePreset(preset: ColorPreset) = withContext(Dispatchers.IO) {
        val presets = loadPresets().toMutableList()

        // 如果已存在相同ID的预设，则替换
        val existingIndex = presets.indexOfFirst { it.id == preset.id }
        if (existingIndex >= 0) {
            presets[existingIndex] = preset
        } else {
            presets.add(preset)
        }

        val jsonString = json.encodeToString(presets)
        presetsFile.writeText(jsonString)
    }

    override suspend fun loadPresets(): List<ColorPreset> = withContext(Dispatchers.IO) {
        if (!presetsFile.exists()) {
            return@withContext emptyList()
        }

        try {
            val jsonString = presetsFile.readText()
            json.decodeFromString<List<ColorPreset>>(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun deletePreset(id: String) = withContext(Dispatchers.IO) {
        val presets = loadPresets().toMutableList()
        presets.removeAll { it.id == id }

        val jsonString = json.encodeToString(presets)
        presetsFile.writeText(jsonString)
    }

    override suspend fun getPreset(id: String): ColorPreset? = withContext(Dispatchers.IO) {
        loadPresets().find { it.id == id }
    }
}