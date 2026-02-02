package ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.isTertiaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import domain.models.AdjustmentConfig
import domain.models.ColorPreset
import domain.models.ImageInfo
import ui.components.ColorAdjusterPanel
import ui.components.CommonButton
import ui.components.CommonButtonType
import ui.components.CommonDialog
import ui.components.CurveEditorPanel
import ui.components.PresetManagerPanel
import org.jetbrains.skia.Image as SkiaImage

@Composable
fun AdjustmentDialog(
    imageInfo: ImageInfo,
    adjustmentConfig: AdjustmentConfig,
    presets: List<ColorPreset>,
    onConfigChange: (AdjustmentConfig) -> Unit,
    onLoadPreset: (ColorPreset) -> Unit,
    onSavePreset: (String) -> Unit,
    onDeletePreset: (String) -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
    windowWidth: Int = 0,
    windowHeight: Int = 0,
    originalImageData: ByteArray? = null  // 原始图片数据（从缓存获取）
) {
    val density = LocalDensity.current

    // 计算弹窗大小：窗口大小向内左右100，上下50
    val dialogWidth = if (windowWidth > 0) {
        with(density) { (windowWidth - 200).toDp() }
    } else 0.dp

    val dialogHeight = if (windowHeight > 0) {
        with(density) { (windowHeight - 100).toDp() }
    } else 0.dp

    val dialogModifier = if (windowWidth > 0 && windowHeight > 0) {
        Modifier.size(width = dialogWidth, height = dialogHeight)
    } else {
        Modifier.fillMaxSize(0.9f).aspectRatio(1.6f)
    }

    CommonDialog(
        title = "图片调整",
        onDismiss = onDismiss,
        modifier = dialogModifier,
        closable = true,
        contentPadding = PaddingValues(0.dp),
        okText = "应用",
        onOk = onApply,
    ) {
        // 主内容区 - 无内边距
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // 主内容区
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                // 左侧面板：图片查看器
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clipToBounds()
                ) {
                    ImageViewer(
                        imageInfo = imageInfo,
                        adjustmentConfig = adjustmentConfig,
                        originalImageData = originalImageData,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                // 右侧面板：调整工具
                Box(
                    modifier = Modifier
                        .width(300.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surface)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // HSL 调整器
                        ColorAdjusterPanel(
                            hslAdjustment = adjustmentConfig.hsl,
                            onHslChange = { newHsl ->
                                onConfigChange(adjustmentConfig.copy(hsl = newHsl))
                            }
                        )

                        HorizontalDivider(
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        )
                        // 曲线编辑器
                        CurveEditorPanel(
                            curveConfig = adjustmentConfig.curves,
                            onCurveChange = { newCurves ->
                                onConfigChange(adjustmentConfig.copy(curves = newCurves))
                            }
                        )
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        )
                        // 预设管理器
                        PresetManagerPanel(
                            presets = presets,
                            onLoadPreset = { preset ->
                                onLoadPreset(preset)
                            },
                            onSavePreset = { name ->
                                onSavePreset(name)
                            },
                            onDeletePreset = { id ->
                                onDeletePreset(id)
                            }
                        )
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        )
                    }
                }


            }
        }
    }
}

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
private fun ImageViewer(
    imageInfo: ImageInfo,
    adjustmentConfig: AdjustmentConfig,
    originalImageData: ByteArray? = null,  // 原始图片数据（从缓存获取）
    modifier: Modifier = Modifier
) {
    // 缩放和平移状态
    val initialScale = 0.9f
    var scale by remember { mutableStateOf(initialScale) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var isMiddleMouseDown by remember { mutableStateOf(false) }
    var showOriginal by remember { mutableStateOf(false) }
    var dragStart by remember { mutableStateOf(Offset.Zero) }
    var lastOffset by remember { mutableStateOf(Offset.Zero) }

    // 容器尺寸
    var containerWidth by remember { mutableStateOf(0) }
    var containerHeight by remember { mutableStateOf(0) }

    // 后台处理器
    val imageProcessor = remember { domain.processors.ImageProcessorWorker() }

    // 使用原始数据（如果提供），否则使用 imageInfo.data
    val imageDataToUse = originalImageData ?: imageInfo.data

    // 原始图片 Bitmap（用于显示原图）
    val originalBitmap = remember(imageDataToUse) {
        try {
            val skiaImage = SkiaImage.makeRaster(
                imageInfo = org.jetbrains.skia.ImageInfo(
                    imageInfo.width,
                    imageInfo.height,
                    org.jetbrains.skia.ColorType.RGBA_8888,
                    org.jetbrains.skia.ColorAlphaType.UNPREMUL
                ),
                bytes = imageDataToUse,
                rowBytes = imageInfo.width * 4
            )
            skiaImage.toComposeImageBitmap()
        } catch (e: Exception) {
            println("Original image conversion failed: ${e.message}")
            null
        }
    }

    // 调整后的图片数据和 Bitmap
    var adjustedImageData by remember { mutableStateOf<ByteArray?>(null) }
    var adjustedBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }

    // 初始化：缓存原始图像数据并立即处理
    LaunchedEffect(imageDataToUse) {
        imageProcessor.initImage(imageDataToUse, imageInfo.width, imageInfo.height)

        // 立即用当前的 adjustmentConfig 处理图片
        try {
            val result = imageProcessor.processWithCachedImage(adjustmentConfig)
            adjustedImageData = result.data

            // 转换为 ImageBitmap
            val skiaImage = SkiaImage.makeRaster(
                imageInfo = org.jetbrains.skia.ImageInfo(
                    result.width,
                    result.height,
                    org.jetbrains.skia.ColorType.RGBA_8888,
                    org.jetbrains.skia.ColorAlphaType.UNPREMUL
                ),
                bytes = result.data,
                rowBytes = result.width * 4
            )
            adjustedBitmap = skiaImage.toComposeImageBitmap()
        } catch (e: Exception) {
            println("Initial image processing failed: ${e.message}")
        }
    }

    // 当调整参数变化时，在后台处理图像
    LaunchedEffect(adjustmentConfig) {
        try {
            val result = imageProcessor.processWithCachedImage(adjustmentConfig)
            adjustedImageData = result.data

            // 转换为 ImageBitmap
            val skiaImage = SkiaImage.makeRaster(
                imageInfo = org.jetbrains.skia.ImageInfo(
                    result.width,
                    result.height,
                    org.jetbrains.skia.ColorType.RGBA_8888,
                    org.jetbrains.skia.ColorAlphaType.UNPREMUL
                ),
                bytes = result.data,
                rowBytes = result.width * 4
            )
            adjustedBitmap = skiaImage.toComposeImageBitmap()
        } catch (e: Exception) {
            println("Image processing failed: ${e.message}")
        }
    }

    // 销毁处理器
    DisposableEffect(Unit) {
        onDispose {
            imageProcessor.destroy()
        }
    }

    // 显示的图片：按住左键显示原图，否则显示调整后的图片
    val displayBitmap = if (showOriginal) originalBitmap else (adjustedBitmap ?: originalBitmap)

    val density = LocalDensity.current

    // 图片的像素尺寸（不需要转换，直接使用）
    val imageWidthPx = imageInfo.width.toFloat()
    val imageHeightPx = imageInfo.height.toFloat()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .onSizeChanged { size ->
                containerWidth = size.width
                containerHeight = size.height
            }
            .pointerInput(Unit) {
                // 监听滚轮缩放（根据鼠标位置）
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Scroll) {
                            val scrollDelta = event.changes.first().scrollDelta.y
                            val mousePos = event.changes.first().position

                            // 计算画布的实际渲染位置
                            val canvasWidth = imageWidthPx * scale
                            val canvasHeight = imageHeightPx * scale
                            val canvasLeft = (containerWidth - canvasWidth) / 2 + offset.x
                            val canvasTop = (containerHeight - canvasHeight) / 2 + offset.y

                            // 计算鼠标在画布中的相对位置（0-1）
                            val canvasX = (mousePos.x - canvasLeft) / canvasWidth
                            val canvasY = (mousePos.y - canvasTop) / canvasHeight

                            // 计算新的缩放比例
                            val delta = if (scrollDelta > 0) 0.9f else 1.1f
                            val newScale = (scale * delta).coerceIn(0.1f, 10f)

                            // 计算新的画布尺寸
                            val newCanvasWidth = imageWidthPx * newScale
                            val newCanvasHeight = imageHeightPx * newScale

                            // 计算新的画布左上角位置，保持鼠标指向的点不变
                            val newCanvasLeft = mousePos.x - canvasX * newCanvasWidth
                            val newCanvasTop = mousePos.y - canvasY * newCanvasHeight

                            // 更新偏移量
                            offset = Offset(
                                newCanvasLeft - (containerWidth - newCanvasWidth) / 2,
                                newCanvasTop - (containerHeight - newCanvasHeight) / 2
                            )
                            scale = newScale
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                // 监听鼠标按键事件和移动
                var lastClickTime = 0L
                val doubleClickThreshold = 300L // 300ms内的两次点击视为双击

                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        when (event.type) {
                            PointerEventType.Press -> {
                                val currentTime = System.currentTimeMillis()

                                // 检查按下的按钮类型
                                if (event.buttons.isPrimaryPressed) {
                                    // 检测双击
                                    if (currentTime - lastClickTime < doubleClickThreshold) {
                                        // 双击：恢复初始状态
                                        scale = initialScale
                                        offset = Offset.Zero
                                        lastClickTime = 0L // 重置，避免三击被识别为双击
                                    } else {
                                        // 单击：显示原图
                                        showOriginal = true
                                        lastClickTime = currentTime
                                    }
                                } else if (event.buttons.isSecondaryPressed) {
                                    // 右键（如果需要）
                                } else if (event.buttons.isTertiaryPressed) {
                                    // 中键：开始拖动
                                    isMiddleMouseDown = true
                                    dragStart = event.changes.first().position
                                    lastOffset = offset
                                }
                            }
                            PointerEventType.Move -> {
                                // 鼠标移动时，如果中键按下，则拖动
                                if (isMiddleMouseDown) {
                                    val currentPos = event.changes.first().position
                                    val dragAmount = currentPos - dragStart
                                    offset = lastOffset + dragAmount
                                }
                            }
                            PointerEventType.Release -> {
                                // 检查释放的按钮
                                if (!event.buttons.isPrimaryPressed && showOriginal) {
                                    showOriginal = false
                                }
                                if (!event.buttons.isTertiaryPressed && isMiddleMouseDown) {
                                    // 中键释放：结束拖动
                                    isMiddleMouseDown = false
                                }
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (displayBitmap != null) {
            // 显示图片 - 直接使用像素尺寸
            Box(
                modifier = Modifier
                    .size(
                        width = imageWidthPx.toInt().dp,
                        height = imageHeightPx.toInt().dp
                    )
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
            ) {
                Image(
                    bitmap = displayBitmap,
                    contentDescription = "Image preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        } else {
            // 加载失败时显示占位符
            Text(
                text = "图片加载失败",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 底部提示信息
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "鼠标滚轮缩放 | 鼠标中键拖动 | 双击恢复缩放与位置 | 鼠标左键按住显示原图",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
