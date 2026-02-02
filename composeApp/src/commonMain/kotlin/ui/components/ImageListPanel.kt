package ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import domain.models.ImageInfo
import org.jetbrains.skia.Image as SkiaImage

@Composable
fun ImageListPanel(
    images: List<ImageInfo>,
    onImageSelected: (ImageInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // 标题
            Text(
                text = "页面图片",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // 图片列表
            if (images.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "当前页面没有图片",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(images) { imageInfo ->
                        ImageListItem(
                            imageInfo = imageInfo,
                            onClick = { onImageSelected(imageInfo) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ImageListItem(
    imageInfo: ImageInfo,
    onClick: () -> Unit
) {
    // 将RGBA字节数组转换为ImageBitmap
    val imageBitmap = remember(imageInfo.data) {
        try {
            // 打印前几个字节用于调试
            if (imageInfo.data.size >= 4) {
                val r = imageInfo.data[0].toInt() and 0xFF
                val g = imageInfo.data[1].toInt() and 0xFF
                val b = imageInfo.data[2].toInt() and 0xFF
                val a = imageInfo.data[3].toInt() and 0xFF
            }

            // 显式使用 RGBA 颜色类型
            val skiaImage = SkiaImage.makeRaster(
                imageInfo = org.jetbrains.skia.ImageInfo(
                    imageInfo.width,
                    imageInfo.height,
                    org.jetbrains.skia.ColorType.RGBA_8888,
                    org.jetbrains.skia.ColorAlphaType.UNPREMUL
                ),
                bytes = imageInfo.data,
                rowBytes = imageInfo.width * 4
            )
            skiaImage.toComposeImageBitmap()
        } catch (e: Exception) {
            println("Image conversion failed: ${e.message}")
            null
        }
    }

    // 计算宽高比
    val aspectRatio = if (imageInfo.height > 0) {
        imageInfo.width.toFloat() / imageInfo.height.toFloat()
    } else {
        1f
    }

    // 判断是否在0.5到2之间
    val useOriginalAspectRatio = aspectRatio in 0.5f..2f

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (useOriginalAspectRatio) {
                    Modifier.aspectRatio(aspectRatio)
                } else {
                    Modifier.aspectRatio(1f)
                }
            )
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (imageBitmap != null) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = "图片 #${imageInfo.imageIndex + 1}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else {
                Text(
                    text = "#${imageInfo.imageIndex + 1}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
