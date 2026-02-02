package ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.input.key.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import domain.models.CurvePoint
import domain.processors.CurveProcessor

@Composable
fun CurveCanvas(
    points: List<CurvePoint>,
    onPointsChange: (List<CurvePoint>) -> Unit,
    channelColor: Color,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var selectedPointIndex by remember { mutableStateOf(-1) }
    var hoveredPointIndex by remember { mutableStateOf(-1) }
    val focusRequester = remember { FocusRequester() }

    // 生成 LUT 用于绘制平滑曲线
    val lut = remember(points) {
        val processor = CurveProcessor()
        processor.generateLUT(points)
    }

    // 请求焦点以接收键盘事件
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Canvas(
        modifier = modifier
            .focusRequester(focusRequester)
            .focusTarget()
            .onKeyEvent { event ->
                // 处理 Delete 键删除选中的点
                if (event.type == KeyEventType.KeyDown &&
                    (event.key == Key.Delete || event.key == Key.Backspace)) {
                    if (selectedPointIndex != -1) {
                        deletePoint(points, selectedPointIndex, onPointsChange)
                        selectedPointIndex = -1
                        return@onKeyEvent true
                    }
                }
                false
            }
            .pointerInput(points) {
                // 处理拖动、右键删除和创建新点
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        when (event.type) {
                            PointerEventType.Press -> {
                                val change = event.changes.first()
                                val offset = change.position
                                val canvasWidth = size.width.toFloat()
                                val canvasHeight = size.height.toFloat()

                                val nearestIndex = findNearestPoint(points, offset, canvasWidth, canvasHeight)

                                // 检查是否是右键点击
                                if (event.buttons.isSecondaryPressed) {
                                    // 右键删除点
                                    if (nearestIndex != -1) {
                                        deletePoint(points, nearestIndex, onPointsChange)
                                        selectedPointIndex = -1
                                    }
                                } else if (nearestIndex != -1) {
                                    // 左键点击已有点：选中并准备拖动
                                    selectedPointIndex = nearestIndex
                                    isDragging = true
                                } else {
                                    // 左键点击空白处：立即创建新点
                                    val x = (offset.x / canvasWidth).coerceIn(0f, 1f)
                                    val y = (1f - offset.y / canvasHeight).coerceIn(0f, 1f)

                                    val newPoints = (points + CurvePoint(x, y)).sortedBy { it.x }
                                    onPointsChange(newPoints)

                                    // 找到新添加点的索引并选中，准备拖动
                                    selectedPointIndex = newPoints.indexOfFirst {
                                        kotlin.math.abs(it.x - x) < 0.001f && kotlin.math.abs(it.y - y) < 0.001f
                                    }
                                    isDragging = true
                                }
                            }
                            PointerEventType.Move -> {
                                if (isDragging && selectedPointIndex != -1) {
                                    val offset = event.changes.first().position
                                    val canvasWidth = size.width.toFloat()
                                    val canvasHeight = size.height.toFloat()

                                    handleDrag(points, selectedPointIndex, offset, canvasWidth, canvasHeight, onPointsChange)
                                }
                            }
                            PointerEventType.Release -> {
                                isDragging = false
                            }
                        }
                    }
                }
            }
    ) {
        // 绘制网格
        drawGrid()

        // 绘制对角线（线性参考）
        drawDiagonalLine()

        // 绘制曲线（使用 LUT）
        drawCurveWithLUT(lut, channelColor)

        // 绘制控制点
        drawControlPoints(points, channelColor, selectedPointIndex)
    }
}

// 查找最近的控制点
private fun findNearestPoint(
    points: List<CurvePoint>,
    offset: Offset,
    canvasWidth: Float,
    canvasHeight: Float
): Int {
    val threshold = 20f // 点击阈值（像素）

    points.forEachIndexed { index, point ->
        val screenX = point.x * canvasWidth
        val screenY = (1f - point.y) * canvasHeight

        val distance = kotlin.math.sqrt(
            (offset.x - screenX) * (offset.x - screenX) +
                    (offset.y - screenY) * (offset.y - screenY)
        )

        if (distance <= threshold) {
            return index
        }
    }

    return -1
}

// 删除控制点
private fun deletePoint(
    points: List<CurvePoint>,
    index: Int,
    onPointsChange: (List<CurvePoint>) -> Unit
) {
    // 不能删除端点
    if (index == 0 || index == points.size - 1) {
        return
    }

    // 至少保留2个点
    if (points.size <= 2) {
        return
    }

    val newPoints = points.toMutableList()
    newPoints.removeAt(index)
    onPointsChange(newPoints)
}

// 处理拖动
private fun handleDrag(
    points: List<CurvePoint>,
    index: Int,
    offset: Offset,
    canvasWidth: Float,
    canvasHeight: Float,
    onPointsChange: (List<CurvePoint>) -> Unit
) {
    var x = (offset.x / canvasWidth).coerceIn(0f, 1f)
    val y = (1f - offset.y / canvasHeight).coerceIn(0f, 1f)

    // 端点不能水平移动
    if (index == 0) {
        x = 0f
    } else if (index == points.size - 1) {
        x = 1f
    } else {
        // 中间点约束在相邻点之间
        val prevPoint = points.getOrNull(index - 1)
        val nextPoint = points.getOrNull(index + 1)

        if (prevPoint != null) x = x.coerceAtLeast(prevPoint.x + 0.01f)
        if (nextPoint != null) x = x.coerceAtMost(nextPoint.x - 0.01f)
    }

    val newPoints = points.toMutableList()
    newPoints[index] = CurvePoint(x, y)
    onPointsChange(newPoints)
}

// 绘制网格
private fun DrawScope.drawGrid() {
    val gridColor = Color.Gray.copy(alpha = 0.2f)
    val strokeWidth = 1f

    // 绘制垂直网格线（4等分）
    for (i in 1..3) {
        val x = size.width * i / 4f
        drawLine(
            color = gridColor,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = strokeWidth
        )
    }

    // 绘制水平网格线（4等分）
    for (i in 1..3) {
        val y = size.height * i / 4f
        drawLine(
            color = gridColor,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = strokeWidth
        )
    }
}

// 绘制对角线（线性参考线）
private fun DrawScope.drawDiagonalLine() {
    drawLine(
        color = Color.Gray.copy(alpha = 0.3f),
        start = Offset(0f, size.height),
        end = Offset(size.width, 0f),
        strokeWidth = 1f
    )
}

// 使用 LUT 绘制平滑曲线
private fun DrawScope.drawCurveWithLUT(lut: IntArray, color: Color) {
    if (lut.isEmpty()) return

    val path = Path()

    // 从第一个点开始
    val firstY = (1f - lut[0] / 255f) * size.height
    path.moveTo(0f, firstY)

    // 绘制曲线的每个点
    for (i in 1 until minOf(lut.size, 256)) {
        val x = (i / 255f) * size.width
        val y = (1f - lut[i] / 255f) * size.height
        path.lineTo(x, y)
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(width = 3f)  // 加粗曲线：从 2f 改为 3f
    )
}

// 绘制控制点
private fun DrawScope.drawControlPoints(
    points: List<CurvePoint>,
    color: Color,
    selectedIndex: Int
) {
    points.forEachIndexed { index, point ->
        val screenX = point.x * size.width
        val screenY = (1f - point.y) * size.height

        // 加大控制点：选中的点从 7f 改为 10f，普通点从 5f 改为 7f
        val radius = if (index == selectedIndex) 10f else 7f
        val pointColor = if (index == selectedIndex) color else color.copy(alpha = 0.8f)

        // 绘制点
        drawCircle(
            color = pointColor,
            radius = radius,
            center = Offset(screenX, screenY)
        )

        // 绘制白色边框
        drawCircle(
            color = Color.White,
            radius = radius,
            center = Offset(screenX, screenY),
            style = Stroke(width = 2f)
        )
    }
}