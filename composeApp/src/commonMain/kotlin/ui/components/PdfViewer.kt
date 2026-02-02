package ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.dp
import org.jetbrains.skia.Image
import org.jetbrains.skia.Image.Companion.makeFromEncoded
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun PdfViewer(
    pdfImageData: ByteArray?,
    width: Int,
    height: Int,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier,
    onNextPage: () -> Unit = {},
    onPrevPage: () -> Unit = {},
    onOpenFile: () -> Unit = {}
) {
    // 防抖动：记录上次翻页时间
    var lastScrollTime by remember { mutableStateOf(0L) }
    val scrollDebounceMs = 300L // 300毫秒内只能翻页一次

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.surfaceVariant)
            .padding(20.dp)
            .onPointerEvent(PointerEventType.Scroll) { event ->
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastScrollTime > scrollDebounceMs) {
                    val scrollDelta = event.changes.first().scrollDelta.y
                    if (scrollDelta > 0) {
                        onNextPage()
                        lastScrollTime = currentTime
                    } else if (scrollDelta < 0) {
                        onPrevPage()
                        lastScrollTime = currentTime
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                // 显示加载指示器
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            pdfImageData != null -> {
                // 将 RGBA 字节数组转换为 Compose ImageBitmap
                val imageBitmap = remember(pdfImageData, width, height) {
                    rgbaToImageBitmap(pdfImageData, width, height)
                }

                Image(
                    bitmap = imageBitmap,
                    contentDescription = "PDF Page",
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "请先",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )

                    CommonButton(
                        text = "打开",
                        onClick = onOpenFile,
                        type = CommonButtonType.SOLID
                    )

                    Text(
                        text = "PDF文件",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

private fun rgbaToImageBitmap(rgbaData: ByteArray, width: Int, height: Int): androidx.compose.ui.graphics.ImageBitmap {
    // 创建 BufferedImage
    val bufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)

    var index = 0
    for (y in 0 until height) {
        for (x in 0 until width) {
            val r = (rgbaData[index++].toInt() and 0xFF)
            val g = (rgbaData[index++].toInt() and 0xFF)
            val b = (rgbaData[index++].toInt() and 0xFF)
            val a = (rgbaData[index++].toInt() and 0xFF)

            val argb = (a shl 24) or (r shl 16) or (g shl 8) or b
            bufferedImage.setRGB(x, y, argb)
        }
    }

    // 转换为 PNG 字节数组
    val outputStream = ByteArrayOutputStream()
    ImageIO.write(bufferedImage, "PNG", outputStream)
    val pngBytes = outputStream.toByteArray()

    // 使用 Skia 创建 ImageBitmap
    val skiaImage = makeFromEncoded(pngBytes)
    return skiaImage.toComposeImageBitmap()
}
