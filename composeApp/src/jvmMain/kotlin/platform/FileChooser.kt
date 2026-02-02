package platform

import data.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

actual class FileChooser {
    actual suspend fun chooseFile(
        title: String,
        allowedExtensions: List<String>
    ): String? = withContext(Dispatchers.IO) {
        val fileChooser = JFileChooser().apply {
            dialogTitle = title
            fileSelectionMode = JFileChooser.FILES_ONLY

            // 设置初始目录为最后打开的目录
            val lastDir = AppPreferences.getLastDirectory()
            if (lastDir != null) {
                val dir = File(lastDir)
                if (dir.exists() && dir.isDirectory) {
                    currentDirectory = dir
                }
            }

            if (allowedExtensions.isNotEmpty()) {
                val filter = FileNameExtensionFilter(
                    "PDF 文件 (*.pdf)",
                    *allowedExtensions.toTypedArray()
                )
                fileFilter = filter
            }
        }

        val result = fileChooser.showOpenDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            val selectedFile = fileChooser.selectedFile
            // 保存选中文件的目录
            AppPreferences.setLastDirectory(selectedFile.parent)
            selectedFile.absolutePath
        } else {
            null
        }
    }
}