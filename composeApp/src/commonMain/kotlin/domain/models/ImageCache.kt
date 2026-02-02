package domain.models

/**
 * 图片缓存数据结构
 * 用于存储原始图片数据和处理参数
 */
data class ImageCache(
    val imageId: String,                    // 图片唯一标识
    val originalData: ByteArray,            // 原始图片数据（RGBA格式）
    val width: Int,                         // 图片宽度
    val height: Int,                        // 图片高度
    val adjustmentConfig: AdjustmentConfig? = null  // 应用的处理参数（null表示未处理）
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ImageCache

        if (imageId != other.imageId) return false
        if (!originalData.contentEquals(other.originalData)) return false
        if (width != other.width) return false
        if (height != other.height) return false
        if (adjustmentConfig != other.adjustmentConfig) return false

        return true
    }

    override fun hashCode(): Int {
        var result = imageId.hashCode()
        result = 31 * result + originalData.contentHashCode()
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + (adjustmentConfig?.hashCode() ?: 0)
        return result
    }
}

/**
 * 图片缓存管理器
 * 负责管理所有图片的原始数据和处理参数
 */
class ImageCacheManager {
    // 图片ID -> 缓存数据
    private val cacheMap = mutableMapOf<String, ImageCache>()

    /**
     * 生成图片唯一标识
     * 优先使用resourceName，如果不存在则使用 "page_{pageIndex}_img_{imageIndex}"
     */
    fun generateImageId(imageInfo: ImageInfo): String {
        return imageInfo.resourceName ?: "page_${imageInfo.pageIndex}_img_${imageInfo.imageIndex}"
    }

    /**
     * 缓存原始图片数据
     * 如果已存在缓存，则不覆盖原始数据，只更新处理参数
     */
    fun cacheOriginalImage(imageInfo: ImageInfo) {
        val imageId = generateImageId(imageInfo)
        if (!cacheMap.containsKey(imageId)) {
            cacheMap[imageId] = ImageCache(
                imageId = imageId,
                originalData = imageInfo.data.copyOf(), // 复制一份避免被修改
                width = imageInfo.width,
                height = imageInfo.height,
                adjustmentConfig = null
            )
        }
    }

    /**
     * 更新图片的处理参数
     */
    fun updateAdjustmentConfig(imageInfo: ImageInfo, config: AdjustmentConfig) {
        val imageId = generateImageId(imageInfo)
        val cache = cacheMap[imageId]
        if (cache != null) {
            cacheMap[imageId] = cache.copy(adjustmentConfig = config)
        } else {
            // 如果缓存不存在，先缓存原始数据
            cacheOriginalImage(imageInfo)
            cacheMap[imageId] = cacheMap[imageId]!!.copy(adjustmentConfig = config)
        }
    }

    /**
     * 获取图片的缓存数据
     */
    fun getCache(imageInfo: ImageInfo): ImageCache? {
        val imageId = generateImageId(imageInfo)
        return cacheMap[imageId]
    }

    /**
     * 获取图片的原始数据
     */
    fun getOriginalData(imageInfo: ImageInfo): ByteArray? {
        return getCache(imageInfo)?.originalData
    }

    /**
     * 获取图片的处理参数
     */
    fun getAdjustmentConfig(imageInfo: ImageInfo): AdjustmentConfig? {
        return getCache(imageInfo)?.adjustmentConfig
    }

    /**
     * 检查图片是否已缓存
     */
    fun isCached(imageInfo: ImageInfo): Boolean {
        val imageId = generateImageId(imageInfo)
        return cacheMap.containsKey(imageId)
    }

    /**
     * 清除所有缓存
     */
    fun clearAll() {
        cacheMap.clear()
    }

    /**
     * 清除指定图片的缓存
     */
    fun clearCache(imageInfo: ImageInfo) {
        val imageId = generateImageId(imageInfo)
        cacheMap.remove(imageId)
    }
}
