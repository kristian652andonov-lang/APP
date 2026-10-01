package com.aura.dashboard.data.apps

import android.content.Context
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.aura.dashboard.core.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Loads and memory-caches launcher icons. */
@Singleton
class AppIconLoader @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val cache = LruCache<String, ImageBitmap>(MAX_CACHED_ICONS)

    fun cached(packageName: String): ImageBitmap? = cache.get(packageName)

    suspend fun load(packageName: String, sizePx: Int): ImageBitmap? {
        cache.get(packageName)?.let { return it }
        return withContext(ioDispatcher) {
            runCatching {
                context.packageManager.getApplicationIcon(packageName)
                    .toBitmap(sizePx, sizePx)
                    .asImageBitmap()
            }.getOrNull()?.also { cache.put(packageName, it) }
        }
    }

    fun invalidate() = cache.evictAll()

    private companion object {
        const val MAX_CACHED_ICONS = 120
    }
}
