package com.yampicture.rental.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yampicture.rental.AppViewModel
import com.yampicture.rental.R
import com.yampicture.rental.data.RentalFull
import com.yampicture.rental.logic.Availability
import com.yampicture.rental.logic.Billing

@Composable
fun NewRentalScreen(vm: AppViewModel, onBack: () -> Unit, onCreated: (Long) -> Unit) {
    val equipment by vm.equipment.collectAsStateWithLifecycle()
    val customers by vm.customers.collectAsStateWithLifecycle()
    val rentals by vm.rentals.collectAsStateWithLifecycle()
    val now by vm.now.collectAsStateWithLifecycle()
    val settings = LocalSettings.current
    val fmt = LocalFmt.current

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var startAt by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var days by remember { mutableIntStateOf(1) }
    var selected by remember { mutableStateOf(emptyList<Long>()) }
    var deposit by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }

    val dueAt = startAt + days * Billing.DAY_MS
    val active = equipment.filter { it.active }
    val chosen = active.filter { it.id in selected }
    val daily = chosen.sumOf { it.dailyRate }
    val total = daily * days
    val depositValue = deposit.toLongOrNull() ?: 0L

    val busy: Map<Long, RentalFull> = active
        .mapNotNull { e ->
            Availability.conflict(rentals, e.id, startAt, dueAt, now)?.let { e.id to it }
        }
        .toMap()
    val selectedBusy = chosen.any { it.id in busy }

    val trimmed = name.trim()
    val suggestions = if (trimmed.isEmpty()) {
        emptyList()
    } else {
        customers.filter { it.name.contains(trimmed, ignoreCase = true) && !it.name.equals(trimmed, ignoreCase = true) }
            .take(4)
    }

    val problem: String? = when {
        trimmed.isEmpty() -> stringResource(R.string.err_name)
        chosen.isEmpty() -> stringResource(R.string.err_select_item)
        selectedBusy -> stringResource(R.string.err_item_busy)
        depositValue > total -> stringResource(R.string.err_deposit_too_big)
        else -> null
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        BackBar(stringResource(R.string.new_rental), onBack)

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ---------------------------------------------------------- customer
            SectionTitle(stringResource(R.string.customer))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.customer_name)) },
            )
            if (suggestions.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    suggestions.forEach { c ->
                        AssistChip(
                            onClick = {
                                name = c.name
                                if (c.phone.isNotBlank()) phone = c.phone
                            },
                            label = { Text(if (c.phone.isBlank()) c.name else "${c.name} · ${c.phone}") },
                        )
                    }
                }
            }
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it.filter { ch -> ch.isDigit() || ch == '+' || ch == ' ' }.take(18) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.phone)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            )

            // ---------------------------------------------------------- period
            SectionTitle(stringResource(R.string.period))
            DateTimeField(stringResource(R.string.pickup_time), startAt, { startAt = it })
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.number_of_days), style = MaterialTheme.typography.bodyLarge)
                Stepper(days, { days = it }, 1, 60)
            }
            YamCard {
                InfoRow(stringResource(R.string.return_by), fmt.dateTime(dueAt), bold = true)
                if (settings.ethiopianDates) {
                    InfoRow(stringResource(R.string.gregorian_date), fmt.greg(dueAt))
                }
            }

            // ---------------------------------------------------------- equipment
            SectionTitle(stringResource(R.string.equipment))
            YamCard {
                if (active.isEmpty()) {
                    Text(stringResource(R.string.no_equipment), style = MaterialTheme.typography.bodyMedium)
                }
                active.forEachIndexed { index, e ->
                    if (index > 0) HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    val blocker = busy[e.id]
                    val isChecked = e.id in selected
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable(enabled = blocker == null || isChecked) {
                                selected = if (isChecked) selected - e.id else selected + e.id
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = null,
                            enabled = blocker == null || isChecked,
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                e.label(settings.isAmharic),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                            )
                            if (blocker != null) {
                                val until = if (now > blocker.rental.dueAt) {
                                    stringResource(R.string.not_returned_yet)
                                } else {
                                    fmt.dateTime(blocker.rental.dueAt)
                                }
                                Text(
                                    stringResource(R.string.busy_with, blocker.customer.name, until),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = badColor(),
                                )
                            }
                        }
                        Text(
                            stringResource(R.string.per_day, moneyText(e.dailyRate)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // ---------------------------------------------------------- payment
            SectionTitle(stringResource(R.string.deposit_section))
            AmountField(
                value = deposit,
                onChange = { deposit = it },
                label = stringResource(R.string.deposit),
                isError = depositValue > total,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = { deposit = if (total > 0) (total / 2).toString() else "" },
                    label = { Text(stringResource(R.string.half)) },
                )
                AssistChip(
                    onClick = { deposit = if (total > 0) total.toString() else "" },
                    label = { Text(stringResource(R.string.full_amount)) },
                )
                AssistChip(
                    onClick = { deposit = "" },
                    label = { Text(stringResource(R.string.no_deposit)) },
                )
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                label = { Text(stringResource(R.string.notes)) },
            )

            // ---------------------------------------------------------- summary
            YamCard {
                InfoRow(stringResource(R.string.price_per_day), moneyText(daily))
                InfoRow(stringResource(R.string.days), days.toString())
                HorizontalDivider(Modifier.padding(vertical = 6.dp))
                InfoRow(stringResource(R.string.total), moneyText(total), bold = true)
                InfoRow(stringResource(R.string.deposit), moneyText(depositValue))
                InfoRow(stringResource(R.string.remaining), moneyText(maxOf(0L, total - depositValue)), bold = true)
            }
            Spacer(Modifier.height(8.dp))
        }

        Column(Modifier.padding(16.dp)) {
            if (problem != null) {
                Text(
                    problem,
                    style = MaterialTheme.typography.bodySmall,
                    color = badColor(),
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            Button(
                onClick = {
                    saving = true
                    vm.createRental(
                        customerName = trimmed,
                        phone = phone,
                        equipmentIds = selected,
                        startAt = startAt,
                        days = days,
                        deposit = depositValue,
                        notes = notes,
                        onDone = onCreated,
                    )
                },
                enabled = problem == null && !saving,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text(stringResource(R.string.save_rental), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
