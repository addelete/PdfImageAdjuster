package domain.processors

import domain.models.CurvePoint
import kotlin.math.*

/**
 * 曲线处理器
 * 使用单调三次 Hermite 插值生成平滑曲线（类似 Photoshop 曲线）
 */
class CurveProcessor {

    /**
     * 生成查找表（LUT）
     * 使用 Hermite 三次插值生成 256 个值的查找表
     */
    fun generateLUT(points: List<CurvePoint>): IntArray {
        val lut = IntArray(256)

        // 确保点按 x 坐标排序
        val sortedPoints = points.sortedBy { it.x }.toMutableList()

        // 确保有起点和终点
        if (sortedPoints.isEmpty() || sortedPoints.first().x > 0f) {
            sortedPoints.add(0, CurvePoint(0f, 0f))
        }
        if (sortedPoints.last().x < 1f) {
            sortedPoints.add(CurvePoint(1f, 1f))
        }

        // 计算切线（自动控制手柄）
        val tangents = calculateTangents(sortedPoints)

        // 使用三次 Hermite 插值生成 LUT
        for (i in 0..255) {
            val x = i / 255f
            val y = interpolateHermite(sortedPoints, tangents, x)
            lut[i] = (y * 255).toInt().coerceIn(0, 255)
        }

        return lut
    }

    /**
     * 计算单调三次插值的切线（Fritsch-Carlson 方法）
     * 自动计算每个点的切线，使曲线平滑且不产生过冲
     */
    private fun calculateTangents(points: List<CurvePoint>): FloatArray {
        val n = points.size
        val tangents = FloatArray(n) { 0f }

        if (n < 2) return tangents

        // 计算每段的斜率
        val deltas = FloatArray(n - 1)
        for (i in 0 until n - 1) {
            val dx = points[i + 1].x - points[i].x
            val dy = points[i + 1].y - points[i].y
            deltas[i] = if (dx > 0) dy / dx else 0f
        }

        // 第一个点的切线
        tangents[0] = deltas[0]

        // 中间点的切线（使用加权平均）
        for (i in 1 until n - 1) {
            val d0 = deltas[i - 1]
            val d1 = deltas[i]

            // 如果斜率符号不同，切线为 0（避免过冲）
            if (d0 * d1 <= 0) {
                tangents[i] = 0f
            } else {
                // 使用调和平均（Fritsch-Carlson 方法）
                val dx0 = points[i].x - points[i - 1].x
                val dx1 = points[i + 1].x - points[i].x
                val common = dx0 + dx1
                tangents[i] = (3f * common) / ((common + dx1) / d0 + (common + dx0) / d1)
            }
        }

        // 最后一个点的切线
        tangents[n - 1] = deltas[n - 2]

        return tangents
    }

    /**
     * 三次 Hermite 插值
     * 使用预计算的切线进行插值
     */
    private fun interpolateHermite(points: List<CurvePoint>, tangents: FloatArray, x: Float): Float {
        // 找到 x 所在的区间
        var i = 0
        for (j in 0 until points.size - 1) {
            if (x >= points[j].x && x <= points[j + 1].x) {
                i = j
                break
            }
        }

        val i1 = i + 1
        if (i1 >= points.size) return points[points.size - 1].y

        val p0 = points[i]
        val p1 = points[i1]
        val m0 = tangents[i]
        val m1 = tangents[i1]

        // 计算归一化参数 t (0-1)
        val dx = p1.x - p0.x
        if (dx == 0f) return p0.y

        val t = (x - p0.x) / dx
        val t2 = t * t
        val t3 = t2 * t

        // Hermite 基函数
        val h00 = 2f * t3 - 3f * t2 + 1f
        val h10 = t3 - 2f * t2 + t
        val h01 = -2f * t3 + 3f * t2
        val h11 = t3 - t2

        // 计算插值结果
        val y = h00 * p0.y + h10 * dx * m0 + h01 * p1.y + h11 * dx * m1

        return y
    }
}
