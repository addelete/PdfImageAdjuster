package ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import domain.models.HslAdjustment

@Composable
fun ColorAdjusterPanel(
    hslAdjustment: HslAdjustment,
    onHslChange: (HslAdjustment) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 标题和重置按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "颜色",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                )

                CommonButton(
                    text = "重置",
                    type = CommonButtonType.TEXT,
                    onClick = {
                        onHslChange(HslAdjustment())
                    }
                )
            }

            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )

            // 色相滑动条
            SliderWithLabel(
                label = "色相",
                value = hslAdjustment.hue,
                valueRange = -180f..180f,
                onValueChange = { newValue ->
                    onHslChange(hslAdjustment.copy(hue = newValue))
                },
                valueFormatter = { "${it.toInt()}°" }
            )

            // 饱和度滑动条
            SliderWithLabel(
                label = "饱和度",
                value = hslAdjustment.saturation,
                valueRange = -100f..100f,
                onValueChange = { newValue ->
                    onHslChange(hslAdjustment.copy(saturation = newValue))
                },
                valueFormatter = { "${it.toInt()}%" }
            )

            // 明度滑动条
            SliderWithLabel(
                label = "明度",
                value = hslAdjustment.lightness,
                valueRange = -100f..100f,
                onValueChange = { newValue ->
                    onHslChange(hslAdjustment.copy(lightness = newValue))
                },
                valueFormatter = { "${it.toInt()}%" }
            )
        }
    }
}