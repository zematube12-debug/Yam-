package com.yampicture.rental.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.yampicture.rental.R
import com.yampicture.rental.logic.EthiopianCalendar

/**
 * Month calendar for the Ethiopian calendar (13 months). Weeks start on Monday (ሰኞ).
 * [onConfirm] gets the Ethiopian year, month (1-13) and day.
 */
@Composable
fun EthDatePickerDialog(
    initialMillis: Long,
    onDismiss: () -> Unit,
    onConfirm: (year: Int, month: Int, day: Int) -> Unit,
) {
    val amharic = LocalSettings.current.isAmharic
    val initial = remember(initialMillis) { EthiopianCalendar.fromMillis(initialMillis) }
    val today = remember { EthiopianCalendar.fromMillis(System.currentTimeMillis()) }

    var viewYear by remember { mutableIntStateOf(initial.year) }
    var viewMonth by remember { mutableIntStateOf(initial.month) }
    var selYear by remember { mutableIntStateOf(initial.year) }
    var selMonth by remember { mutableIntStateOf(initial.month) }
    var selDay by remember { mutableIntStateOf(initial.day) }

    fun shiftMonth(delta: Int) {
        var m = viewMonth + delta
        var y = viewYear
        if (m > 13) {
            m = 1
            y += 1
        } else if (m < 1) {
            m = 13
            y -= 1
        }
        viewMonth = m
        viewYear = y
    }

    val monthNames = if (amharic) EthiopianCalendar.monthsAm else EthiopianCalendar.monthsEn
    val weekdayHeads = if (amharic) EthiopianCalendar.weekdaysShortAm else EthiopianCalendar.weekdaysShortEn
    val daysInMonth = EthiopianCalendar.daysInMonth(viewYear, viewMonth)
    val offset = EthiopianCalendar.weekdayIndex(viewYear, viewMonth, 1)
    val weeks = (offset + daysInMonth + 6) / 7

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.padding(16.dp),
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 20.dp)) {
                Text(
                    stringResource(R.string.select_date),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "${selDay} ${monthNames[selMonth - 1]} $selYear",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { shiftMonth(-1) }) {
                        Icon(painterResource(R.drawable.ic_chevron_left), contentDescription = stringResource(R.string.previous))
                    }
                    Text(
                        "${monthNames[viewMonth - 1]} $viewYear",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    IconButton(onClick = { shiftMonth(1) }) {
                        Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = stringResource(R.string.next))
                    }
                }

                Row(Modifier.fillMaxWidth()) {
                    weekdayHeads.forEach { w ->
                        Text(
                            w,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))

                for (week in 0 until weeks) {
                    Row(Modifier.fillMaxWidth()) {
                        for (col in 0 until 7) {
                            val day = week * 7 + col - offset + 1
                            if (day < 1 || day > daysInMonth) {
                                Spacer(Modifier.weight(1f).aspectRatio(1f))
                            } else {
                                val selected = day == selDay && viewMonth == selMonth && viewYear == selYear
                                val isToday = day == today.day && viewMonth == today.month && viewYear == today.year
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(2.dp)
                                        .clip(CircleShape)
                                        .background(if (selected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent)
                                        .then(
                                            if (isToday && !selected) {
                                                Modifier.border(BorderStroke(1.dp, MaterialTheme.colorScheme.primary), CircleShape)
                                            } else {
                                                Modifier
                                            },
                                        )
                                        .clickable {
                                            selDay = day
                                            selMonth = viewMonth
                                            selYear = viewYear
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        day.toString(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = if (selected || isToday) FontWeight.Bold else FontWeight.Normal,
                                    )
                                }
                            }
                        }
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                    TextButton(onClick = { onConfirm(selYear, selMonth, selDay) }) {
                        Text(stringResource(R.string.next))
                    }
                }
            }
        }
    }
}

/**
 * Time picker for the Ethiopian clock: choose the part of the day (ጠዋት / ቀን / ማታ / ሌሊት), the Ethiopian
 * hour and the minute. [onConfirm] returns the normal 24-hour clock hour and minute.
 */
@Composable
fun EthTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour24: Int, minute: Int) -> Unit,
) {
    val amharic = LocalSettings.current.isAmharic
    var hour by remember { mutableIntStateOf(initialHour) }
    var minute by remember { mutableIntStateOf(initialMinute) }

    val period = EthiopianCalendar.periodOf(hour)
    val periodNames = if (amharic) EthiopianCalendar.periodsAm else EthiopianCalendar.periodsEn

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.padding(16.dp),
        ) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
                Text(
                    stringResource(R.string.select_time),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    EthiopianCalendar.formatTime(hour, minute, amharic),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(R.string.clock_24_hint, "%02d:%02d".format(hour, minute)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))

                // part of the day
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    periodNames.forEachIndexed { index, name ->
                        FilterChip(
                            selected = period == index,
                            onClick = {
                                // keep the same position inside the part of the day
                                val position = hour - EthiopianCalendar.periodStart[period]
                                hour = EthiopianCalendar.periodStart[index] + position
                            },
                            label = { Text(name) },
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))

                // Ethiopian hour (six choices inside each part of the day)
                Text(
                    stringResource(R.string.hour),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth()) {
                    for (k in 0 until 6) {
                        val h24 = EthiopianCalendar.periodStart[period] + k
                        val selected = hour == h24
                        Box(
                            Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                )
                                .clickable { hour = h24 },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                EthiopianCalendar.ethHour(h24).toString(),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))

                // minute
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.minute), style = MaterialTheme.typography.bodyLarge)
                    Stepper(minute, { minute = it }, 0, 59)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0, 15, 30, 45).forEach { m ->
                        FilterChip(
                            selected = minute == m,
                            onClick = { minute = m },
                            label = { Text(":%02d".format(m)) },
                        )
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                    TextButton(onClick = { onConfirm(hour, minute) }) { Text(stringResource(R.string.ok)) }
                }
            }
        }
    }
}
