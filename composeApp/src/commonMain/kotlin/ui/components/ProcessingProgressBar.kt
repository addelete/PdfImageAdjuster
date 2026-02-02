package ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


/**
 * 处理进度状态
 */
sealed class ProcessingState {
    object Idle : ProcessingState()
    object Preparing : ProcessingState()
    data class Processing(val current: Int, val total: Int) : ProcessingState()
    object Completed : ProcessingState()
}

/**
 * 进度显示组件
 */
@Composable
fun ProcessingProgressBar(
    state: ProcessingState,
    onStop: () -> Unit,
    onCompleted: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // 完成状态5秒后自动消失
    LaunchedEffect(state) {
        if (state is ProcessingState.Completed) {
            kotlinx.coroutines.delay(5000)
            // 延迟后直接调用回调，让父组件决定是否重置状态
            onCompleted()
        }
    }

    AnimatedVisibility(
        visible = state !is ProcessingState.Idle,
        enter = fadeIn() + expandHorizontally(),
        exit = fadeOut() + shrinkHorizontally(),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .wrapContentWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
                when (state) {
                    is ProcessingState.Preparing -> {
                        // 进度指示器
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // 准备文本
                        Text(
                            text = "准备中...",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }

                    is ProcessingState.Processing -> {
                        // 进度指示器
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // 进度文本
                        Text(
                            text = "处理中 ${state.current}/${state.total}",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )

                        // 停止按钮
                        CommonButton(
                            text = "停止",
                            onClick = onStop
                        )
                    }

                    is ProcessingState.Completed -> {
                        // 完成图标（绿色）
                        Text(
                            text = "✓",
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 14.sp,
                            color = androidx.compose.ui.graphics.Color(0xFF4CAF50)  // 绿色
                        )

                        // 完成文本
                        Text(
                            text = "处理完成",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }

                    else -> {}
                }
        }
    }
}
