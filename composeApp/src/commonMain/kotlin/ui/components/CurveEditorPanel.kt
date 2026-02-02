package ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import domain.models.CurveConfig
import domain.models.CurvePoint

@Composable
fun CurveEditorPanel(
    curveConfig: CurveConfig,
    onCurveChange: (CurveConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedChannel by remember { mutableStateOf("RGB") }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 标题和重置按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "曲线",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                )

                CommonButton(
                    text = "重置",
                    type = CommonButtonType.TEXT,
                    onClick = {
                        onCurveChange(CurveConfig())
                    }
                )
            }

            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )

            // 曲线画布
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(4.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
//                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                CurveCanvas(
                    points = getCurrentChannelPoints(curveConfig, selectedChannel),
                    onPointsChange = { newPoints ->
                        val newConfig = updateChannelPoints(curveConfig, selectedChannel, newPoints)
                        onCurveChange(newConfig)
                    },
                    channelColor = getChannelColor(selectedChannel),
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 通道选择器（彩色圆圈）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChannelCircle(
                    color = Color.Gray,
                    isSelected = selectedChannel == "RGB",
                    onClick = { selectedChannel = "RGB" }
                )
                ChannelCircle(
                    color = Color.Red,
                    isSelected = selectedChannel == "R",
                    onClick = { selectedChannel = "R" }
                )
                ChannelCircle(
                    color = Color.Green,
                    isSelected = selectedChannel == "G",
                    onClick = { selectedChannel = "G" }
                )
                ChannelCircle(
                    color = Color.Blue,
                    isSelected = selectedChannel == "B",
                    onClick = { selectedChannel = "B" }
                )
            }
        }
    }
}

@Composable
private fun ChannelCircle(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // 外圆环
        Box(
            modifier = Modifier
                .size(20.dp)
                .border(3.dp, color, CircleShape)
        )

        // 选中时的内部圆点
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

private fun getCurrentChannelPoints(config: CurveConfig, channel: String): List<CurvePoint> {
    return when (channel) {
        "RGB" -> config.rgb
        "R" -> config.r
        "G" -> config.g
        "B" -> config.b
        else -> config.rgb
    }
}

private fun updateChannelPoints(config: CurveConfig, channel: String, points: List<CurvePoint>): CurveConfig {
    return when (channel) {
        "RGB" -> config.copy(rgb = points)
        "R" -> config.copy(r = points)
        "G" -> config.copy(g = points)
        "B" -> config.copy(b = points)
        else -> config.copy(rgb = points)
    }
}

private fun getChannelColor(channel: String): Color {
    return when (channel) {
        "RGB" -> Color.Gray
        "R" -> Color.Red
        "G" -> Color.Green
        "B" -> Color.Blue
        else -> Color.Gray
    }
}