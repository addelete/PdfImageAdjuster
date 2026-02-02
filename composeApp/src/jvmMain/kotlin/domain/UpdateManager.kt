package domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * 自动更新管理器
 * 负责下载和替换 JAR 文件
 */
object UpdateManager {

    /**
     * 下载更新的 JAR 文件
     * @param downloadUrl JAR 文件的下载 URL
     * @param version 版本号
     * @return 下载的临时文件路径，如果失败返回 null
     */
    suspend fun downloadUpdate(downloadUrl: String, version: String): File? = withContext(Dispatchers.IO) {
        try {
            Logger.info("Starting download from: $downloadUrl")

            val url = URI(downloadUrl).toURL()
            val connection = url.openConnection()
            connection.connectTimeout = 30000
            connection.readTimeout = 30000

            // 创建临时文件
            val tempDir = File(System.getProperty("java.io.tmpdir"))
            val tempFile = File(tempDir, "PdfImageAdjuster-$version-update.jar")

            // 下载文件
            connection.getInputStream().use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalBytes = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalBytes += bytesRead
                    }

                    Logger.info("Downloaded $totalBytes bytes to ${tempFile.absolutePath}")
                }
            }

            tempFile
        } catch (e: Exception) {
            Logger.error("Failed to download update", e)
            null
        }
    }

    /**
     * 获取当前运行的 JAR 文件路径
     */
    fun getCurrentJarPath(): File? {
        return try {
            val jarPath = UpdateManager::class.java.protectionDomain.codeSource.location.toURI().path
            val file = File(jarPath)
            if (file.exists() && file.extension == "jar") {
                Logger.info("Current JAR path: ${file.absolutePath}")
                file
            } else {
                Logger.warn("Not running from JAR file: ${file.absolutePath}")
                null
            }
        } catch (e: Exception) {
            Logger.error("Failed to get current JAR path", e)
            null
        }
    }

    /**
     * 应用更新：创建更新脚本并重启应用
     * @param newJarFile 新的 JAR 文件
     * @return 是否成功创建更新脚本
     */
    fun applyUpdate(newJarFile: File): Boolean {
        return try {
            val currentJar = getCurrentJarPath()
            if (currentJar == null) {
                Logger.error("Cannot apply update: not running from JAR")
                return false
            }

            Logger.info("Applying update: ${newJarFile.absolutePath} -> ${currentJar.absolutePath}")

            // 创建更新脚本
            val updateScript = createUpdateScript(currentJar, newJarFile)
            if (updateScript == null) {
                Logger.error("Failed to create update script")
                return false
            }

            // 执行更新脚本
            executeUpdateScript(updateScript)
            true
        } catch (e: Exception) {
            Logger.error("Failed to apply update", e)
            false
        }
    }

    /**
     * 创建更新脚本
     */
    private fun createUpdateScript(currentJar: File, newJar: File): File? {
        return try {
            val os = System.getProperty("os.name").lowercase()
            val scriptFile = when {
                os.contains("win") -> createWindowsUpdateScript(currentJar, newJar)
                os.contains("mac") || os.contains("nix") || os.contains("nux") ->
                    createUnixUpdateScript(currentJar, newJar)
                else -> null
            }
            scriptFile
        } catch (e: Exception) {
            Logger.error("Failed to create update script", e)
            null
        }
    }

    private fun createWindowsUpdateScript(currentJar: File, newJar: File): File {
        val scriptFile = File(System.getProperty("java.io.tmpdir"), "update.bat")
        val javaPath = System.getProperty("java.home") + "\\bin\\java.exe"

        scriptFile.writeText("""
            @echo off
            echo Waiting for application to close...
            timeout /t 2 /nobreak > nul
            echo Replacing JAR file...
            copy /Y "${newJar.absolutePath}" "${currentJar.absolutePath}"
            echo Starting updated application...
            start "" "$javaPath" -jar "${currentJar.absolutePath}"
            del "%~f0"
        """.trimIndent())

        Logger.info("Created Windows update script: ${scriptFile.absolutePath}")
        return scriptFile
    }

    private fun createUnixUpdateScript(currentJar: File, newJar: File): File {
        val scriptFile = File(System.getProperty("java.io.tmpdir"), "update.sh")
        val javaPath = System.getProperty("java.home") + "/bin/java"

        scriptFile.writeText("""
            #!/bin/bash
            echo "Waiting for application to close..."
            sleep 2
            echo "Replacing JAR file..."
            cp -f "${newJar.absolutePath}" "${currentJar.absolutePath}"
            echo "Starting updated application..."
            "$javaPath" -jar "${currentJar.absolutePath}" &
            rm -f "$0"
        """.trimIndent())

        scriptFile.setExecutable(true)
        Logger.info("Created Unix update script: ${scriptFile.absolutePath}")
        return scriptFile
    }

    private fun executeUpdateScript(scriptFile: File) {
        try {
            val os = System.getProperty("os.name").lowercase()
            val command = when {
                os.contains("win") -> listOf("cmd", "/c", scriptFile.absolutePath)
                else -> listOf("sh", scriptFile.absolutePath)
            }

            Logger.info("Executing update script: ${scriptFile.absolutePath}")
            ProcessBuilder(command).start()
        } catch (e: Exception) {
            Logger.error("Failed to execute update script", e)
        }
    }
}
