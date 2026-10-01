package com.aura.dashboard.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.aura.dashboard.data.preferences.UserPreferencesRepository
import com.aura.dashboard.data.repository.NewsRepository
import com.aura.dashboard.domain.model.NewsCategory
import com.aura.dashboard.domain.model.NewsError
import com.aura.dashboard.domain.model.NewsRefreshInterval
import com.aura.dashboard.domain.model.NewsRefreshResult
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@EntryPoint
@InstallIn(SingletonComponent::class)
interface NewsWorkerEntryPoint {
    fun newsRepository(): NewsRepository
    fun preferences(): UserPreferencesRepository
}

/** Keeps the cached headlines fresh in the background so the dashboard is current offline. */
class NewsRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(applicationContext, NewsWorkerEntryPoint::class.java)
        val prefs = entryPoint.preferences().current()
        if (!prefs.onboardingCompleted) return Result.success()

        val result = entryPoint.newsRepository().refresh(prefs.countryCode, NewsCategory.TOP, prefs.newsArticleCount)
        val transient = result is NewsRefreshResult.Failure &&
            result.error in setOf(NewsError.OFFLINE, NewsError.SERVER, NewsError.RATE_LIMITED)
        return if (transient && runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.success()
    }

    private companion object {
        const val MAX_ATTEMPTS = 3
    }
}

@Singleton
class NewsRefreshScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    fun apply(interval: NewsRefreshInterval) {
        val workManager = runCatching { WorkManager.getInstance(context) }.getOrNull() ?: return
        if (interval == NewsRefreshInterval.MANUAL) {
            workManager.cancelUniqueWork(WORK_NAME)
            return
        }
        val request = PeriodicWorkRequestBuilder<NewsRefreshWorker>(interval.hours, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(true)
                    .build(),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
            .build()
        workManager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    private companion object {
        const val WORK_NAME = "news-refresh"
    }
}
