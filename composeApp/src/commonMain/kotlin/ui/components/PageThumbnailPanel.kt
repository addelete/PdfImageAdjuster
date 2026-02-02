package ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import data.PdfRepository
import kotlinx.coroutines.launch
import org.jetbrains.skia.Image as SkiaImage

@Composable
fun PageThumbnailPanel(
    totalPages: Int,
    currentPageNumber: Int,
    pdfRepository: PdfRepository,
    onPageClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    processedPageTrigger: Int = 0  // 页面处理触发器，每次页面被处理时递增
) {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // 缓存已加载的缩略图，使用totalPages作为key，当PDF改变时自动清空
    val thumbnailCache = remember { mutableStateMapOf<Int, ImageBitmap?>() }

    // 记录上次处理的页面集合
    val processedPages = remember { mutableStateSetOf<Int>() }

    // 监听 processedPageTrigger 变化，刷新已处理的页面缩略图
    LaunchedEffect(processedPageTrigger) {
        if (processedPageTrigger > 0 && processedPages.isNotEmpty()) {
            // 重新加载所有已处理页面的缩略图
            processedPages.forEach { pageNumber ->
                if (thumbnailCache.containsKey(pageNumber)) {
                    launch {
                        try {
                            val rendered = pdfRepository.renderPage(pageNumber - 1, 0.3f)
                            val skiaImage = SkiaImage.makeFromEncoded(
                                rgbaToImageBytes(rendered.imageData, rendered.width, rendered.height)
                            )
                            thumbnailCache[pageNumber] = skiaImage.toComposeImageBitmap()
                        } catch (e: Exception) {
                            println("Failed to reload thumbnail for page $pageNumber: ${e.message}")
                        }
                    }
                }
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(
            items = (1..totalPages).toList(),
            key = { _, pageNum -> pageNum }
        ) { _, pageNumber ->
            PageThumbnailItem(
                pageNumber = pageNumber,
                isSelected = pageNumber == currentPageNumber,
                thumbnail = thumbnailCache[pageNumber],
                onLoadThumbnail = {
                    if (!thumbnailCache.containsKey(pageNumber)) {
                        coroutineScope.launch {
                            try {
                                val rendered = pdfRepository.renderPage(pageNumber - 1, 0.3f)
                                val skiaImage = SkiaImage.makeFromEncoded(
                                    rgbaToImageBytes(rendered.imageData, rendered.width, rendered.height)
                                )
                                thumbnailCache[pageNumber] = skiaImage.toComposeImageBitmap()
                                // 记录该页面已加载缩略图
                                processedPages.add(pageNumber)
                            } catch (e: Exception) {
                                println("Failed to load thumbnail for page $pageNumber: ${e.message}")
                                thumbnailCache[pageNumber] = null
                            }
                        }
                    }
                },
                onClick = { onPageClick(pageNumber) }
            )
        }
    }
}

@Composable
private fun PageThumbnailItem(
    pageNumber: Int,
    isSelected: Boolean,
    thumbnail: ImageBitmap?,
    onLoadThumbnail: () -> Unit,
    onClick: () -> Unit
) {
    // 触发加载缩略图
    LaunchedEffect(pageNumber) {
        onLoadThumbnail()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clickable { onClick() }
            .then(
                if (isSelected) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary)
                } else {
                    Modifier
                }
            )
            .padding(top = 10.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RectangleShape
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when {
                    thumbnail != null -> {
                        Image(
                            bitmap = thumbnail,
                            contentDescription = "$pageNumber",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                    else -> {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
            }
            Text(
                text = "$pageNumber",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(4.dp)
            )
        }
    }
}

/**
 * 将RGBA字节数组转换为PNG图片字节数组
 */
private fun rgbaToImageBytes(rgbaData: ByteArray, width: Int, height: Int): ByteArray {
    // 使用 makeRaster 直接从 RGBA 数据创建图像
    val skiaImage = SkiaImage.makeRaster(
        imageInfo = org.jetbrains.skia.ImageInfo(
            width,
            height,
            org.jetbrains.skia.ColorType.RGBA_8888,
            org.jetbrains.skia.ColorAlphaType.UNPREMUL
        ),
        bytes = rgbaData,
        rowBytes = width * 4
    )
    return skiaImage.encodeToData()?.bytes ?: ByteArray(0)
}
