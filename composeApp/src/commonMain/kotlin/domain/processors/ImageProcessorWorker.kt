package domain.processors

import domain.models.AdjustmentConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 图像处理后台工作器
 * 在独立的协程中处理图像调整，避免阻塞 UI 线程
 */
class ImageProcessorWorker {
    // 使用专用调度器处理图片
    private val processorScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // 缓存的原始图像数据
    private var cachedOriginalImage: CachedImageData? = null

    // 处理器实例
    private val colorAdjuster = ColorAdjuster()
    private val curveProcessor = CurveProcessor()

    // 是否已初始化
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized

    /**
     * 缓存的图像数据
     */
    private data class CachedImageData(
        val data: ByteArray,
        val width: Int,
        val height: Int
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is CachedImageData) return false

            if (!data.contentEquals(other.data)) return false
            if (width != other.width) return false
            if (height != other.height) return false

            return true
        }

        override fun hashCode(): Int {
            var result = data.contentHashCode()
            result = 31 * result + width
            result = 31 * result + height
            return result
        }
    }

    /**
     * 初始化：克隆并缓存原始图像数据
     * 在打开调整弹窗时调用
     */
    suspend fun initImage(imageData: ByteArray, width: Int, height: Int) {
        withContext(processorScope.coroutineContext) {
            // 克隆图像数据
            val clonedData = imageData.copyOf()
            cachedOriginalImage = CachedImageData(clonedData, width, height)
            _isInitialized.value = true
        }
    }

    /**
     * 处理图像：使用缓存的原始图像数据
     * 仅传递调整参数，使用已缓存的原图
     */
    suspend fun processWithCachedImage(config: AdjustmentConfig): ProcessResult {
        return withContext(processorScope.coroutineContext) {
            val cached = cachedOriginalImage
                ?: throw IllegalStateException("Worker not initialized. Call initImage first.")

            // 克隆原始数据用于处理
            val workingData = cached.data.copyOf()

            // 应用调整
            applyAdjustments(workingData, cached.width, cached.height, config)

            ProcessResult(
                data = workingData,
                width = cached.width,
                height = cached.height
            )
        }
    }

    /**
     * 处理图像：传递图像数据和调整参数
     * 用于处理新的图像或不使用缓存的场景
     */
    suspend fun processWithImageData(
        imageData: ByteArray,
        width: Int,
        height: Int,
        config: AdjustmentConfig
    ): ProcessResult {
        return withContext(processorScope.coroutineContext) {
            // 克隆数据用于处理
            val workingData = imageData.copyOf()

            // 应用调整
            applyAdjustments(workingData, width, height, config)

            ProcessResult(
                data = workingData,
                width = width,
                height = height
            )
        }
    }

    /**
     * 共用的调整逻辑
     * 应用 HSL 和曲线调整
     */
    private fun applyAdjustments(
        data: ByteArray,
        width: Int,
        height: Int,
        config: AdjustmentConfig
    ) {
        // 预先生成曲线 LUT 以提高性能
        val rgbLut = curveProcessor.generateLUT(config.curves.rgb)
        val rLut = curveProcessor.generateLUT(config.curves.r)
        val gLut = curveProcessor.generateLUT(config.curves.g)
        val bLut = curveProcessor.generateLUT(config.curves.b)

        // 处理每个像素 (RGBA 格式)
        for (i in data.indices step 4) {
            val r = data[i].toInt() and 0xFF
            val g = data[i + 1].toInt() and 0xFF
            val b = data[i + 2].toInt() and 0xFF

            // 应用 HSL 调整
            val (h, s, l) = rgbToHsl(r, g, b)
            val adjustedHsl = applyHslAdjustment(h, s, l, config.hsl)
            val (newR, newG, newB) = hslToRgb(adjustedHsl)

            // 应用曲线调整 (先应用 RGB 整体曲线，再应用各通道曲线)
            val curvedR = rgbLut[newR.coerceIn(0, 255)]
            val curvedG = rgbLut[newG.coerceIn(0, 255)]
            val curvedB = rgbLut[newB.coerceIn(0, 255)]

            data[i] = rLut[curvedR].toByte()
            data[i + 1] = gLut[curvedG].toByte()
            data[i + 2] = bLut[curvedB].toByte()
            // Alpha 通道保持不变
        }
    }

    // RGB 转 HSL
    private fun rgbToHsl(r: Int, g: Int, b: Int): Triple<Float, Float, Float> {
        val rf = r / 255f
        val gf = g / 255f
        val bf = b / 255f

        val max = maxOf(rf, gf, bf)
        val min = minOf(rf, gf, bf)
        val delta = max - min

        val l = (max + min) / 2f

        if (delta == 0f) {
            return Triple(0f, 0f, l * 100f)
        }

        val s = if (l < 0.5f) {
            delta / (max + min)
        } else {
            delta / (2f - max - min)
        }

        val h = when (max) {
            rf -> ((gf - bf) / delta + if (gf < bf) 6f else 0f) / 6f
            gf -> ((bf - rf) / delta + 2f) / 6f
            else -> ((rf - gf) / delta + 4f) / 6f
        }

        return Triple(h * 360f, s * 100f, l * 100f)
    }

    // HSL 转 RGB
    private fun hslToRgb(hsl: Triple<Float, Float, Float>): Triple<Int, Int, Int> {
        val (h, s, l) = hsl
        val hNorm = h / 360f
        val sNorm = s / 100f
        val lNorm = l / 100f

        if (sNorm == 0f) {
            val gray = (lNorm * 255).toInt()
            return Triple(gray, gray, gray)
        }

        val q = if (lNorm < 0.5f) {
            lNorm * (1f + sNorm)
        } else {
            lNorm + sNorm - lNorm * sNorm
        }

        val p = 2f * lNorm - q

        val r = (hueToRgb(p, q, hNorm + 1f / 3f) * 255).toInt()
        val g = (hueToRgb(p, q, hNorm) * 255).toInt()
        val b = (hueToRgb(p, q, hNorm - 1f / 3f) * 255).toInt()

        return Triple(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
    }

    // 色调转 RGB 辅助函数
    private fun hueToRgb(p: Float, q: Float, t: Float): Float {
        var tNorm = t
        if (tNorm < 0f) tNorm += 1f
        if (tNorm > 1f) tNorm -= 1f

        return when {
            tNorm < 1f / 6f -> p + (q - p) * 6f * tNorm
            tNorm < 1f / 2f -> q
            tNorm < 2f / 3f -> p + (q - p) * (2f / 3f - tNorm) * 6f
            else -> p
        }
    }

    // 应用 HSL 调整
    private fun applyHslAdjustment(
        h: Float, s: Float, l: Float,
        adjustment: domain.models.HslAdjustment
    ): Triple<Float, Float, Float> {
        val newH = (h + adjustment.hue).mod(360f)
        val newS = (s + adjustment.saturation).coerceIn(0f, 100f)
        val newL = (l + adjustment.lightness).coerceIn(0f, 100f)
        return Triple(newH, newS, newL)
    }

    /**
     * 清除缓存的图像数据
     */
    fun clearCache() {
        cachedOriginalImage = null
        _isInitialized.value = false
    }

    /**
     * 销毁工作器，释放资源
     */
    fun destroy() {
        processorScope.cancel()
        clearCache()
    }
}

/**
 * 处理结果
 */
data class ProcessResult(
    val data: ByteArray,
    val width: Int,
    val height: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ProcessResult) return false

        if (!data.contentEquals(other.data)) return false
        if (width != other.width) return false
        if (height != other.height) return false

        return true
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + width
        result = 31 * result + height
        return result
    }
}
