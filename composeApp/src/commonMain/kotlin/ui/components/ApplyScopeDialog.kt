package ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 应用范围枚举
 */
enum class ApplyScope {
    CURRENT_IMAGE,      // 应用到此图片
    CURRENT_PAGE,       // 应用到本页所有图片
    ALL_PAGES           // 应用到本文档所有图片
}

/**
 * 应用范围选择对话框
 */
@Composable
fun ApplyScopeDialog(
    onScopeSelected: (ApplyScope) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedScope by remember { mutableStateOf(ApplyScope.CURRENT_IMAGE) }

    CommonDialog(
        title = "选择应用范围",
        onDismiss = onDismiss,
        modifier = Modifier.width(400.dp).wrapContentHeight(),
        closable = true,
        onOk = { onScopeSelected(selectedScope) },
        okText = "确定",
        cancelText = "取消",
        useWeight = false
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 应用到此图片
            ScopeOption(
                title = "应用到此图片",
                description = "仅处理当前选中的图片",
                selected = selectedScope == ApplyScope.CURRENT_IMAGE,
                onClick = { selectedScope = ApplyScope.CURRENT_IMAGE }
            )

            // 应用到本页所有图片
            ScopeOption(
                title = "应用到本页所有图片",
                description = "使用相同参数处理当前页面的所有图片",
                selected = selectedScope == ApplyScope.CURRENT_PAGE,
                onClick = { selectedScope = ApplyScope.CURRENT_PAGE }
            )

            // 应用到本文档所有图片
            ScopeOption(
                title = "应用到本文档所有图片",
                description = "使用相同参数处理整个文档的所有图片",
                selected = selectedScope == ApplyScope.ALL_PAGES,
                onClick = { selectedScope = ApplyScope.ALL_PAGES }
            )
        }
    }
}

/**
 * 单个范围选项组件
 */
@Composable
private fun ScopeOption(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick
            )
            Spacer(modifier = Modifier.width(4.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    }
                )
            }
        }
    }
}
