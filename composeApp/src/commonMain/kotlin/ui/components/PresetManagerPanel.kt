package ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import domain.models.ColorPreset
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PresetManagerPanel(
    presets: List<ColorPreset>,
    onLoadPreset: (ColorPreset) -> Unit,
    onSavePreset: (String) -> Unit,
    onDeletePreset: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSaveDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedPresetId by remember { mutableStateOf("") }
    var presetName by remember { mutableStateOf("") }
    var currentLoadedPresetName by remember { mutableStateOf<String?>(null) }
    var saveMode by remember { mutableStateOf("new") } // "new" or "overwrite"
    var showValidationError by remember { mutableStateOf(false) }

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
            // 标题和保存按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "预设",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                )

                CommonButton(
                    text = "保存",
                    type = CommonButtonType.TEXT,
                    onClick = {
                        // Auto-fill preset name if loaded from preset
                        presetName = currentLoadedPresetName ?: ""
                        saveMode = if (currentLoadedPresetName != null) "overwrite" else "new"
                        showSaveDialog = true
                    }
                )
            }

            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )
            // 预设列表
            if (presets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无预设",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.height(200.dp),
                ) {
                    items(presets) { preset ->
                        PresetItem(
                            preset = preset,
                            onLoad = {
                                currentLoadedPresetName = preset.name
                                onLoadPreset(preset)
                            },
                            onDelete = {
                                selectedPresetId = preset.id
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    // 保存预设对话框
    if (showSaveDialog) {
        val nameExists = presets.any { it.name == presetName.trim() }
        val isOverwritingCurrent = currentLoadedPresetName != null &&
                                   presetName.trim() == currentLoadedPresetName
        val nameError = when {
            presetName.isBlank() -> "名称不能为空"
            saveMode == "new" && nameExists -> "名称已存在"
            saveMode == "overwrite" && !isOverwritingCurrent && nameExists -> "名称已存在"
            else -> null
        }

        CommonDialog(
            title = "保存预设",
            onDismiss = {
                showSaveDialog = false
                presetName = ""
                showValidationError = false
            },
            modifier = Modifier.width(400.dp).wrapContentHeight(),
            closable = true,
            onOk = {
                if (nameError == null) {
                    onSavePreset(presetName.trim())
                    if (saveMode == "overwrite" || saveMode == "new") {
                        currentLoadedPresetName = presetName.trim()
                    }
                    presetName = ""
                    showSaveDialog = false
                    showValidationError = false
                } else {
                    showValidationError = true
                }
            },
            okText = "保存",
            cancelText = "取消",
            useWeight = false
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = presetName,
                    onValueChange = { presetName = it },
                    label = { Text("预设名称") },
                    singleLine = true,
                    isError = showValidationError && nameError != null,
                    supportingText = if (showValidationError && nameError != null) {
                        { Text(nameError) }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )

                // 只在从预设加载时显示覆盖/另存为选项
                if (currentLoadedPresetName != null) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = saveMode == "overwrite",
                                onClick = { saveMode = "overwrite" }
                            )
                            Text("覆盖当前预设")
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = saveMode == "new",
                                onClick = { saveMode = "new" }
                            )
                            Text("另存为新预设")
                        }
                    }
                }
            }
        }
    }

    // 删除预设对话框
    if (showDeleteDialog) {
        CommonDialog(
            title = "删除预设",
            onDismiss = {
                showDeleteDialog = false
                selectedPresetId = ""
            },
            modifier = Modifier.width(400.dp).wrapContentHeight(),
            closable = true,
            onOk = {
                onDeletePreset(selectedPresetId)
                showDeleteDialog = false
                selectedPresetId = ""
            },
            okText = "删除",
            cancelText = "取消",
            useWeight = false
        ) {
            Text("确定要删除这个预设吗？")
        }
    }
}