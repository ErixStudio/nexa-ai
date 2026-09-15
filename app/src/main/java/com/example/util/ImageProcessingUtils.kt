package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min

data class ProcessedImage(
    val bitmap: Bitmap,
    val fileUri: Uri,
    val width: Int,
    val height: Int
)

object ImageProcessingUtils {

    // Optimal maximum dimension for financial chart analysis with Vision LLMs (Qwen-VL)
    // Preserves crisp candlestick wicks, small price labels, indicator lines, and timeframe text
    private const val MAX_CHART_DIMENSION = 1600
    private const val JPEG_QUALITY = 85

    /**
     * Creates a new temporary file in the app's cache directory for high-resolution camera capture.
     */
    fun createCameraImageUri(context: Context): Uri {
        val imageDir = File(context.cacheDir, "images").apply {
            if (!exists()) mkdirs()
        }
        val file = File(imageDir, "camera_chart_${System.currentTimeMillis()}.jpg")
        val authority = "${context.packageName}.fileprovider"
        return FileProvider.getUriForFile(context, authority, file)
    }

    /**
     * Loads, auto-rotates (based on EXIF), safely resizes, and prepares a high-quality chart image
     * from any Uri (Camera output or Gallery selection).
     * Reliably buffers the stream to local storage to prevent ContentProvider permission or multi-read issues.
     */
    suspend fun processChartImage(context: Context, sourceUri: Uri): ProcessedImage? = withContext(Dispatchers.IO) {
        var tempFile: File? = null
        try {
            // 1. Stage input stream to a local temporary cache file to avoid multiple ContentProvider stream issues
            val tempDir = File(context.cacheDir, "temp_images").apply { if (!exists()) mkdirs() }
            tempFile = File(tempDir, "raw_chart_${System.currentTimeMillis()}.tmp")

            val copied = copyUriToFile(context, sourceUri, tempFile)
            if (!copied || !tempFile.exists() || tempFile.length() <= 0L) {
                return@withContext null
            }

            // 2. Read EXIF Orientation safely from the staged file
            val orientation = readExifOrientationFromFile(tempFile)

            // 3. Decode Image Dimensions first (bounds only)
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(tempFile.absolutePath, boundsOptions)

            val origWidth = boundsOptions.outWidth
            val origHeight = boundsOptions.outHeight

            if (origWidth <= 0 || origHeight <= 0) {
                return@withContext null
            }

            // 4. Compute memory-safe sample size (keep maximum quality for Qwen-VL chart recognition)
            val maxDimension = max(origWidth, origHeight)
            var sampleSize = 1
            while ((maxDimension / sampleSize) > (MAX_CHART_DIMENSION * 1.5)) {
                sampleSize *= 2
            }

            // 5. Decode full bitmap with sample size
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val rawBitmap = BitmapFactory.decodeFile(tempFile.absolutePath, decodeOptions)
                ?: return@withContext null

            // 6. Apply EXIF Rotation & Matrix
            val rotatedBitmap = applyExifRotation(rawBitmap, orientation)

            // 7. Scale intelligently if dimensions still exceed optimal max chart dimension
            val finalBitmap = scaleIfNeeded(rotatedBitmap, MAX_CHART_DIMENSION)

            // 8. Save a clean, durable copy in cache for UI previews & Room history
            val savedUri = saveProcessedBitmapToCache(context, finalBitmap)

            ProcessedImage(
                bitmap = finalBitmap,
                fileUri = savedUri,
                width = finalBitmap.width,
                height = finalBitmap.height
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            try {
                tempFile?.delete()
            } catch (_: Exception) {}
        }
    }

    /**
     * Safely loads a Bitmap from any durable URI (FileProvider, file URI, content URI, or absolute path).
     * Used when Compose recomposition or process lifecycle needs to reload the durable image.
     */
    fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            val scheme = uri.scheme
            if (scheme == "file" || scheme == null) {
                val path = uri.path ?: uri.toString()
                val file = File(path)
                if (file.exists()) {
                    BitmapFactory.decodeFile(file.absolutePath)
                } else {
                    null
                }
            } else {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    BitmapFactory.decodeStream(input)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun copyUriToFile(context: Context, sourceUri: Uri, destinationFile: File): Boolean {
        return try {
            val scheme = sourceUri.scheme
            if (scheme == "file" || scheme == null) {
                val srcPath = sourceUri.path ?: sourceUri.toString()
                val srcFile = File(srcPath)
                if (srcFile.exists()) {
                    srcFile.copyTo(destinationFile, overwrite = true)
                    return true
                }
            }
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destinationFile).use { output ->
                    input.copyTo(output)
                    output.flush()
                }
            }
            destinationFile.exists() && destinationFile.length() > 0L
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun readExifOrientationFromFile(file: File): Int {
        return try {
            val exif = ExifInterface(file.absolutePath)
            exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } catch (e: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
    }

    /**
     * Converts a chart Bitmap to clean, high-quality Base64 string for Qwen-VL analysis.
     */
    fun bitmapToBase64(bitmap: Bitmap, quality: Int = JPEG_QUALITY): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    private fun applyExifRotation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }

        return try {
            val transformed = Bitmap.createBitmap(
                bitmap,
                0,
                0,
                bitmap.width,
                bitmap.height,
                matrix,
                true
            )
            if (transformed != bitmap) {
                bitmap.recycle()
            }
            transformed
        } catch (e: Exception) {
            bitmap
        }
    }

    private fun scaleIfNeeded(bitmap: Bitmap, maxDim: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val currentMax = max(width, height)

        if (currentMax <= maxDim) {
            return bitmap
        }

        val scaleFactor = maxDim.toFloat() / currentMax.toFloat()
        val targetWidth = (width * scaleFactor).toInt().coerceAtLeast(1)
        val targetHeight = (height * scaleFactor).toInt().coerceAtLeast(1)

        val scaled = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
        if (scaled != bitmap) {
            bitmap.recycle()
        }
        return scaled
    }

    private fun saveProcessedBitmapToCache(context: Context, bitmap: Bitmap): Uri {
        val imageDir = File(context.cacheDir, "images").apply {
            if (!exists()) mkdirs()
        }
        val file = File(imageDir, "chart_processed_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            out.flush()
        }
        val authority = "${context.packageName}.fileprovider"
        return FileProvider.getUriForFile(context, authority, file)
    }
}
