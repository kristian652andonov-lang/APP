package com.aura.dashboard.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

data class Reminder(
    val id: Long = 0,
    val title: String,
    val description: String = "",
    /** First day the reminder fires; also the anchor for weekly/monthly repeats. */
    val startDate: LocalDate,
    val time: LocalTime,
    val repeat: Repeat = Repeat.NEVER,
    val isEnabled: Boolean = true,
    /** Next time the reminder will fire, or null when a one-time reminder has already fired. */
    val nextTriggerAt: Instant? = null,
    val lastTriggeredAt: Instant? = null,
    val createdAt: Instant = Instant.EPOCH,
) {
    val isFinished: Boolean get() = nextTriggerAt == null
}
