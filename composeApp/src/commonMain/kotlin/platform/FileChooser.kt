package platform

expect class FileChooser() {
    suspend fun chooseFile(
        title: String = "选择文件",
        allowedExtensions: List<String> = emptyList()
    ): String?
}