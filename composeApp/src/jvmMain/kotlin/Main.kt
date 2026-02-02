import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import ui.screens.MainScreen
import data.PdfRepositoryImpl
import domain.VersionChecker
import domain.Logger
import domain.models.UpdateCheckResult
import ui.components.UpdateDialog
import kotlinx.coroutines.launch
import java.awt.Desktop
import java.net.URI

fun main() = application {
    // 初始化日志系统
    Logger.initialize()
    Logger.info("Application main() started")
    val windowState = rememberWindowState(placement = WindowPlacement.Maximized)
    val versionChecker = remember { VersionChecker() }
    var updateResult by remember { mutableStateOf<UpdateCheckResult.UpdateAvailable?>(null) }
    val scope = rememberCoroutineScope()

    // 启动时检查更新
    LaunchedEffect(Unit) {
        scope.launch {
            Logger.info("Checking for updates...")
            when (val result = versionChecker.checkForUpdates()) {
                is UpdateCheckResult.UpdateAvailable -> {
                    Logger.info("Update available: ${result.latestVersion}")
                    updateResult = result
                }
                is UpdateCheckResult.NoUpdate -> {
                    Logger.info("Current version is up to date: ${result.currentVersion}")
                }
                is UpdateCheckResult.Error -> {
                    Logger.error("Failed to check for updates: ${result.message}")
                }
            }
        }
    }

    Window(
        onCloseRequest = {
            Logger.info("Application closing...")
            Logger.close()
            exitApplication()
        },
        title = "PDF 颜色调整器",
        state = windowState
    ) {
        MainScreen(
            pdfRepository = PdfRepositoryImpl()
        )

        // 显示更新对话框
        updateResult?.let { result ->
            UpdateDialog(
                updateResult = result,
                onDismiss = { updateResult = null },
                onDownload = { url ->
                    scope.launch {
                        try {
                            Logger.info("Opening download URL: $url")
                            // 直接打开下载链接，让用户下载并安装
                            if (Desktop.isDesktopSupported()) {
                                Desktop.getDesktop().browse(URI(url))
                            }
                        } catch (e: Exception) {
                            Logger.error("Failed to open download URL", e)
                        }
                        updateResult = null
                    }
                }
            )
        }
    }
}