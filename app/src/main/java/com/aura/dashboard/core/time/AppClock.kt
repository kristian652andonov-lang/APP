package com.aura.dashboard.core.time

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Source of "now". The zone is read on every call so time zone changes are picked up
 * without restarting the app. Abstracted for testability.
 */
interface AppClock {
    fun now(): Instant
    fun zone(): ZoneId
    fun today(): LocalDate = now().atZone(zone()).toLocalDate()
}

@Singleton
class SystemAppClock @Inject constructor() : AppClock {
    override fun now(): Instant = Instant.now()
    override fun zone(): ZoneId = ZoneId.systemDefault()
}
