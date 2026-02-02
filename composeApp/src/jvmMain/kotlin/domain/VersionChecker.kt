package domain

import domain.models.UpdateCheckResult
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class VersionInfo(
    val version: String,
    val release_notes: String
)

class VersionChecker {
    private val httpClient = HttpClient()
    private val currentVersion = BuildConfig.VERSION // 从编译时嵌入的版本号
    private val updateUrl = "https://raw.githubusercontent.com/addelete/PdfImageAdjuster/main/version.json"
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun checkForUpdates(): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val response: String = httpClient.get(updateUrl).bodyAsText()
            val versionInfo = json.decodeFromString<VersionInfo>(response)

            if (compareVersions(versionInfo.version, currentVersion) > 0) {
                // 根据当前平台生成下载链接
                val downloadUrl = getDownloadUrlForCurrentPlatform(versionInfo.version)

                UpdateCheckResult.UpdateAvailable(
                    currentVersion = currentVersion,
                    latestVersion = versionInfo.version,
                    downloadUrl = downloadUrl,
                    releaseNotes = versionInfo.release_notes
                )
            } else {
                UpdateCheckResult.NoUpdate(currentVersion)
            }
        } catch (e: Exception) {
            UpdateCheckResult.Error(e.message ?: "Failed to check for updates")
        }
    }

    private fun getDownloadUrlForCurrentPlatform(version: String): String {
        val osName = System.getProperty("os.name").lowercase()
        val baseUrl = "https://github.com/addelete/PdfImageAdjuster/releases/download/v$version"

        return when {
            osName.contains("win") -> "$baseUrl/PdfImageAdjuster-$version.exe"
            osName.contains("mac") -> "$baseUrl/PdfImageAdjuster-$version.dmg"
            osName.contains("nix") || osName.contains("nux") -> "$baseUrl/PdfImageAdjuster-$version.deb"
            else -> "$baseUrl/PdfImageAdjuster-$version.exe" // 默认 Windows
        }
    }

    private fun compareVersions(v1: String, v2: String): Int {
        val parts1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
        val parts2 = v2.split(".").map { it.toIntOrNull() ?: 0 }

        val maxLength = maxOf(parts1.size, parts2.size)

        for (i in 0 until maxLength) {
            val part1 = parts1.getOrNull(i) ?: 0
            val part2 = parts2.getOrNull(i) ?: 0

            if (part1 != part2) {
                return part1.compareTo(part2)
            }
        }

        return 0
    }

    fun close() {
        httpClient.close()
    }
}
