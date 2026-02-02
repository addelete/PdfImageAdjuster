package data

import data.PdfRepository
import domain.models.PdfDocument
import domain.models.PdfPage
import domain.models.ImageInfo
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.apache.pdfbox.rendering.PDFRenderer
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineNode
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import javax.imageio.ImageIO

class PdfRepositoryImpl : PdfRepository {
    private var document: PDDocument? = null
    private var renderer: PDFRenderer? = null
    private val processedImages = mutableMapOf<String, ByteArray>()
    private var currentPdfPath: String? = null

    // 添加互斥锁，保护 PDF 文档的并发访问
    private val documentMutex = Mutex()

    override suspend fun loadPdf(filePath: String): PdfDocument = withContext(Dispatchers.IO) {
        close() // 关闭之前的文档

        val file = File(filePath)
        document = Loader.loadPDF(file)
        renderer = PDFRenderer(document!!)
        currentPdfPath = filePath  // 保存当前PDF路径

        // 只返回基本信息，不预加载所有页面的图片
        PdfDocument(
            path = filePath,
            totalPages = document!!.numberOfPages,
            pages = emptyList() // 按需加载，不预加载所有页面
        )
    }

    override suspend fun extractImages(pageIndex: Int): List<ImageInfo> = withContext(Dispatchers.IO) {
        documentMutex.withLock {
            val doc = document ?: throw IllegalStateException("PDF document not loaded")
            val page = doc.getPage(pageIndex)
            val resources = page.resources
            val images = mutableListOf<ImageInfo>()

            var imageIndex = 0
            for (name in resources.xObjectNames) {
                if (resources.isImageXObject(name)) {
                    val pdImage = resources.getXObject(name) as PDImageXObject
                    val bufferedImage = pdImage.image

                    // 转换为RGBA字节数组
                    val rgbaData = bufferedImageToRGBA(bufferedImage)

                    images.add(
                        ImageInfo(
                            pageIndex = pageIndex + 1,
                            imageIndex = imageIndex++,
                            width = bufferedImage.width,
                            height = bufferedImage.height,
                            data = rgbaData,
                            resourceName = name.name
                        )
                    )
                }
            }

            images
        }
    }

    override suspend fun renderPage(pageIndex: Int, scale: Float): RenderedPage = withContext(Dispatchers.IO) {
        documentMutex.withLock {
            val doc = document ?: throw IllegalStateException("PDF document not loaded")
            val renderer = renderer ?: throw IllegalStateException("Renderer not initialized")

            val bufferedImage = renderer.renderImage(pageIndex, scale)
            val rgbaData = bufferedImageToRGBA(bufferedImage)

            RenderedPage(
                imageData = rgbaData,
                width = bufferedImage.width,
                height = bufferedImage.height
            )
        }
    }

    override suspend fun replaceImage(imageInfo: ImageInfo, newImageData: ByteArray) = withContext(Dispatchers.IO) {
        documentMutex.withLock {
            val doc = document ?: throw IllegalStateException("PDF document not loaded")
            val resourceName = imageInfo.resourceName ?: throw IllegalStateException("Resource name not found")

            // 将RGBA字节数组转换为BufferedImage
            val bufferedImage = rgbaToBufferedImage(newImageData, imageInfo.width, imageInfo.height)

            // 创建新的PDImageXObject
            val newPdImage = PDImageXObject.createFromByteArray(doc, bufferedImageToByteArray(bufferedImage), resourceName)

            // 替换页面中的图像资源
            val page = doc.getPage(imageInfo.pageIndex - 1)
            val resources = page.resources
            resources.put(org.apache.pdfbox.cos.COSName.getPDFName(resourceName), newPdImage)

            // 记录已处理的图像
            processedImages[resourceName] = newImageData
        }
    }

    override suspend fun savePdf(outputPath: String): ByteArray = withContext(Dispatchers.IO) {
        val doc = document ?: throw IllegalStateException("PDF document not loaded")

        val outputStream = ByteArrayOutputStream()
        doc.save(outputStream)
        val pdfBytes = outputStream.toByteArray()

        // 同时保存到文件
        FileOutputStream(outputPath).use { fileOut ->
            fileOut.write(pdfBytes)
        }

        pdfBytes
    }

    override fun close() {
        document?.close()
        document = null
        renderer = null
        processedImages.clear()
    }

    override suspend fun getPageImages(pageIndex: Int): List<ImageInfo> {
        return extractImages(pageIndex)
    }

    override fun getTotalPages() : Int {
        val doc = document ?: throw IllegalStateException("PDF document not loaded")
        return doc.numberOfPages
    }

    override fun getCurrentPdfPath(): String? {
        return currentPdfPath
    }

    override suspend fun getOutline(): List<OutlineItem> = withContext(Dispatchers.IO) {
        documentMutex.withLock {
            val doc = document ?: throw IllegalStateException("PDF document not loaded")
            val outline = doc.documentCatalog.documentOutline

            if (outline == null) {
                return@withLock emptyList()
            }

            buildOutlineTree(outline)
        }
    }

    private fun buildOutlineTree(outlineNode: PDOutlineNode): List<OutlineItem> {
        val items = mutableListOf<OutlineItem>()

        var current: PDOutlineItem? = outlineNode.firstChild
        while (current != null) {
            val pageNumber = try {
                // Try to get page number from the outline item
                val action = current.action
                if (action != null && action is PDActionGoTo) {
                    val dest = action.destination
                    if (dest is PDPageDestination) {
                        val pageNum = dest.retrievePageNumber()
                        if (pageNum >= 0) pageNum + 1 else 1
                    } else {
                        1
                    }
                } else {
                    1
                }
            } catch (e: Exception) {
                1
            }

            val children = buildOutlineTree(current)

            items.add(
                OutlineItem(
                    title = current.title ?: "Untitled",
                    pageNumber = pageNumber,
                    children = children
                )
            )

            current = current.nextSibling
        }

        return items
    }

    private fun bufferedImageToByteArray(image: BufferedImage): ByteArray {
        val outputStream = ByteArrayOutputStream()
        ImageIO.write(image, "PNG", outputStream)
        return outputStream.toByteArray()
    }

    private fun bufferedImageToRGBA(image: BufferedImage): ByteArray {
        val width = image.width
        val height = image.height
        val rgbaData = ByteArray(width * height * 4)

        var index = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                val argb = image.getRGB(x, y)
                val r = ((argb shr 16) and 0xFF).toByte()
                val g = ((argb shr 8) and 0xFF).toByte()
                val b = (argb and 0xFF).toByte()
                val a = ((argb shr 24) and 0xFF).toByte()

                rgbaData[index++] = r
                rgbaData[index++] = g
                rgbaData[index++] = b
                rgbaData[index++] = a
            }
        }

        return rgbaData
    }

    private fun rgbaToBufferedImage(rgbaData: ByteArray, width: Int, height: Int): BufferedImage {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)

        var index = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                val r = (rgbaData[index++].toInt() and 0xFF)
                val g = (rgbaData[index++].toInt() and 0xFF)
                val b = (rgbaData[index++].toInt() and 0xFF)
                val a = (rgbaData[index++].toInt() and 0xFF)

                val argb = (a shl 24) or (r shl 16) or (g shl 8) or b
                image.setRGB(x, y, argb)
            }
        }

        return image
    }
}