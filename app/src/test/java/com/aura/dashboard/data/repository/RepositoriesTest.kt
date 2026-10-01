package com.aura.dashboard.data.repository

import android.app.AlarmManager
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.aura.dashboard.data.local.AuraDatabase
import com.aura.dashboard.domain.model.Reminder
import com.aura.dashboard.domain.model.Repeat
import com.aura.dashboard.domain.model.Task
import com.aura.dashboard.notifications.AlarmTarget
import com.aura.dashboard.notifications.AndroidAlarmScheduler
import com.aura.dashboard.testing.FakeClock
import com.aura.dashboard.testing.FakeDismisser
import com.aura.dashboard.testing.FakePreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RepositoriesTest {

    private val zone = ZoneId.of("Europe/Sofia")
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var database: AuraDatabase
    private lateinit var clock: FakeClock
    private lateinit var tasks: OfflineFirstTaskRepository
    private lateinit var reminders: OfflineFirstReminderRepository
    private val alarmManager get() = context.getSystemService(AlarmManager::class.java)

    private fun at(value: String): Instant = LocalDateTime.parse(value).atZone(zone).toInstant()

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, AuraDatabase::class.java).allowMainThreadQueries().build()
        clock = FakeClock(at("2026-10-01T09:00"), zone)
        val scheduler = AndroidAlarmScheduler(context)
        val prefs = FakePreferences()
        tasks = OfflineFirstTaskRepository(database, scheduler, FakeDismisser(), prefs, clock)
        reminders = OfflineFirstReminderRepository(database, scheduler, FakeDismisser(), prefs, clock)
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun `tasks persist and can be completed and reopened`() = runTest {
        val id = tasks.save(Task(title = "  Buy groceries  ", dueDate = LocalDate.parse("2026-10-01")))
        assertEquals("Buy groceries", tasks.getTask(id)!!.title)

        tasks.setCompleted(id, true)
        val done = tasks.getTask(id)!!
        assertTrue(done.isCompleted)
        assertEquals(clock.instant, done.completedAt)

        tasks.setCompleted(id, false)
        assertFalse(tasks.getTask(id)!!.isCompleted)
        assertNull(tasks.getTask(id)!!.completedAt)
    }

    @Test
    fun `completing a recurring task creates the next occurrence and undo removes it`() = runTest {
        val id = tasks.save(Task(title = "Gym", dueDate = LocalDate.parse("2026-10-01"), repeat = Repeat.DAILY))
        tasks.setCompleted(id, true)

        val all = tasks.observeTasks().first()
        assertEquals(2, all.size)
        val next = all.single { it.id != id }
        assertEquals(LocalDate.parse("2026-10-02"), next.dueDate)
        assertEquals(id, next.previousOccurrenceId)
        assertFalse(next.isCompleted)

        tasks.setCompleted(id, false)
        assertEquals(listOf(id), tasks.observeTasks().first().map { it.id })
    }

    @Test
    fun `clearing the date also clears time and repeat`() = runTest {
        val id = tasks.save(Task(title = "Read", dueDate = null, dueTime = LocalTime.NOON, repeat = Repeat.WEEKLY))
        val saved = tasks.getTask(id)!!
        assertNull(saved.dueTime)
        assertEquals(Repeat.NEVER, saved.repeat)
    }

    @Test
    fun `task with due time schedules an alarm`() = runTest {
        tasks.save(Task(title = "Call mom", dueDate = LocalDate.parse("2026-10-01"), dueTime = LocalTime.of(18, 0)))
        val alarm = shadowOf(alarmManager).nextScheduledAlarm
        assertNotNull(alarm)
        assertEquals(at("2026-10-01T18:00").toEpochMilli(), alarm.triggerAtTime)
    }

    @Test
    fun `reminder in the past is rejected`() = runTest {
        val result = reminders.save(Reminder(title = "Late", startDate = LocalDate.parse("2026-10-01"), time = LocalTime.of(8, 0)))
        assertIs<ReminderSaveResult.TimeInPast>(result)
    }

    @Test
    fun `saving a reminder schedules its alarm and exposes it as next`() = runTest {
        val result = reminders.save(
            Reminder(title = "Take a break", startDate = LocalDate.parse("2026-10-01"), time = LocalTime.of(18, 30)),
        )
        assertIs<ReminderSaveResult.Saved>(result)
        assertEquals("Take a break", reminders.observeNextReminder().first()?.title)
        assertEquals(at("2026-10-01T18:30").toEpochMilli(), shadowOf(alarmManager).nextScheduledAlarm!!.triggerAtTime)
    }

    @Test
    fun `firing a daily reminder advances it to tomorrow`() = runTest {
        val saved = reminders.save(
            Reminder(title = "Drink water", startDate = LocalDate.parse("2026-10-01"), time = LocalTime.of(14, 30), repeat = Repeat.DAILY),
        ) as ReminderSaveResult.Saved
        clock.instant = at("2026-10-01T14:30")
        val fired = reminders.onReminderFired(saved.id)!!
        assertEquals(at("2026-10-02T14:30"), fired.nextTriggerAt)
        assertEquals(clock.instant, fired.lastTriggeredAt)
    }

    @Test
    fun `firing a one-time reminder finishes it`() = runTest {
        val saved = reminders.save(
            Reminder(title = "Pay bills", startDate = LocalDate.parse("2026-10-01"), time = LocalTime.of(10, 0)),
        ) as ReminderSaveResult.Saved
        clock.instant = at("2026-10-01T10:00")
        val fired = reminders.onReminderFired(saved.id)!!
        assertNull(fired.nextTriggerAt)
        assertNull(reminders.observeNextReminder().first())
    }

    @Test
    fun `disabled reminders cancel their alarm and do not fire`() = runTest {
        val saved = reminders.save(
            Reminder(title = "Stretch", startDate = LocalDate.parse("2026-10-01"), time = LocalTime.of(16, 0)),
        ) as ReminderSaveResult.Saved
        reminders.setEnabled(saved.id, false)
        assertNull(shadowOf(alarmManager).nextScheduledAlarm)
        assertNull(reminders.onReminderFired(saved.id))
    }

    @Test
    fun `snooze schedules a separate alarm`() = runTest {
        val saved = reminders.save(
            Reminder(title = "Stand up", startDate = LocalDate.parse("2026-10-01"), time = LocalTime.of(16, 0), repeat = Repeat.DAILY),
        ) as ReminderSaveResult.Saved
        reminders.snooze(saved.id, Duration.ofMinutes(10))
        val triggers = shadowOf(alarmManager).scheduledAlarms.map { it.triggerAtTime }
        assertTrue(clock.instant.plus(Duration.ofMinutes(10)).toEpochMilli() in triggers)
        assertTrue(at("2026-10-01T16:00").toEpochMilli() in triggers)
    }

    @Test
    fun `alarm targets round trip through their uri`() {
        listOf(AlarmTarget.ReminderDue(7), AlarmTarget.ReminderSnooze(8), AlarmTarget.TaskDue(9)).forEach {
            assertEquals(it, AlarmTarget.fromUri(it.toUri()))
        }
    }
}
