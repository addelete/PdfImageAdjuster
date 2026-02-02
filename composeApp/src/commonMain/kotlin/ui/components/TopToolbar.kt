package ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun TopToolbar(
    pdfLoaded: Boolean,
    currentPageNumber: Int,
    totalPages: Int,
    pageInputValue: String,
    onOpenFile: () -> Unit,
    onSavePdf: () -> Unit,
    onPrevPage: () -> Unit,
    onNextPage: () -> Unit,
    onPageInputChange: (String) -> Unit,
    onPageInputSubmit: () -> Unit,
    isProcessing: Boolean = false,
    rightContent: @Composable () -> Unit = {}
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧按钮组
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CommonButton(
                    onClick = onOpenFile,
                    type = CommonButtonType.TEXT,
                    icon = Icons.Default.FolderOpen,
                    text = "打开",
                    enabled = !isProcessing
                )

                CommonButton(
                    onClick = onSavePdf,
                    type = CommonButtonType.TEXT,
                    icon = Icons.Default.Save,
                    text = "保存",
                    enabled = pdfLoaded && !isProcessing
                )
            }

            // 中间页面导航
            if (pdfLoaded) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CommonButton(
                        icon = Icons.Default.ChevronLeft,
                        type = CommonButtonType.TEXT,
//                        contentDescription = "上一页",
                        onClick = onPrevPage,
                        enabled = currentPageNumber > 1
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val focusManager = LocalFocusManager.current
                        var hasFocus by remember { mutableStateOf(false) }

                        // 下划线样式的文本输入框
                        Box(
                            modifier = Modifier.width(40.dp)
                        ) {
                            BasicTextField(
                                value = pageInputValue,
                                onValueChange = onPageInputChange,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { focusState ->
                                        if (hasFocus && !focusState.isFocused) {
                                            onPageInputSubmit()
                                        }
                                        hasFocus = focusState.isFocused
                                    },
                                textStyle = TextStyle(
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        onPageInputSubmit()
                                        focusManager.clearFocus()
                                    }
                                ),
                                singleLine = true,
                                decorationBox = { innerTextField ->
                                    Column {
                                        Box(
                                            modifier = Modifier.fillMaxWidth(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            innerTextField()
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        HorizontalDivider(
                                            thickness = 1.dp,
                                            color = if (hasFocus) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                            }
                                        )
                                    }
                                }
                            )
                        }

                        Text(
                            text = "/ $totalPages",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    CommonButton(
                        icon = Icons.Default.ChevronRight,
                        type = CommonButtonType.TEXT,
                        onClick = onNextPage,
                        enabled = currentPageNumber < totalPages
                    )
                }
            }

            // 右侧内容区域
            Box(
                modifier = Modifier.wrapContentWidth(),
                contentAlignment = Alignment.CenterEnd
            ) {
                rightContent()
            }
        }

        // 底部下划线
        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
        )
    }
}