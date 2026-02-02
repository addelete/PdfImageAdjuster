package ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 按钮类型枚举
 */
enum class CommonButtonType {
    SOLID,      // 实心按钮
    OUTLINE,    // 边框按钮
    DASHED,     // 虚线边框按钮
    TEXT        // 文本按钮
}

/**
 * 通用按钮组件 - antd风格
 *
 * @param onClick 点击事件
 * @param modifier 修饰符
 * @param type 按钮类型：solid(实心)、outline(边框)、dashed(虚线)、text(文本)
 * @param text 按钮文本，如果为null则只显示图标
 * @param icon 按钮图标
 * @param enabled 是否启用
 */
@Composable
fun CommonButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    type: CommonButtonType = CommonButtonType.SOLID,
    text: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()

    // 小圆角
    val shape = RoundedCornerShape(4.dp)

    // 根据是否只有图标来决定按钮形状
    val isIconOnly = icon != null && text == null

    // 按钮内容
    val buttonContent: @Composable RowScope.() -> Unit = {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = if (isIconOnly) 0.dp else 4.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = text ?: "icon",
                    modifier = Modifier.size(16.dp)
                )
            }
            if (text != null && icon != null) {
                Spacer(modifier = Modifier.width(6.dp))
            }
            if (text != null) {
                Text(
                    text = text,
                    fontSize = 12.sp
                )
            }
        }
    }

    when (type) {
        CommonButtonType.SOLID -> {
            Button(
                onClick = onClick,
                modifier = modifier.then(
                    if (isIconOnly) Modifier.size(28.dp) else Modifier.height(28.dp)
                ),
                enabled = enabled,
                shape = shape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                ),
                contentPadding = PaddingValues(horizontal = if (isIconOnly) 0.dp else 4.dp),
                interactionSource = interactionSource,
                content = buttonContent
            )
        }

        CommonButtonType.OUTLINE -> {
            OutlinedButton(
                onClick = onClick,
                modifier = modifier.then(
                    if (isIconOnly) Modifier.size(28.dp) else Modifier.height(28.dp)
                ),
                enabled = enabled,
                shape = shape,
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = Color.Transparent,
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (enabled) {
                        when {
                            isPressed -> MaterialTheme.colorScheme.primary
                            isHovered -> MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                            else -> MaterialTheme.colorScheme.outline
                        }
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    }
                ),
                contentPadding = PaddingValues(horizontal = if (isIconOnly) 0.dp else 4.dp),
                interactionSource = interactionSource,
                content = buttonContent
            )
        }

        CommonButtonType.DASHED -> {
            // 虚线边框按钮 - 使用OutlinedButton但视觉上模拟虚线效果
            // 注意：Compose不直接支持虚线边框，这里使用实线但可以通过自定义绘制实现虚线
            OutlinedButton(
                onClick = onClick,
                modifier = modifier.then(
                    if (isIconOnly) Modifier.size(28.dp) else Modifier.height(28.dp)
                ),
                enabled = enabled,
                shape = shape,
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    disabledContainerColor = Color.Transparent,
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    }
                ),
                contentPadding = PaddingValues(horizontal = if (isIconOnly) 0.dp else 4.dp),
                interactionSource = interactionSource,
                content = buttonContent
            )
        }

        CommonButtonType.TEXT -> {
            TextButton(
                onClick = onClick,
                modifier = modifier.then(
                    if (isIconOnly) Modifier.size(28.dp) else Modifier.height(28.dp)
                ),
                enabled = enabled,
                shape = shape,
                colors = ButtonDefaults.textButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = Color.Transparent,
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                ),
                contentPadding = PaddingValues(horizontal = if (isIconOnly) 0.dp else 4.dp),
                interactionSource = interactionSource,
                content = buttonContent
            )
        }
    }
}
