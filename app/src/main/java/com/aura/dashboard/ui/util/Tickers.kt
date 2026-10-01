package com.aura.dashboard.ui.util

import com.aura.dashboard.core.time.AppClock
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Instant

/** Emits the current time now and at the start of every following minute. */
fun minuteTicker(clock: AppClock): Flow<Instant> = flow {
    while (true) {
        val now = clock.now()
        emit(now)
        val millisIntoMinute = now.toEpochMilli() % 60_000
        delay(60_000 - millisIntoMinute + 50)
    }
}
