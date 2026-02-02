package domain.models

sealed class UpdateCheckResult {
    data class UpdateAvailable(
        val currentVersion: String,
        val latestVersion: String,
        val downloadUrl: String,
        val releaseNotes: String
    ) : UpdateCheckResult()

    data class NoUpdate(val currentVersion: String) : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}
