package domain.processors

import domain.models.AdjustmentConfig
import domain.models.HslAdjustment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.*

class ColorAdjuster {
    // 使用专用调度器处理图片
    private val imageDispatcher = Dispatchers.Default.limitedParallelism(4)

    suspend fun adjustImage(
        imageData: ByteArray,
        config: AdjustmentConfig
    ): ByteArray = withContext(imageDispatcher) {
        // 原地修改，零拷贝
        processPixels(imageData, config)
        imageData
    }

    private fun processPixels(data: ByteArray, config: AdjustmentConfig) {
        // 处理 RGBA 像素
        for (i in data.indices step 4) {
            val r = data[i].toInt() and 0xFF
            val g = data[i + 1].toInt() and 0xFF
            val b = data[i + 2].toInt() and 0xFF

            // 应用 HSL 调整
            val (h, s, l) = rgbToHsl(r, g, b)
            val adjustedHsl = applyHslAdjustment(h, s, l, config.hsl)
            val (newR, newG, newB) = hslToRgb(adjustedHsl)

            // 应用曲线调整
            data[i] = applyCurve(newR, config.curves.r).toByte()
            data[i + 1] = applyCurve(newG, config.curves.g).toByte()
            data[i + 2] = applyCurve(newB, config.curves.b).toByte()
        }
    }

    private fun rgbToHsl(r: Int, g: Int, b: Int): Triple<Float, Float, Float> {
        val rf = r / 255f
        val gf = g / 255f
        val bf = b / 255f

        val max = maxOf(rf, gf, bf)
        val min = minOf(rf, gf, bf)
        val delta = max - min

        val l = (max + min) / 2f

        if (delta == 0f) {
            return Triple(0f, 0f, l)
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

    private fun applyHslAdjustment(
        h: Float, s: Float, l: Float,
        adjustment: HslAdjustment
    ): Triple<Float, Float, Float> {
        val newH = (h + adjustment.hue).mod(360f)
        val newS = (s + adjustment.saturation).coerceIn(0f, 100f)
        val newL = (l + adjustment.lightness).coerceIn(0f, 100f)
        return Triple(newH, newS, newL)
    }

    private fun applyCurve(value: Int, points: List<domain.models.CurvePoint>): Int {
        // 简化实现：线性插值
        val normalized = value / 255f

        // 如果只有默认点，直接返回
        if (points.size <= 2 && points.all { it.x == it.y }) {
            return value
        }

        // 找到相邻的两个点进行插值
        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]

            if (normalized >= p1.x && normalized <= p2.x) {
                val t = (normalized - p1.x) / (p2.x - p1.x)
                val interpolated = p1.y + t * (p2.y - p1.y)
                return (interpolated * 255).toInt().coerceIn(0, 255)
            }
        }

        return (normalized * 255).toInt().coerceIn(0, 255)
    }
}