package eu.kanade.tachiyomi.data.image.coil

import android.graphics.Bitmap
import android.os.Build
import androidx.core.graphics.drawable.toDrawable
import coil.ImageLoader
import coil.decode.*
import coil.fetch.SourceResult
import coil.request.Options
import eu.kanade.tachiyomi.util.system.GLUtil
import eu.kanade.tachiyomi.util.system.ImageUtil
import okio.BufferedSource
import tachiyomi.decoder.ImageDecoder

/**
 * A [Decoder] that uses built-in [ImageDecoder] to decode images that is not supported by the system.
 */
class TachiyomiImageDecoder(private val resources: ImageSource, private val options: Options) : Decoder {

    override suspend fun decode(): DecodeResult {
        val decoder = resources.sourceOrNull()?.use {
            ImageDecoder.newInstance(it.inputStream(), options.cropBorders, displayProfile)
        }

        check(decoder != null && decoder.width > 0 && decoder.height > 0) { "Failed to initialize decoder." }

        val srcWidth = decoder.width
        val srcHeight = decoder.height

        val dstWidth = options.size.widthPx(options.scale) { srcWidth }
        val dstHeight = options.size.heightPx(options.scale) { srcHeight }

        val sampleSize = DecodeUtils.calculateInSampleSize(
            srcWidth = srcWidth,
            srcHeight = srcHeight,
            dstWidth = dstWidth,
            dstHeight = dstHeight,
            scale = options.scale,
        )

        var bitmap = decoder.decode(sampleSize = sampleSize)
        decoder.recycle()

        check(bitmap != null) { "Failed to decode image." }

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            options.bitmapConfig == Bitmap.Config.HARDWARE &&
            maxOf(bitmap.width, bitmap.height) <= GLUtil.maxTextureSize
        ) {
            val hwBitmap = bitmap.copy(Bitmap.Config.HARDWARE, false)
            if (hwBitmap != null) {
                bitmap.recycle()
                bitmap = hwBitmap
            }
        }

        if (maxOf(bitmap.width, bitmap.height) > GLUtil.maxTextureSize) {
            val widthRatio = bitmap.width / GLUtil.maxTextureSize.toFloat()
            val heightRatio = bitmap.height / GLUtil.maxTextureSize.toFloat()

            val targetWidth: Float
            val targetHeight: Float

            if (widthRatio >= heightRatio) {
                targetWidth = GLUtil.maxTextureSize.toFloat()
                targetHeight = (targetWidth / bitmap.width) * bitmap.height
            } else {
                targetHeight = GLUtil.maxTextureSize.toFloat()
                targetWidth = (targetHeight / bitmap.height) * bitmap.width
            }

            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, targetWidth.toInt(), targetHeight.toInt(), true)
            bitmap.recycle()
            bitmap = scaledBitmap
        }

        return DecodeResult(
            drawable = bitmap.toDrawable(options.context.resources),
            isSampled = sampleSize > 1,
        )
    }

    class Factory : Decoder.Factory {

        override fun create(result: SourceResult, options: Options, imageLoader: ImageLoader): Decoder? {
            if (isApplicable(result.source.source()) || options.customDecoder) return TachiyomiImageDecoder(result.source, options)
            return null
        }

        private fun isApplicable(source: BufferedSource): Boolean {
            val type = source.peek().inputStream().use {
                ImageUtil.findImageType(it)
            }
            return when (type) {
                ImageUtil.ImageType.AVIF, ImageUtil.ImageType.JXL -> true
                ImageUtil.ImageType.HEIF -> Build.VERSION.SDK_INT < Build.VERSION_CODES.O
                else -> false
            }
        }

        override fun equals(other: Any?) = other is Factory

        override fun hashCode() = javaClass.hashCode()
    }

    companion object {
        var displayProfile: ByteArray? = null
    }
}
