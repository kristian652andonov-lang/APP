package com.aura.dashboard.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.aura.dashboard.data.weather.DeviceLocationProvider
import com.aura.dashboard.data.weather.OpenMeteoService
import com.aura.dashboard.domain.model.UserPreferences
import com.aura.dashboard.domain.model.WeatherError
import com.aura.dashboard.domain.model.WeatherLocation
import com.aura.dashboard.testing.FakeClock
import com.aura.dashboard.testing.FakePreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.File
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WeatherRepositoryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val server = MockWebServer()
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; explicitNulls = false; isLenient = true }
    private val clock = FakeClock(Instant.parse("2026-10-02T08:00:00Z"))
    private val plovdiv = WeatherLocation("Plovdiv", null, "BG", latitude = 42.14583, longitude = 24.74972)

    private val forecast = """
        {"timezone":"Europe/Sofia","utc_offset_seconds":10800,
         "current":{"temperature_2m":18.2,"apparent_temperature":17.0,"relative_humidity_2m":55,"is_day":1,"weather_code":0,"wind_speed_10m":7.5},
         "hourly":{"time":["2026-10-02T11:00"],"temperature_2m":[18.2],"weather_code":[0],"precipitation_probability":[0],"is_day":[1]},
         "daily":{"time":["2026-10-02"],"weather_code":[0],"temperature_2m_max":[21.0],"temperature_2m_min":[9.0],
                  "sunrise":["2026-10-02T07:13"],"sunset":["2026-10-02T18:55"],"precipitation_probability_max":[0],"uv_index_max":[4.0]}}
    """.trimIndent()

    @Before
    fun setUp() {
        server.start()
        File(context.filesDir, "weather").deleteRecursively()
    }

    @After
    fun tearDown() = server.close()

    private fun TestScope.repository(prefs: FakePreferences): OpenMeteoWeatherRepository {
        val service = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(OpenMeteoService::class.java)
        val dispatcher = StandardTestDispatcher(testScheduler)
        return OpenMeteoWeatherRepository(
            context = context,
            service = service,
            preferences = prefs,
            locationProvider = DeviceLocationProvider(context, dispatcher),
            json = json,
            clock = clock,
            ioDispatcher = dispatcher,
            scope = backgroundScope,
        )
    }

    @Test
    fun `fetches with rounded coordinates, then serves the cache until it is stale`() = runTest {
        server.enqueue(MockResponse.Builder().body(forecast).build())
        val repository = repository(FakePreferences(UserPreferences(weatherLocation = plovdiv)))

        val loaded = repository.state.first { it.snapshot != null }
        assertEquals(18.2, loaded.snapshot!!.current.temperature)
        val url = server.takeRequest().url
        assertEquals("/v1/forecast", url.encodedPath)
        assertEquals("42.15", url.queryParameter("latitude"))
        assertEquals("24.75", url.queryParameter("longitude"))
        assertEquals("celsius", url.queryParameter("temperature_unit"))

        repository.refresh(force = false)
        assertEquals(1, server.requestCount)

        clock.instant = clock.instant.plus(Duration.ofMinutes(31))
        server.enqueue(MockResponse.Builder().body(forecast).build())
        repository.refresh(force = false)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `server errors back off automatic refreshes but not manual ones`() = runTest {
        server.enqueue(MockResponse.Builder().code(500).build())
        val repository = repository(FakePreferences(UserPreferences(weatherLocation = plovdiv)))

        assertEquals(WeatherError.SERVER, repository.state.first { it.error != null }.error)
        assertEquals(1, server.requestCount)

        // The every-minute check on Home must not hammer a failing server…
        clock.instant = clock.instant.plus(Duration.ofMinutes(1))
        repository.refresh(force = false)
        assertEquals(1, server.requestCount)

        // …but the refresh button always tries,
        server.enqueue(MockResponse.Builder().code(500).build())
        repository.refresh(force = true)
        assertEquals(2, server.requestCount)

        // and automatic refreshes resume after the back-off.
        clock.instant = clock.instant.plus(Duration.ofMinutes(6))
        server.enqueue(MockResponse.Builder().body(forecast).build())
        repository.refresh(force = false)
        assertEquals(3, server.requestCount)
        assertEquals(null, repository.state.value.error)
    }

    @Test
    fun `without a chosen place the country's capital is used`() = runTest {
        server.enqueue(MockResponse.Builder().body(forecast).build())
        val repository = repository(FakePreferences(UserPreferences(countryCode = "BG")))

        assertEquals("Sofia", repository.state.first { it.snapshot != null }.location?.name)
        assertEquals("42.7", server.takeRequest().url.queryParameter("latitude"))
    }
}
