package ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties


/**
 * 通用对话框组件
 *
 * @param title 对话框标题
 * @param onDismiss 关闭对话框回调
 * @param modifier 修饰符
 * @param closable 是否显示关闭按钮，默认true
 * @param onOk 确定按钮回调，如果为null则不显示确定按钮
 * @param okText 确定按钮文本，默认"确定"
 * @param cancelText 取消按钮文本，默认"取消"
 * @param footer 自定义底部内容，如果提供则替换默认的按钮区域
 * @param contentPadding 内容区域的内边距，默认16.dp，设置为0.dp可去除内边距
 * @param useWeight 内容区域是否使用weight占据剩余空间，默认true。设置为false时内容区域会根据内容自动调整高度
 * @param content 对话框主体内容
 */
@Composable
fun CommonDialog(
    title: String,
    onDismiss:  (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    closable: Boolean = true,
    onOk: (() -> Unit)? = null,
    okText: String = "确定",
    cancelText: String = "取消",
    footer: (@Composable () -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    useWeight: Boolean = true,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss ?: {},
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // 标题栏
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (closable) {
                        CommonButton(
                            onClick = onDismiss ?: {},
                            type = CommonButtonType.TEXT,
                            icon = Icons.Default.Close,
                        )
                    }
                }

                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )

                // 主体内容 - 根据 useWeight 参数决定是否占据剩余空间
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (useWeight) Modifier.weight(1f) else Modifier)
                        .padding(contentPadding)
                ) {
                    content()
                }

                // 底部区域
                if (footer != null) {
                    // 使用自定义footer
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        footer()
                    }
                } else if (onOk != null) {
                    // 使用默认按钮
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CommonButton(
                            onClick = onDismiss ?: {},
                            type = CommonButtonType.TEXT,
                            text = cancelText
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        CommonButton(
                            onClick = onOk,
                            type = CommonButtonType.SOLID,
                            text = okText
                        )
                    }
                }
            }
        }
    }
}
