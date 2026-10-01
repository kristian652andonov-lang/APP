package com.aura.dashboard.data.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import com.aura.dashboard.core.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

/**
 * Copies a picked photo into private app storage as a small JPEG, so the avatar survives
 * even if the original is deleted and no storage permission is ever needed.
 */
@Singleton
class AvatarStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val directory: File get() = File(context.filesDir, "avatar").apply { mkdirs() }

    suspend fun save(uri: Uri): String? = withContext(ioDispatcher) {
        runCatching {
            val bitmap = decode(uri) ?: return@runCatching null
            val file = File(directory, "avatar_${System.currentTimeMillis()}.jpg")
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 88, it) }
            bitmap.recycle()
            directory.listFiles()?.filter { it != file }?.forEach { it.delete() }
            file.absolutePath
        }.getOrNull()
    }

    suspend fun clear() = withContext(ioDispatcher) {
        directory.listFiles()?.forEach { it.delete() }
    }

    private fun decode(uri: Uri): Bitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // ImageDecoder applies EXIF rotation for us.
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val largest = max(info.size.width, info.size.height)
                if (largest > MAX_SIZE) {
                    val scale = MAX_SIZE.toFloat() / largest
                    decoder.setTargetSize((info.size.width * scale).toInt(), (info.size.height * scale).toInt())
                }
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIZE) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }

    private companion object {
        const val MAX_SIZE = 512
    }
}
