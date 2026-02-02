package ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import domain.models.UpdateCheckResult

@Composable
fun UpdateDialog(
    updateResult: UpdateCheckResult.UpdateAvailable,
    onDismiss: () -> Unit,
    onDownload: (String) -> Unit
) {
    CommonDialog(
        title = "发现新版本",
        onDismiss = onDismiss,
        modifier = Modifier.width(500.dp),
        useWeight = false,
        footer = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CommonButton(
                    onClick = onDismiss,
                    type = CommonButtonType.TEXT,
                    text = "稍后提醒"
                )
                Spacer(modifier = Modifier.width(8.dp))
                CommonButton(
                    onClick = { onDownload(updateResult.downloadUrl) },
                    type = CommonButtonType.SOLID,
                    text = "下载更新"
                )
            }
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("当前版本: ${updateResult.currentVersion}")
            Text("最新版本: ${updateResult.latestVersion}")

            if (updateResult.releaseNotes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("更新内容:", style = MaterialTheme.typography.titleSmall)
                Text(
                    updateResult.releaseNotes,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
