package com.aura.dashboard.data.weather

import com.aura.dashboard.domain.model.WeatherLocation

/** Default weather location until the user picks a city or shares their location. */
object CapitalCities {
    private val capitals: Map<String, Triple<String, Double, Double>> = mapOf(
        "BG" to Triple("Sofia", 42.70, 23.32), "AL" to Triple("Tirana", 41.33, 19.82),
        "AR" to Triple("Buenos Aires", -34.60, -58.38), "AT" to Triple("Vienna", 48.21, 16.37),
        "AU" to Triple("Canberra", -35.28, 149.13), "BA" to Triple("Sarajevo", 43.86, 18.41),
        "BE" to Triple("Brussels", 50.85, 4.35), "BR" to Triple("Brasília", -15.79, -47.88),
        "CA" to Triple("Ottawa", 45.42, -75.70), "CH" to Triple("Bern", 46.95, 7.45),
        "CL" to Triple("Santiago", -33.45, -70.67), "CN" to Triple("Beijing", 39.90, 116.41),
        "CO" to Triple("Bogotá", 4.71, -74.07), "CY" to Triple("Nicosia", 35.19, 33.38),
        "CZ" to Triple("Prague", 50.08, 14.44), "DE" to Triple("Berlin", 52.52, 13.40),
        "DK" to Triple("Copenhagen", 55.68, 12.57), "EE" to Triple("Tallinn", 59.44, 24.75),
        "EG" to Triple("Cairo", 30.04, 31.24), "ES" to Triple("Madrid", 40.42, -3.70),
        "FI" to Triple("Helsinki", 60.17, 24.94), "FR" to Triple("Paris", 48.86, 2.35),
        "GB" to Triple("London", 51.51, -0.13), "GR" to Triple("Athens", 37.98, 23.73),
        "HK" to Triple("Hong Kong", 22.32, 114.17), "HR" to Triple("Zagreb", 45.81, 15.98),
        "HU" to Triple("Budapest", 47.50, 19.04), "IE" to Triple("Dublin", 53.35, -6.26),
        "IL" to Triple("Jerusalem", 31.77, 35.21), "IN" to Triple("New Delhi", 28.61, 77.21),
        "IS" to Triple("Reykjavík", 64.15, -21.94), "IT" to Triple("Rome", 41.90, 12.50),
        "JP" to Triple("Tokyo", 35.68, 139.69), "KR" to Triple("Seoul", 37.57, 126.98),
        "LT" to Triple("Vilnius", 54.69, 25.28), "LU" to Triple("Luxembourg", 49.61, 6.13),
        "LV" to Triple("Riga", 56.95, 24.11), "MD" to Triple("Chișinău", 47.01, 28.86),
        "ME" to Triple("Podgorica", 42.43, 19.26), "MK" to Triple("Skopje", 42.00, 21.43),
        "MT" to Triple("Valletta", 35.90, 14.51), "MX" to Triple("Mexico City", 19.43, -99.13),
        "NG" to Triple("Abuja", 9.08, 7.40), "NL" to Triple("Amsterdam", 52.37, 4.90),
        "NO" to Triple("Oslo", 59.91, 10.75), "NZ" to Triple("Wellington", -41.29, 174.78),
        "PH" to Triple("Manila", 14.60, 120.98), "PK" to Triple("Islamabad", 33.68, 73.05),
        "PL" to Triple("Warsaw", 52.23, 21.01), "PT" to Triple("Lisbon", 38.72, -9.14),
        "RO" to Triple("Bucharest", 44.43, 26.10), "RS" to Triple("Belgrade", 44.79, 20.45),
        "SA" to Triple("Riyadh", 24.71, 46.68), "SE" to Triple("Stockholm", 59.33, 18.07),
        "SG" to Triple("Singapore", 1.35, 103.82), "SI" to Triple("Ljubljana", 46.06, 14.51),
        "SK" to Triple("Bratislava", 48.15, 17.11), "TR" to Triple("Ankara", 39.93, 32.86),
        "TW" to Triple("Taipei", 25.03, 121.57), "UA" to Triple("Kyiv", 50.45, 30.52),
        "AE" to Triple("Abu Dhabi", 24.45, 54.38), "US" to Triple("Washington", 38.91, -77.04),
        "ZA" to Triple("Pretoria", -25.75, 28.19),
    )

    fun forCountry(countryCode: String): WeatherLocation? =
        capitals[countryCode.uppercase()]?.let { (name, lat, lon) ->
            WeatherLocation(name = name, countryCode = countryCode.uppercase(), latitude = lat, longitude = lon)
        }
}
