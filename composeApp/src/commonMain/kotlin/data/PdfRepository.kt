package data

import domain.models.PdfDocument
import domain.models.ImageInfo
import domain.models.AdjustmentConfig

data class RenderedPage(
    val imageData: ByteArray,
    val width: Int,
    val height: Int
)

data class OutlineItem(
    val title: String,
    val pageNumber: Int,
    val children: List<OutlineItem> = emptyList()
)

interface PdfRepository {
    suspend fun loadPdf(filePath: String): PdfDocument
    suspend fun extractImages(pageIndex: Int): List<ImageInfo>
    suspend fun renderPage(pageIndex: Int, scale: Float = 1.5f): RenderedPage
    suspend fun replaceImage(imageInfo: ImageInfo, newImageData: ByteArray)
    suspend fun savePdf(outputPath: String): ByteArray
    fun close()

    /**
     * 获取指定页面的所有图片
     */
    suspend fun getPageImages(pageIndex: Int): List<ImageInfo>

    /**
     * 获取文档总页数
     */
    fun getTotalPages(): Int

    /**
     * 获取当前加载的PDF文件路径
     */
    fun getCurrentPdfPath(): String?

    /**
     * 获取PDF文档的目录大纲
     */
    suspend fun getOutline(): List<OutlineItem>
}