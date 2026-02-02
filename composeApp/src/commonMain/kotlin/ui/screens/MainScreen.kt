package ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import platform.FileChooser
import platform.PresetStorage
import data.PdfRepository
import data.RenderedPage
import domain.models.ImageInfo
import domain.models.AdjustmentConfig
import domain.models.ColorPreset
import ui.components.TopToolbar
import ui.components.ImageListPanel
import ui.components.PdfViewer
import ui.components.ApplyScopeDialog
import ui.components.ApplyScope
import ui.components.ProcessingProgressBar
import ui.components.ProcessingState
import ui.components.SavePdfDialog
import ui.components.LeftSidePanel
import data.OutlineItem
import domain.models.ImageCacheManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    pdfRepository: PdfRepository = TODO("需要在调用处提供实现")
) {
    var pdfLoaded by remember { mutableStateOf(false) }
    var currentPageNumber by remember { mutableStateOf(1) }
    var totalPages by remember { mutableStateOf(0) }
    var pageInputValue by remember { mutableStateOf("1") }
    var isModalOpen by remember { mutableStateOf(false) }
    var selectedImage by remember { mutableStateOf<ImageInfo?>(null) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var currentImages by remember { mutableStateOf<List<ImageInfo>>(emptyList()) }
    var currentPageImageData by remember { mutableStateOf<ByteArray?>(null) }
    var currentPageWidth by remember { mutableStateOf(0) }
    var currentPageHeight by remember { mutableStateOf(0) }
    var isPageLoading by remember { mutableStateOf(false) }

    // 窗口大小
    var windowWidth by remember { mutableStateOf(0) }
    var windowHeight by remember { mutableStateOf(0) }
    val density = LocalDensity.current

    // PDF仓库和文件选择器
    val fileChooser = remember { FileChooser() }
    val presetStorage = remember { PresetStorage() }
    val coroutineScope = rememberCoroutineScope()

    // 调整配置和预设
    var adjustmentConfig by remember { mutableStateOf(AdjustmentConfig()) }
    var presets by remember { mutableStateOf<List<ColorPreset>>(emptyList()) }

    // 加载预设列表
    fun loadPresets() {
        presets = presetStorage.loadAllPresets()
    }

    // 图片缓存管理器
    val imageCacheManager = remember { ImageCacheManager() }

    // 应用范围选择对话框
    var showApplyScopeDialog by remember { mutableStateOf(false) }

    // 处理进度状态
    var processingState by remember { mutableStateOf<ProcessingState>(ProcessingState.Idle) }

    // 当前处理任务
    var currentProcessingJob by remember { mutableStateOf<Job?>(null) }

    // PDF目录大纲
    var outlineItems by remember { mutableStateOf<List<OutlineItem>>(emptyList()) }

    // 页面处理触发器，每次页面被处理时递增
    var processedPageTrigger by remember { mutableStateOf(0) }

    // 加载页面内容（图片和渲染）
    suspend fun loadPageContent(pageNumber: Int) {
        try {
            isPageLoading = true
            val images = pdfRepository.extractImages(pageNumber - 1)
            currentImages = images

            // 渲染当前页面（使用更高的缩放比例以提高清晰度）
            val renderedPage = pdfRepository.renderPage(pageNumber - 1, 2.5f)
            currentPageImageData = renderedPage.imageData
            currentPageWidth = renderedPage.width
            currentPageHeight = renderedPage.height
        } catch (e: Exception) {
            println("Failed to load page: ${e.message}")
        } finally {
            isPageLoading = false
        }
    }

    // 打开PDF文件的通用函数
    val openPdfFile: () -> Unit = {
        coroutineScope.launch {
            try {
                val filePath = fileChooser.chooseFile(
                    title = "选择PDF文件",
                    allowedExtensions = listOf("pdf")
                )
                if (filePath != null) {
                    // 清理之前的缓存，释放内存
                    imageCacheManager.clearAll()
                    // 加载PDF
                    val pdfDocument = pdfRepository.loadPdf(filePath)
                    pdfLoaded = true
                    totalPages = pdfDocument.totalPages
                    currentPageNumber = 1
                    pageInputValue = "1"

                    // 加载目录大纲
                    outlineItems = try {
                        pdfRepository.getOutline()
                    } catch (e: Exception) {
                        println("Failed to load outline: ${e.message}")
                        emptyList()
                    }

                    // 加载第一页的图片和渲染页面
                    loadPageContent(currentPageNumber)
                }
            } catch (e: Exception) {
                // TODO: 显示错误消息
                println("load pdf failed: ${e.message}")
            }
        }
    }

    // 开发时自动加载默认PDF（通过环境变量 DEV_PDF_PATH 配置）
    LaunchedEffect(Unit) {
        try {
            val defaultPdfPath = System.getenv("DEV_PDF_PATH")
            if (!defaultPdfPath.isNullOrBlank()) {
                val file = java.io.File(defaultPdfPath)
                if (file.exists()) {
                    val pdfDocument = pdfRepository.loadPdf(defaultPdfPath)
                    pdfLoaded = true
                    totalPages = pdfDocument.totalPages
                    currentPageNumber = 1
                    pageInputValue = "1"

                    // 加载目录大纲
                    outlineItems = try {
                        pdfRepository.getOutline()
                    } catch (e: Exception) {
                        println("Failed to load outline: ${e.message}")
                        emptyList()
                    }

                    loadPageContent(currentPageNumber)
                } else {
                    println("DEV_PDF_PATH file not found: $defaultPdfPath")
                }
            }
        } catch (e: Exception) {
            println("auto load pdf failed: ${e.message}")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                windowWidth = size.width
                windowHeight = size.height
            }
    ) {
        // 顶部工具栏
        TopToolbar(
            pdfLoaded = pdfLoaded,
            currentPageNumber = currentPageNumber,
            totalPages = totalPages,
            pageInputValue = pageInputValue,
            isProcessing = processingState !is ProcessingState.Idle,
            onOpenFile = openPdfFile,
            onSavePdf = { showSaveDialog = true },
            onPrevPage = {
                if (currentPageNumber > 1) {
                    currentPageNumber--
                    pageInputValue = currentPageNumber.toString()
                    coroutineScope.launch {
                        loadPageContent(currentPageNumber)
                    }
                }
            },
            onNextPage = {
                if (currentPageNumber < totalPages) {
                    currentPageNumber++
                    pageInputValue = currentPageNumber.toString()
                    coroutineScope.launch {
                        loadPageContent(currentPageNumber)
                    }
                }
            },
            onPageInputChange = { pageInputValue = it },
            onPageInputSubmit = {
                val pageNum = pageInputValue.toIntOrNull()
                if (pageNum != null && pageNum in 1..totalPages) {
                    currentPageNumber = pageNum
                    coroutineScope.launch {
                        loadPageContent(currentPageNumber)
                    }
                }
            },
            rightContent = {
                ProcessingProgressBar(
                    state = processingState,
                    onStop = {
                        currentProcessingJob?.cancel()
                        processingState = ProcessingState.Idle
                    },
                    onCompleted = {
                        processingState = ProcessingState.Idle
                    }
                )
            }
        )

        // 工作区
        // 使用PDF路径作为key，确保打开新PDF时完全重新渲染
        key(pdfRepository.getCurrentPdfPath()) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
            ) {
            // 左侧面板（页面缩略图和目录）
            if (pdfLoaded) {
               
               LeftSidePanel(
                    totalPages = totalPages,
                    currentPageNumber = currentPageNumber,
                    outlineItems = outlineItems,
                    pdfRepository = pdfRepository,
                    onPageClick = { pageNumber ->
                        currentPageNumber = pageNumber
                        pageInputValue = pageNumber.toString()
                        coroutineScope.launch {
                            loadPageContent(currentPageNumber)
                        }
                    },
                    modifier = Modifier.width(250.dp),
                    processedPageTrigger = processedPageTrigger
                )
                VerticalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )
            }

            // 中间PDF查看区域
            Box(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                PdfViewer(
                    pdfImageData = currentPageImageData,
                    width = currentPageWidth,
                    height = currentPageHeight,
                    isLoading = isPageLoading,
                    modifier = Modifier.fillMaxSize(),
                    onOpenFile = openPdfFile,
                    onNextPage = {
                        if (currentPageNumber < totalPages) {
                            currentPageNumber++
                            pageInputValue = currentPageNumber.toString()
                            coroutineScope.launch {
                                loadPageContent(currentPageNumber)
                            }
                        }
                    },
                    onPrevPage = {
                        if (currentPageNumber > 1) {
                            currentPageNumber--
                            pageInputValue = currentPageNumber.toString()
                            coroutineScope.launch {
                                loadPageContent(currentPageNumber)
                            }
                        }
                    }
                )
            }

            // 只在PDF加载后显示右侧图片列表
            if (pdfLoaded) {
                VerticalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )

                // 右侧图片列表
                ImageListPanel(
                    images = currentImages,
                    onImageSelected = { imageInfo ->
                        selectedImage = imageInfo

                        // 缓存原始图片数据（如果未缓存）
                        if (!imageCacheManager.isCached(imageInfo)) {
                            imageCacheManager.cacheOriginalImage(imageInfo)
                        }

                        // 加载已缓存的处理参数，如果没有则使用默认值
                        adjustmentConfig = imageCacheManager.getAdjustmentConfig(imageInfo)
                            ?: AdjustmentConfig()

                        // 加载预设列表
                        loadPresets()

                        isModalOpen = true
                    },
                    modifier = Modifier.width(300.dp)
                )
            }
            }
        }
    }

    // 图片调整弹窗
    if (isModalOpen && selectedImage != null) {
        AdjustmentDialog(
            imageInfo = selectedImage!!,
            adjustmentConfig = adjustmentConfig,
            presets = presets,
            onConfigChange = { newConfig ->
                adjustmentConfig = newConfig
            },
            onLoadPreset = { preset ->
                adjustmentConfig = AdjustmentConfig(
                    hsl = preset.hsl,
                    curves = preset.curves
                )
            },
            onSavePreset = { name ->
                // 检查是否已存在同名预设（覆盖模式）
                val existingPreset = presets.find { it.name == name }

                val newPreset = ColorPreset(
                    id = existingPreset?.id ?: presetStorage.sanitizeFileName(name),
                    name = name,
                    createdAt = existingPreset?.createdAt ?: System.currentTimeMillis(),
                    hsl = adjustmentConfig.hsl,
                    curves = adjustmentConfig.curves
                )

                // 保存到文件
                if (presetStorage.savePreset(newPreset)) {
                    // 重新加载预设列表
                    loadPresets()
                }
            },
            onDeletePreset = { id ->
                // 从文件中删除
                if (presetStorage.deletePreset(id)) {
                    // 重新加载预设列表
                    loadPresets()
                }
            },
            onApply = {
                // 显示应用范围选择对话框
                showApplyScopeDialog = true
            },
            onDismiss = {
                isModalOpen = false
                selectedImage = null
            },
            windowWidth = windowWidth,
            windowHeight = windowHeight,
            originalImageData = imageCacheManager.getOriginalData(selectedImage!!)
        )
    }

    // 应用范围选择对话框
    if (showApplyScopeDialog && selectedImage != null) {
        ApplyScopeDialog(
            onScopeSelected = { scope ->
                showApplyScopeDialog = false
                isModalOpen = false  // 关闭调整对话框

                // 立即显示准备状态
                processingState = ProcessingState.Preparing

                // 启动处理任务
                currentProcessingJob = coroutineScope.launch {
                    applyAdjustmentsWithProgress(
                        scope = scope,
                        selectedImage = selectedImage!!,
                        adjustmentConfig = adjustmentConfig,
                        pdfRepository = pdfRepository,
                        imageCacheManager = imageCacheManager,
                        currentPageNumber = currentPageNumber,
                        onProgressUpdate = { current, total ->
                            processingState = ProcessingState.Processing(current, total)
                        },
                        onPageProcessed = { processedPageIndex ->
                            // 如果处理的是当前页，刷新
                            launch {
                                if (processedPageIndex == currentPageNumber) {
                                    loadPageContent(currentPageNumber)
                                }
                            }
                            // 递增触发器，通知缩略图面板刷新
                            processedPageTrigger++
                        }
                    )
                    // 处理完成后的操作
                    processingState = ProcessingState.Completed
                }
            },
            onDismiss = {
                showApplyScopeDialog = false
            }
        )
    }

    // 保存对话框
    if (showSaveDialog) {
        val currentPath = pdfRepository.getCurrentPdfPath()
        val defaultFileName = if (currentPath != null) {
            val file = java.io.File(currentPath)
            val nameWithoutExt = file.nameWithoutExtension
            "${nameWithoutExt}_processed.pdf"
        } else {
            "output.pdf"
        }

        SavePdfDialog(
            defaultFileName = defaultFileName,
            onSave = { fileName ->
                coroutineScope.launch {
                    try {
                        val currentPath = pdfRepository.getCurrentPdfPath()
                        if (currentPath != null) {
                            val sourceFile = java.io.File(currentPath)
                            val parentDir = sourceFile.parent
                            val outputPath = if (parentDir != null) {
                                "$parentDir${java.io.File.separator}$fileName"
                            } else {
                                fileName
                            }

                            // 确保文件名以 .pdf 结尾
                            val finalPath = if (!outputPath.endsWith(".pdf", ignoreCase = true)) {
                                "$outputPath.pdf"
                            } else {
                                outputPath
                            }

                            pdfRepository.savePdf(finalPath)
                            showSaveDialog = false
                        }
                    } catch (e: Exception) {
                        println("savePdf failed: ${e.message}")
                        e.printStackTrace()
                    }
                }
            },
            onDismiss = {
                showSaveDialog = false
            }
        )
    }
}

/**
 * 应用调整参数到图片（带进度跟踪）
 */
private suspend fun applyAdjustmentsWithProgress(
    scope: ApplyScope,
    selectedImage: ImageInfo,
    adjustmentConfig: AdjustmentConfig,
    pdfRepository: PdfRepository,
    imageCacheManager: ImageCacheManager,
    currentPageNumber: Int,
    onProgressUpdate: (current: Int, total: Int) -> Unit,
    onPageProcessed: (pageIndex: Int) -> Unit
) {
    try {
        when (scope) {
            ApplyScope.CURRENT_IMAGE -> {
                // 单张图片：直接处理
                val imagesToProcess = listOf(selectedImage)
                processImagesWithProgress(
                    images = imagesToProcess,
                    adjustmentConfig = adjustmentConfig,
                    pdfRepository = pdfRepository,
                    imageCacheManager = imageCacheManager,
                    onProgressUpdate = onProgressUpdate,
                    onPageProcessed = onPageProcessed
                )
            }
            ApplyScope.CURRENT_PAGE -> {
                // 当前页：直接处理
                val imagesToProcess = pdfRepository.getPageImages(currentPageNumber - 1)
                processImagesWithProgress(
                    images = imagesToProcess,
                    adjustmentConfig = adjustmentConfig,
                    pdfRepository = pdfRepository,
                    imageCacheManager = imageCacheManager,
                    onProgressUpdate = onProgressUpdate,
                    onPageProcessed = onPageProcessed
                )
            }
            ApplyScope.ALL_PAGES -> {
                // 所有页面：逐页提取和处理，避免长时间准备
                processAllPagesWithProgress(
                    pdfRepository = pdfRepository,
                    adjustmentConfig = adjustmentConfig,
                    imageCacheManager = imageCacheManager,
                    onProgressUpdate = onProgressUpdate,
                    onPageProcessed = onPageProcessed
                )
            }
        }
    } catch (e: Exception) {
        println("applyAdjustmentsWithProgress failed: ${e.message}")
        e.printStackTrace()
        throw e  // 重新抛出异常，让调用者处理
    }
}

/**
 * 处理所有页面的图片（逐页提取和处理，避免长时间准备）
 */
private suspend fun processAllPagesWithProgress(
    pdfRepository: PdfRepository,
    adjustmentConfig: AdjustmentConfig,
    imageCacheManager: ImageCacheManager,
    onProgressUpdate: (current: Int, total: Int) -> Unit,
    onPageProcessed: (pageIndex: Int) -> Unit
) {
    val imageProcessor = domain.processors.ImageProcessorWorker()

    try {
        // 获取总页数
        val totalPages = pdfRepository.getTotalPages()

        var totalProcessedImages = 0
        var estimatedTotalImages = totalPages  // 初始估算：每页至少1张图

        // 逐页处理
        for (pageIndex in 0 until totalPages) {
            // 检查协程是否被取消
            if (!kotlinx.coroutines.coroutineScope { isActive }) {
                break
            }


            // 提取当前页的图片
            val pageImages = pdfRepository.getPageImages(pageIndex)

            // 更新总图片数估算
            if (pageIndex == 0 && pageImages.isNotEmpty()) {
                // 根据第一页的图片数量估算总数
                estimatedTotalImages = pageImages.size * totalPages
            }

            // 处理当前页的每张图片
            for (imageInfo in pageImages) {
                // 检查协程是否被取消
                if (!kotlinx.coroutines.coroutineScope { isActive }) {
                    break
                }

                totalProcessedImages++

                // 更新进度
                onProgressUpdate(totalProcessedImages, estimatedTotalImages)

                // 确保原图数据已缓存
                if (!imageCacheManager.isCached(imageInfo)) {
                    imageCacheManager.cacheOriginalImage(imageInfo)
                }

                // 获取原始图片数据
                val originalData = imageCacheManager.getOriginalData(imageInfo)
                    ?: imageInfo.data

                // 使用ImageProcessor处理图片
                imageProcessor.initImage(originalData, imageInfo.width, imageInfo.height)
                val processedResult = imageProcessor.processWithCachedImage(adjustmentConfig)

                // 替换PDF中的图片
                pdfRepository.replaceImage(imageInfo, processedResult.data)

                // 更新缓存中的处理参数
                imageCacheManager.updateAdjustmentConfig(imageInfo, adjustmentConfig)
            }

            // 当前页处理完成，触发刷新
            if (pageImages.isNotEmpty()) {
                onPageProcessed(pageIndex + 1)
            }
        }
    } finally {
        imageProcessor.destroy()
    }
}

/**
 * 处理图片列表（带进度跟踪和实时刷新）
 */
private suspend fun processImagesWithProgress(
    images: List<ImageInfo>,
    adjustmentConfig: AdjustmentConfig,
    pdfRepository: PdfRepository,
    imageCacheManager: ImageCacheManager,
    onProgressUpdate: (current: Int, total: Int) -> Unit,
    onPageProcessed: (pageIndex: Int) -> Unit
) {
    val imageProcessor = domain.processors.ImageProcessorWorker()
    val processedPagesInCurrentBatch = mutableSetOf<Int>()

    try {
        val totalImages = images.size

        for ((index, imageInfo) in images.withIndex()) {
            // 检查协程是否被取消
            if (!kotlinx.coroutines.coroutineScope { isActive }) {
                break
            }

            // 更新进度
            onProgressUpdate(index + 1, totalImages)

            // 确保原图数据已缓存
            if (!imageCacheManager.isCached(imageInfo)) {
                imageCacheManager.cacheOriginalImage(imageInfo)
            }

            // 获取原始图片数据
            val originalData = imageCacheManager.getOriginalData(imageInfo)
                ?: imageInfo.data

            // 使用ImageProcessor处理图片
            imageProcessor.initImage(originalData, imageInfo.width, imageInfo.height)
            val processedResult = imageProcessor.processWithCachedImage(adjustmentConfig)

            // 替换PDF中的图片
            pdfRepository.replaceImage(imageInfo, processedResult.data)

            // 更新缓存中的处理参数
            imageCacheManager.updateAdjustmentConfig(imageInfo, adjustmentConfig)

            // 记录当前页面
            val currentPageIndex = imageInfo.pageIndex

            // 检查下一张图片是否属于不同的页面（或者是最后一张图片）
            val isLastImage = index == totalImages - 1
            val nextImagePageIndex = if (!isLastImage) images[index + 1].pageIndex else -1
            val isPageComplete = isLastImage || (nextImagePageIndex != currentPageIndex)


            // 如果当前页面的所有图片都处理完了，且该页面还没有刷新过，则通知刷新
            if (isPageComplete && !processedPagesInCurrentBatch.contains(currentPageIndex)) {
                processedPagesInCurrentBatch.add(currentPageIndex)
                onPageProcessed(currentPageIndex)
            }
        }
    } finally {
        imageProcessor.destroy()
    }
}