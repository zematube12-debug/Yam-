package com.yampicture.rental.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.yampicture.rental.R
import com.yampicture.rental.data.Cat
import com.yampicture.rental.data.RentalFull
import com.yampicture.rental.logic.Billing
import com.yampicture.rental.logic.EthiopianCalendar
import com.yampicture.rental.logic.RentalStatus
import com.yampicture.rental.logic.balance
import com.yampicture.rental.logic.status
import com.yampicture.rental.logic.total
import com.yampicture.rental.util.DateTools
import java.util.Calendar

// ------------------------------------------------------------------ cards and tiles

@Composable
fun YamCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    val border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    val shape = MaterialTheme.shapes.large
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            colors = colors,
            border = border,
        ) {
            Column(Modifier.padding(16.dp), content = content)
        }
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            colors = colors,
            border = border,
        ) {
            Column(Modifier.padding(16.dp), content = content)
        }
    }
}

@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.Unspecified,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun StatusChip(status: RentalStatus) {
    val (container, content) = statusPalette(status)
    Surface(shape = RoundedCornerShape(50), color = container) {
        Text(
            statusLabel(status),
            color = content,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier.padding(top = 8.dp, bottom = 2.dp),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun EmptyState(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 24.dp), contentAlignment = Alignment.Center) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun InfoRow(label: String, value: String, valueColor: Color = Color.Unspecified, bold: Boolean = false) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
            color = valueColor,
        )
    }
}

@Composable
fun BarRow(label: String, valueText: String, fraction: Float, color: Color) {
    Column(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(8.dp))
            Text(valueText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction.coerceIn(0.02f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(color),
            )
        }
    }
}

// ------------------------------------------------------------------ headers

@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        actions()
    }
}

@Composable
fun BackBar(title: String, onBack: () -> Unit, actions: @Composable RowScope.() -> Unit = {}) {
    TopAppBar(
        title = { Text(title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.back))
            }
        },
        actions = actions,
        windowInsets = WindowInsets(0, 0, 0, 0),
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
    )
}

@Composable
fun MonthSwitcher(monthIndex: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val fmt = LocalFmt.current
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { onChange(monthIndex - 1) }) {
            Icon(painterResource(R.drawable.ic_chevron_left), contentDescription = stringResource(R.string.previous))
        }
        Text(
            fmt.month(monthIndex / 12, monthIndex % 12),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        IconButton(onClick = { onChange(monthIndex + 1) }) {
            Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = stringResource(R.string.next))
        }
    }
}

// ------------------------------------------------------------------ inputs

@Composable
fun AmountField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.filter { c -> c.isDigit() }.take(9)) },
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        suffix = { Text(stringResource(R.string.currency)) },
        singleLine = true,
        isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
}

@Composable
fun Stepper(value: Int, onChange: (Int) -> Unit, min: Int, max: Int, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        FilledTonalIconButton(onClick = { if (value > min) onChange(value - 1) }, enabled = value > min) {
            Text("−", style = MaterialTheme.typography.titleLarge)
        }
        Text(
            value.toString(),
            modifier = Modifier.width(56.dp),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        FilledTonalIconButton(onClick = { if (value < max) onChange(value + 1) }, enabled = value < max) {
            Text("+", style = MaterialTheme.typography.titleLarge)
        }
    }
}

/** Tappable box that shows a date and time and opens a date picker followed by a time picker. */
@Composable
fun DateTimeField(label: String, value: Long, onChange: (Long) -> Unit, modifier: Modifier = Modifier) {
    val fmt = LocalFmt.current
    val ethiopian = LocalSettings.current.ethiopianDates
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    var pickedDay by remember { mutableLongStateOf(0L) }

    Surface(
        onClick = { showDate = true },
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(fmt.dateTime(value), style = MaterialTheme.typography.bodyLarge)
        }
    }

    if (showDate) {
        if (ethiopian) {
            EthDatePickerDialog(
                initialMillis = value,
                onDismiss = { showDate = false },
                onConfirm = { y, m, d ->
                    val g = EthiopianCalendar.toGregorian(y, m, d)
                    pickedDay = DateTools.utcDay(g.first, g.second - 1, g.third)
                    showDate = false
                    showTime = true
                },
            )
        } else {
            val state = rememberDatePickerState(initialSelectedDateMillis = DateTools.localToUtcDay(value))
            DatePickerDialog(
                onDismissRequest = { showDate = false },
                confirmButton = {
                    TextButton(onClick = {
                        val picked = state.selectedDateMillis
                        showDate = false
                        if (picked != null) {
                            pickedDay = picked
                            showTime = true
                        }
                    }) { Text(stringResource(R.string.next)) }
                },
                dismissButton = {
                    TextButton(onClick = { showDate = false }) { Text(stringResource(R.string.cancel)) }
                },
            ) {
                DatePicker(state = state)
            }
        }
    }

    if (showTime) {
        val c = Calendar.getInstance()
        c.timeInMillis = value
        if (ethiopian) {
            EthTimePickerDialog(
                initialHour = c.get(Calendar.HOUR_OF_DAY),
                initialMinute = c.get(Calendar.MINUTE),
                onDismiss = { showTime = false },
                onConfirm = { h, m ->
                    onChange(DateTools.combine(pickedDay, h, m))
                    showTime = false
                },
            )
        } else {
            val timeState = rememberTimePickerState(
                initialHour = c.get(Calendar.HOUR_OF_DAY),
                initialMinute = c.get(Calendar.MINUTE),
                is24Hour = true,
            )
            Dialog(
                onDismissRequest = { showTime = false },
                properties = DialogProperties(usePlatformDefaultWidth = false),
            ) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    modifier = Modifier.padding(16.dp),
                ) {
                    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        TimePicker(state = timeState)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { showTime = false }) { Text(stringResource(R.string.cancel)) }
                            TextButton(onClick = {
                                onChange(DateTools.combine(pickedDay, timeState.hour, timeState.minute))
                                showTime = false
                            }) { Text(stringResource(R.string.ok)) }
                        }
                    }
                }
            }
        }
    }
}

/** Date-only field for the money ledger. Picking today keeps the current time; other days use noon. */
@Composable
fun DateField(label: String, value: Long, onChange: (Long) -> Unit, modifier: Modifier = Modifier) {
    val fmt = LocalFmt.current
    val ethiopian = LocalSettings.current.ethiopianDates
    var show by remember { mutableStateOf(false) }

    Surface(
        onClick = { show = true },
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(fmt.date(value), style = MaterialTheme.typography.bodyLarge)
        }
    }

    if (show) {
        fun finish(utcDay: Long) {
            val now = System.currentTimeMillis()
            val noon = DateTools.combine(utcDay, 12, 0)
            onChange(if (DateTools.sameDay(noon, now)) now else noon)
        }
        if (ethiopian) {
            EthDatePickerDialog(
                initialMillis = value,
                onDismiss = { show = false },
                onConfirm = { y, m, d ->
                    val g = EthiopianCalendar.toGregorian(y, m, d)
                    show = false
                    finish(DateTools.utcDay(g.first, g.second - 1, g.third))
                },
            )
        } else {
            val state = rememberDatePickerState(initialSelectedDateMillis = DateTools.localToUtcDay(value))
            DatePickerDialog(
                onDismissRequest = { show = false },
                confirmButton = {
                    TextButton(onClick = {
                        val picked = state.selectedDateMillis
                        show = false
                        if (picked != null) finish(picked)
                    }) { Text(stringResource(R.string.ok)) }
                },
                dismissButton = {
                    TextButton(onClick = { show = false }) { Text(stringResource(R.string.cancel)) }
                },
            ) {
                DatePicker(state = state)
            }
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = if (destructive) badColor() else MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

// ------------------------------------------------------------------ labels

@Composable
fun statusLabel(status: RentalStatus): String = stringResource(
    when (status) {
        RentalStatus.RESERVED -> R.string.status_reserved
        RentalStatus.ACTIVE -> R.string.status_active
        RentalStatus.OVERDUE -> R.string.status_overdue
        RentalStatus.RETURNED -> R.string.status_returned
    },
)

@Composable
fun categoryLabel(category: String): String = stringResource(
    when (category) {
        Cat.DEPOSIT -> R.string.cat_deposit
        Cat.PAYMENT -> R.string.cat_payment
        Cat.SHOOT -> R.string.cat_shoot
        Cat.OTHER_INCOME -> R.string.cat_other_income
        Cat.REPAIR -> R.string.cat_repair
        Cat.PURCHASE -> R.string.cat_purchase
        Cat.TRANSPORT -> R.string.cat_transport
        Cat.SALARY -> R.string.cat_salary
        Cat.BILLS -> R.string.cat_bills
        Cat.MARKETING -> R.string.cat_marketing
        Cat.REFUND -> R.string.cat_refund
        else -> R.string.cat_other_expense
    },
)

@Composable
fun durationText(ms: Long): String {
    val total = maxOf(0L, ms)
    val days = (total / Billing.DAY_MS).toInt()
    val hours = ((total % Billing.DAY_MS) / Billing.HOUR_MS).toInt()
    val minutes = ((total % Billing.HOUR_MS) / 60_000L).toInt()
    return when {
        days > 0 -> stringResource(R.string.dur_days_hours, days, hours)
        hours > 0 -> stringResource(R.string.dur_hours_minutes, hours, minutes)
        else -> stringResource(R.string.dur_minutes, minutes)
    }
}

@Composable
fun moneyText(n: Long): String = stringResource(R.string.money_fmt, LocalFmt.current.num(n))

// ------------------------------------------------------------------ rental card

@Composable
fun RentalCard(f: RentalFull, now: Long, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val settings = LocalSettings.current
    val fmt = LocalFmt.current
    val status = f.status(now)
    val total = f.total(now, settings.graceMs)
    val balance = f.balance(now, settings.graceMs)
    val r = f.rental

    val timing = when (status) {
        RentalStatus.OVERDUE -> stringResource(R.string.overdue_by, durationText(now - r.dueAt))
        RentalStatus.ACTIVE -> stringResource(R.string.due_in, durationText(r.dueAt - now), fmt.dateTime(r.dueAt))
        RentalStatus.RESERVED -> stringResource(R.string.starts_on, fmt.dateTime(r.startAt))
        RentalStatus.RETURNED -> stringResource(R.string.returned_on, fmt.dateTime(r.returnedAt ?: r.dueAt))
    }

    YamCard(modifier, onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    f.customer.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    f.items.joinToString(", ") { it.label(settings.isAmharic) },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            StatusChip(status)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            timing,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = if (status == RentalStatus.OVERDUE) badColor() else MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                stringResource(R.string.total_label, moneyText(total)),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (balance > 0) {
                Text(
                    stringResource(R.string.balance_due, moneyText(balance)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = badColor(),
                    fontWeight = FontWeight.SemiBold,
                )
            } else {
                Text(
                    stringResource(R.string.fully_paid),
                    style = MaterialTheme.typography.bodyMedium,
                    color = goodColor(),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

/** A small square marker, used as a legend dot. */
@Composable
fun Dot(color: Color, size: Int = 10) {
    Box(Modifier.size(size.dp).clip(RoundedCornerShape(50)).background(color))
}
