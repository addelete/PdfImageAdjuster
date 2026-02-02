package ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import data.OutlineItem
import data.PdfRepository

enum class LeftPanelTab {
    PAGES,
    OUTLINE
}

@Composable
fun LeftSidePanel(
    totalPages: Int,
    currentPageNumber: Int,
    outlineItems: List<OutlineItem>,
    pdfRepository: PdfRepository,
    onPageClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    processedPageTrigger: Int = 0  // 页面处理触发器
) {
    var selectedTab by remember { mutableStateOf(LeftPanelTab.PAGES) }

    Column(modifier = modifier.fillMaxHeight()) {
        // Tab Row with reduced height
        SecondaryTabRow(
            selectedTabIndex = selectedTab.ordinal,
            modifier = Modifier.fillMaxWidth().height(36.dp)
        ) {
            Tab(
                selected = selectedTab == LeftPanelTab.PAGES,
                onClick = { selectedTab = LeftPanelTab.PAGES },
                text = { Text("页面") },
                modifier = Modifier.height(36.dp)
            )
            Tab(
                selected = selectedTab == LeftPanelTab.OUTLINE,
                onClick = { selectedTab = LeftPanelTab.OUTLINE },
                text = { Text("目录") },
                modifier = Modifier.height(36.dp)
            )
        }

        // Content
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (selectedTab) {
                LeftPanelTab.PAGES -> {
                    PageThumbnailPanel(
                        totalPages = totalPages,
                        currentPageNumber = currentPageNumber,
                        pdfRepository = pdfRepository,
                        onPageClick = onPageClick,
                        processedPageTrigger = processedPageTrigger
                    )
                }
                LeftPanelTab.OUTLINE -> {
                    OutlinePanel(
                        outlineItems = outlineItems,
                        onPageClick = onPageClick
                    )
                }
            }
        }
    }
}
