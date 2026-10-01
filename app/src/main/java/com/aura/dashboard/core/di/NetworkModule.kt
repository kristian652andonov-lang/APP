package com.aura.dashboard.core.di

import android.content.Context
import android.util.Xml
import com.aura.dashboard.BuildConfig
import com.aura.dashboard.data.news.NewsApiConfig
import com.aura.dashboard.data.news.NewsDataProvider
import com.aura.dashboard.data.news.api.NewsDataService
import com.aura.dashboard.data.news.rss.RssNewsProvider
import com.aura.dashboard.data.news.rss.RssParser
import com.aura.dashboard.data.repository.NewsProviderChain
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
        isLenient = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(@ApplicationContext context: Context): OkHttpClient = OkHttpClient.Builder()
        .cache(Cache(context.cacheDir.resolve("http_cache"), 10L * 1024 * 1024))
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request()
            val withAgent = if (request.header("User-Agent") == null) {
                request.newBuilder().header("User-Agent", USER_AGENT).build()
            } else {
                request
            }
            chain.proceed(withAgent)
        }
        .build()

    @Provides
    @Singleton
    fun provideNewsApiConfig(): NewsApiConfig = NewsApiConfig(
        baseUrl = BuildConfig.NEWS_API_BASE_URL,
        apiKey = BuildConfig.NEWS_API_KEY,
    )

    @Provides
    @Singleton
    fun provideNewsDataService(client: OkHttpClient, json: Json, config: NewsApiConfig): NewsDataService =
        Retrofit.Builder()
            .baseUrl(config.effectiveBaseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(NewsDataService::class.java)

    @Provides
    @Singleton
    fun provideNewsProviderChain(
        config: NewsApiConfig,
        service: NewsDataService,
        client: OkHttpClient,
        json: Json,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): NewsProviderChain {
        val rss = RssNewsProvider(client, RssParser { Xml.newPullParser() }, ioDispatcher)
        val api = if (config.isConfigured) NewsDataProvider(service, config, json) else null
        return NewsProviderChain(listOfNotNull(api, rss))
    }

    private val USER_AGENT = "Mozilla/5.0 (Linux; Android) Aura/${BuildConfig.VERSION_NAME}"
}
