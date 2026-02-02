package domain

import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 应用日志记录器
 * 日志文件保存在用户的 AppData 目录中
 */
object Logger {
    private var logFile: File? = null
    private var writer: PrintWriter? = null
    private val mutex = Mutex()
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

    enum class Level {
        DEBUG, INFO, WARN, ERROR
    }

    /**
     * 初始化日志系统
     * 如果日志文件已存在，将被覆盖
     */
    fun initialize() {
        try {
            val appDataDir = getAppDataDirectory()
            val logDir = File(appDataDir, "PdfImageAdjuster")

            // 确保日志目录存在
            if (!logDir.exists()) {
                logDir.mkdirs()
            }

            // 创建日志文件（覆盖模式）
            logFile = File(logDir, "app.log")
            writer = PrintWriter(FileWriter(logFile, false), true)

            // 记录启动信息
            info("=".repeat(80))
            info("Application started at ${getCurrentTimestamp()}")
            info("Log file: ${logFile?.absolutePath}")
            info("=".repeat(80))
        } catch (e: Exception) {
            System.err.println("Failed to initialize logger: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * 关闭日志系统
     */
    fun close() {
        try {
            info("=".repeat(80))
            info("Application stopped at ${getCurrentTimestamp()}")
            info("=".repeat(80))
            writer?.close()
            writer = null
        } catch (e: Exception) {
            System.err.println("Failed to close logger: ${e.message}")
        }
    }

    /**
     * 获取 AppData 目录
     */
    private fun getAppDataDirectory(): String {
        val os = System.getProperty("os.name").lowercase()
        return when {
            os.contains("win") -> {
                System.getenv("APPDATA") ?: System.getProperty("user.home")
            }
            os.contains("mac") -> {
                "${System.getProperty("user.home")}/Library/Application Support"
            }
            else -> {
                "${System.getProperty("user.home")}/.local/share"
            }
        }
    }

    /**
     * 获取当前时间戳
     */
    private fun getCurrentTimestamp(): String {
        return LocalDateTime.now().format(dateTimeFormatter)
    }

    /**
     * 写入日志
     */
    private fun log(level: Level, message: String, throwable: Throwable? = null) {
        try {
            val timestamp = getCurrentTimestamp()
            val logMessage = "[$timestamp] [${level.name}] $message"

            // 写入文件
            writer?.println(logMessage)

            // 同时输出到控制台
            println(logMessage)

            // 如果有异常，记录堆栈跟踪
            if (throwable != null) {
                writer?.println(throwable.stackTraceToString())
                throwable.printStackTrace()
            }
        } catch (e: Exception) {
            System.err.println("Failed to write log: ${e.message}")
        }
    }

    /**
     * 记录 DEBUG 级别日志
     */
    fun debug(message: String) {
        log(Level.DEBUG, message)
    }

    /**
     * 记录 INFO 级别日志
     */
    fun info(message: String) {
        log(Level.INFO, message)
    }

    /**
     * 记录 WARN 级别日志
     */
    fun warn(message: String, throwable: Throwable? = null) {
        log(Level.WARN, message, throwable)
    }

    /**
     * 记录 ERROR 级别日志
     */
    fun error(message: String, throwable: Throwable? = null) {
        log(Level.ERROR, message, throwable)
    }

    /**
     * 获取日志文件路径
     */
    fun getLogFilePath(): String? {
        return logFile?.absolutePath
    }
}
