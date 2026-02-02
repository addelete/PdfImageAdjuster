package platform

import domain.models.PresetFile
import domain.models.ColorPreset
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.io.File

/**
 * 预设存储服务
 * 负责预设文件的读写操作
 */
actual class PresetStorage {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    /**
     * 获取预设存储目录
     */
    fun getPresetDirectory(): File {
        val appDataDir = System.getenv("APPDATA") ?: System.getProperty("user.home")
        val presetDir = File(appDataDir, "PdfImageAdjuster/presets")

        // 确保目录存在
        if (!presetDir.exists()) {
            presetDir.mkdirs()
        }

        return presetDir
    }

    /**
     * 加载所有预设
     */
    actual fun loadAllPresets(): List<ColorPreset> {
        val presetDir = getPresetDirectory()
        val presets = mutableListOf<ColorPreset>()

        presetDir.listFiles { file -> file.extension == "json" }?.forEach { file ->
            try {
                val content = file.readText()
                val presetFile = json.decodeFromString<PresetFile>(content)

                // 转换为 ColorPreset
                val preset = ColorPreset(
                    id = file.nameWithoutExtension,
                    name = presetFile.name,
                    description = presetFile.description,
                    createdAt = presetFile.createdAt,
                    hsl = presetFile.hsl,
                    curves = presetFile.curves
                )
                presets.add(preset)
            } catch (e: Exception) {
                println("Failed to load preset from ${file.name}: ${e.message}")
            }
        }

        return presets.sortedByDescending { it.createdAt }
    }

    /**
     * 保存预设
     * @param preset 要保存的预设
     * @return 是否保存成功
     */
    actual fun savePreset(preset: ColorPreset): Boolean {
        return try {
            val presetDir = getPresetDirectory()

            // 使用预设名称作为文件名（清理非法字符）
            val fileName = sanitizeFileName(preset.name) + ".json"
            val file = File(presetDir, fileName)

            // 转换为 PresetFile 格式
            val presetFile = PresetFile(
                version = "v1",
                name = preset.name,
                description = preset.description,
                createdAt = preset.createdAt,
                hsl = preset.hsl,
                curves = preset.curves
            )

            // 序列化并写入文件
            val content = json.encodeToString(presetFile)
            file.writeText(content)

            true
        } catch (e: Exception) {
            println("Failed to save preset: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    /**
     * 删除预设
     * @param presetId 预设ID（文件名，不含扩展名）
     * @return 是否删除成功
     */
    actual fun deletePreset(presetId: String): Boolean {
        return try {
            val presetDir = getPresetDirectory()
            val file = File(presetDir, "$presetId.json")

            if (file.exists()) {
                val deleted = file.delete()
                deleted
            } else {
                false
            }
        } catch (e: Exception) {
            println("Failed to delete preset: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    /**
     * 清理文件名中的非法字符
     */
    actual fun sanitizeFileName(name: String): String {
        // 替换Windows文件名中的非法字符
        return name.replace(Regex("[<>:\"/\\\\|?*]"), "_")
    }
}