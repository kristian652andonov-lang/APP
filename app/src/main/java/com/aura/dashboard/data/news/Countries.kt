package com.aura.dashboard.data.news

import java.util.Locale

data class Country(val code: String) {
    /** Localized country name in the device language. */
    val displayName: String
        get() = Locale.Builder().setRegion(code).build().getDisplayCountry(Locale.getDefault()).ifEmpty { code }

    /** Regional indicator flag emoji built from the ISO code. */
    val flag: String
        get() = code.uppercase().map { String(Character.toChars(0x1F1E6 + (it - 'A'))) }.joinToString("")
}

object Countries {
    /** Countries supported by the NewsData.io API. Bulgaria, the default, is listed first. */
    val all: List<Country> = listOf(
        "BG", "AL", "AR", "AT", "AU", "BA", "BE", "BR", "CA", "CH", "CL", "CN", "CO", "CY", "CZ",
        "DE", "DK", "EE", "EG", "ES", "FI", "FR", "GB", "GR", "HK", "HR", "HU", "IE", "IL", "IN",
        "IS", "IT", "JP", "KR", "LT", "LU", "LV", "MD", "ME", "MK", "MT", "MX", "NG", "NL", "NO",
        "NZ", "PH", "PK", "PL", "PT", "RO", "RS", "SA", "SE", "SG", "SI", "SK", "TR", "TW", "UA",
        "AE", "US", "ZA",
    ).map(::Country)

    fun byCode(code: String): Country = all.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: Country(code.uppercase())
}
