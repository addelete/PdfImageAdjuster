package domain.models

data class PdfDocument(
    val path: String,
    val totalPages: Int,
    val pages: List<PdfPage>
)

data class PdfPage(
    val pageNumber: Int,
    val images: List<ImageInfo>
)

data class ImageInfo(
    val pageIndex: Int,
    val imageIndex: Int,
    val width: Int,
    val height: Int,
    val data: ByteArray,
    val resourceName: String? = null  // PDFBox 资源名称，如 "Im1"
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ImageInfo

        if (pageIndex != other.pageIndex) return false
        if (imageIndex != other.imageIndex) return false
        if (width != other.width) return false
        if (height != other.height) return false
        if (!data.contentEquals(other.data)) return false
        if (resourceName != other.resourceName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = pageIndex
        result = 31 * result + imageIndex
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + data.contentHashCode()
        result = 31 * result + (resourceName?.hashCode() ?: 0)
        return result
    }
}