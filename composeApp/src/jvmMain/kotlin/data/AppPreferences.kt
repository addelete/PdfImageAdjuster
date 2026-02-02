package data

import java.io.File
import java.util.Properties

object AppPreferences {
    private const val PREFS_FILE = "app.properties"
    private const val KEY_LAST_DIRECTORY = "last.directory"

    private val prefsFile: File by lazy {
        val userHome = System.getProperty("user.home")
        val appDir = File(userHome, ".pdfimageadjuster")
        if (!appDir.exists()) {
            appDir.mkdirs()
        }
        File(appDir, PREFS_FILE)
    }

    private val properties: Properties by lazy {
        Properties().apply {
            if (prefsFile.exists()) {
                prefsFile.inputStream().use { load(it) }
            }
        }
    }

    fun getLastDirectory(): String? {
        return properties.getProperty(KEY_LAST_DIRECTORY)
    }

    fun setLastDirectory(directory: String) {
        properties.setProperty(KEY_LAST_DIRECTORY, directory)
        saveProperties()
    }

    private fun saveProperties() {
        try {
            prefsFile.outputStream().use {
                properties.store(it, "PDF Image Adjuster Preferences")
            }
        } catch (e: Exception) {
            println("Failed to save preferences: ${e.message}")
        }
    }
}
