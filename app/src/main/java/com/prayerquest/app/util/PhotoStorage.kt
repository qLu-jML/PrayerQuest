package com.prayerquest.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * App-private photo storage + compression utility.
 *
 * All user photos (Photo Prayers, Gratitude entries, Answered-Prayer
 * testimonies) MUST go through this util so that:
 *   1. Files live in filesDir/<category> — never MediaStore, never external.
 *   2. Images are downsampled + re-encoded as JPEG with a consistent quality
 *      budget, so a user snapping a 12MP phone photo doesn't cost us 4MB
 *      of storage per prayer.
 *   3. EXIF orientation is baked into the pixels and then stripped, so no
 *      downstream viewer has to re-apply the rotation and no location/
 *      device metadata leaks with the file.
 *   4. Every file can be deleted with [deletePhoto] without worrying about
 *      where on disk it lives.
 *
 * **Privacy invariant:** nothing in this file talks to the network. Group
 * sharing and any future cloud sync are opt-in features that live elsewhere
 * and must never pass these paths over the wire — see [PrayerItem.photoUri].
 */
object PhotoStorage {

    /** Max pixel dimension (longer edge). Bigger is re-sampled down. */
    private const val MAX_DIMENSION = 1280

    /** JPEG quality — 80 is the sweet spot for photo-like content. */
    private const val JPEG_QUALITY = 80

    /** Sub-directory under filesDir for each photo kind. */
    enum class Category(internal val dirName: String) {
        PRAYER_ITEM("prayer_photos"),
        GRATITUDE("gratitude_photos"),
        TESTIMONY("testimony_photos"),
    }

    /**
     * Reads [sourceUri] via the supplied ContentResolver, compresses it, and
     * writes the result into app-private storage under the given [category].
     *
     * Returns the absolute file path of the stored JPEG, or null if the
     * source could not be decoded.
     */
    suspend fun savePhoto(
        context: Context,
        sourceUri: Uri,
        category: Category,
    ): String? = withContext(Dispatchers.IO) {
        try {
            val bitmap = decodeAndDownsample(context, sourceUri) ?: return@withContext null
            val oriented = applyExifOrientation(context, sourceUri, bitmap)
            val outFile = allocateFile(context, category)
            FileOutputStream(outFile).use { out ->
                oriented.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            }
            // Free decoded bitmaps — they can be ~5MB each.
            if (oriented !== bitmap) bitmap.recycle()
            oriented.recycle()
            outFile.absolutePath
        } catch (t: Throwable) {
            null
        }
    }

    /**
     * Deletes the stored photo at [absolutePath]. Safe to call with a null
     * path or a path that no longer exists — returns true if the file was
     * actually deleted, false otherwise.
     */
    fun deletePhoto(absolutePath: String?): Boolean {
        if (absolutePath.isNullOrBlank()) return false
        return try {
            val f = File(absolutePath)
            if (f.exists()) f.delete() else false
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Counts photo files across every category. Used by the 200-photo free
     * tier cap. Cheap enough to call on the hot path — it's a filesystem
     * `listFiles()` on at most three small directories.
     */
    fun countAllPhotos(context: Context): Int {
        var total = 0
        for (category in Category.entries) {
            val dir = File(context.filesDir, category.dirName)
            if (dir.exists() && dir.isDirectory) {
                total += dir.listFiles()?.size ?: 0
            }
        }
        return total
    }

    // ─────────────────────────────────────────────────────────────────────
    // Internals
    // ─────────────────────────────────────────────────────────────────────

    private fun allocateFile(context: Context, category: Category): File {
        val dir = File(context.filesDir, category.dirName).apply { mkdirs() }
        return File(dir, "${UUID.randomUUID()}.jpg")
    }

    private fun decodeAndDownsample(context: Context, sourceUri: Uri): Bitmap? {
        val cr = context.contentResolver

        // First pass: read bounds only so we know the downsample factor.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        cr.openInputStream(sourceUri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            ?: return null
        val srcW = bounds.outWidth
        val srcH = bounds.outHeight
        if (srcW <= 0 || srcH <= 0) return null

        val longer = maxOf(srcW, srcH)
        var sample = 1
        while (longer / sample > MAX_DIMENSION) sample *= 2

        // Second pass: actual decode at the chosen sample size.
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = cr.openInputStream(sourceUri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        } ?: return null

        // Scale down again if inSampleSize couldn't land exactly at the cap.
        val newLonger = maxOf(decoded.width, decoded.height)
        if (newLonger <= MAX_DIMENSION) return decoded
        val scale = MAX_DIMENSION.toFloat() / newLonger
        val scaled = Bitmap.createScaledBitmap(
            decoded,
            (decoded.width * scale).toInt().coerceAtLeast(1),
            (decoded.height * scale).toInt().coerceAtLeast(1),
            true
        )
        if (scaled !== decoded) decoded.recycle()
        return scaled
    }

    /**
     * Reads EXIF orientation from the original stream and returns a bitmap
     * rotated/flipped so the pixel data matches what the user saw in the
     * camera preview. Doing this up-front means we can ship a plain JPEG
     * with no EXIF, which also strips any GPS or device fingerprint the
     * camera app may have attached.
     */
    private fun applyExifOrientation(
        context: Context,
        sourceUri: Uri,
        src: Bitmap,
    ): Bitmap {
        val orientation = try {
            context.contentResolver.openInputStream(sourceUri)?.use {
                ExifInterface(it).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (_: Throwable) {
            ExifInterface.ORIENTATION_NORMAL
        }
        if (orientation == ExifInterface.ORIENTATION_NORMAL ||
            orientation == ExifInterface.ORIENTATION_UNDEFINED
        ) {
            return src
        }
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f); matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f); matrix.postScale(-1f, 1f)
            }
        }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
    }
}
