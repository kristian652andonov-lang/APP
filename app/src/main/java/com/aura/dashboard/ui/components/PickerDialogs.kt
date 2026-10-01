package com.aura.dashboard.ui.components

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aura.dashboard.R
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/** Material date picker working in LocalDate (the M3 picker uses UTC midnight millis). */
@Composable
fun AuraDatePickerDialog(
    initial: LocalDate?,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    minDate: LocalDate? = null,
) {
    val minMillis = minDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
    val state = rememberDatePickerState(
        initialSelectedDateMillis = (initial ?: LocalDate.now()).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = minMillis == null || utcTimeMillis >= minMillis
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis?.let { millis ->
                        onConfirm(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                },
                enabled = state.selectedDateMillis != null,
            ) { Text(stringResource(R.string.action_done)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
        shape = RoundedCornerShape(28.dp),
    ) {
        DatePicker(state = state)
    }
}

@Composable
fun AuraTimePickerDialog(
    initial: LocalTime?,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val start = initial ?: LocalTime.now().plusHours(1).withMinute(0)
    val state = rememberTimePickerState(
        initialHour = start.hour,
        initialMinute = start.minute,
        is24Hour = DateFormat.is24HourFormat(context),
    )
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.padding(horizontal = 24.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.picker_select_time),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                )
                TimePicker(state = state)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
                    TextButton(onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) }) {
                        Text(stringResource(R.string.action_done))
                    }
                }
            }
        }
    }
}
