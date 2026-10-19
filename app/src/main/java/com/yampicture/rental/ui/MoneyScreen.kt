package com.yampicture.rental.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yampicture.rental.AppViewModel
import com.yampicture.rental.R
import com.yampicture.rental.data.Cat
import com.yampicture.rental.data.LedgerEntry
import com.yampicture.rental.data.Types
import com.yampicture.rental.logic.DateUtil

private enum class MoneyFilter { ALL, INCOME, EXPENSE }

@Composable
fun MoneyScreen(vm: AppViewModel, onOpenRental: (Long) -> Unit) {
    val ledger by vm.ledger.collectAsStateWithLifecycle()
    val fmt = LocalFmt.current

    var monthIndex by rememberSaveable { mutableIntStateOf(DateUtil.monthIndexOf(System.currentTimeMillis())) }
    var filterName by rememberSaveable { mutableStateOf(MoneyFilter.ALL.name) }
    var showAdd by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<LedgerEntry?>(null) }
    val filter = MoneyFilter.valueOf(filterName)

    val (start, end) = DateUtil.monthBounds(monthIndex / 12, monthIndex % 12)
    val inMonth = ledger.filter { it.at in start until end }
    val income = inMonth.filter { it.type == Types.INCOME }.sumOf { it.amount }
    val expense = inMonth.filter { it.type == Types.EXPENSE }.sumOf { it.amount }
    val net = income - expense

    val visible = inMonth.filter {
        when (filter) {
            MoneyFilter.ALL -> true
            MoneyFilter.INCOME -> it.type == Types.INCOME
            MoneyFilter.EXPENSE -> it.type == Types.EXPENSE
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader(title = stringResource(R.string.money))
            MonthSwitcher(monthIndex, { monthIndex = it }, Modifier.padding(horizontal = 8.dp))

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatTile(stringResource(R.string.income), moneyText(income), Modifier.weight(1f), goodColor())
                StatTile(stringResource(R.string.expense), moneyText(expense), Modifier.weight(1f), badColor())
                StatTile(
                    stringResource(R.string.net),
                    moneyText(net),
                    Modifier.weight(1f),
                    if (net >= 0) goodColor() else badColor(),
                )
            }

            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MoneyFilter.values().forEach { flt ->
                    FilterChip(
                        selected = filter == flt,
                        onClick = { filterName = flt.name },
                        label = {
                            Text(
                                when (flt) {
                                    MoneyFilter.ALL -> stringResource(R.string.filter_all)
                                    MoneyFilter.INCOME -> stringResource(R.string.income)
                                    MoneyFilter.EXPENSE -> stringResource(R.string.expense)
                                },
                            )
                        },
                    )
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp, top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (visible.isEmpty()) {
                    item { EmptyState(stringResource(R.string.no_transactions)) }
                } else {
                    items(visible, key = { it.id }) { e ->
                        LedgerRow(
                            e = e,
                            onOpenRental = onOpenRental,
                            onDelete = { toDelete = e },
                            dateText = fmt.date(e.at),
                        )
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { showAdd = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
            text = { Text(stringResource(R.string.add_transaction), fontWeight = FontWeight.SemiBold) },
        )
    }

    if (showAdd) {
        AddTransactionDialog(
            onDismiss = { showAdd = false },
            onSave = { type, category, amount, at, note ->
                vm.addLedger(type, category, amount, at, note)
                showAdd = false
                // Jump to the month the new entry belongs to so the user sees it immediately.
                monthIndex = DateUtil.monthIndexOf(at)
            },
        )
    }

    val del = toDelete
    if (del != null) {
        ConfirmDialog(
            title = stringResource(R.string.delete_entry_title),
            text = stringResource(R.string.delete_entry_text),
            confirmLabel = stringResource(R.string.delete),
            destructive = true,
            onConfirm = {
                vm.deleteLedger(del.id)
                toDelete = null
            },
            onDismiss = { toDelete = null },
        )
    }
}

@Composable
private fun LedgerRow(
    e: LedgerEntry,
    onOpenRental: (Long) -> Unit,
    onDelete: () -> Unit,
    dateText: String,
) {
    val income = e.type == Types.INCOME
    val rentalId = e.rentalId
    YamCard(onClick = if (rentalId != null) ({ onOpenRental(rentalId) }) else null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(categoryLabel(e.category), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                val detail = buildList {
                    add(dateText)
                    if (rentalId != null) add(stringResource(R.string.rental_number, rentalId))
                    if (e.note.isNotBlank()) add(e.note)
                }.joinToString(" · ")
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                (if (income) "+" else "−") + moneyText(e.amount),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = if (income) goodColor() else badColor(),
            )
            IconButton(onClick = onDelete) {
                Icon(painterResource(R.drawable.ic_delete), contentDescription = stringResource(R.string.delete))
            }
        }
    }
}

@Composable
private fun AddTransactionDialog(
    onDismiss: () -> Unit,
    onSave: (type: String, category: String, amount: Long, at: Long, note: String) -> Unit,
) {
    var type by remember { mutableStateOf(Types.EXPENSE) }
    var category by remember { mutableStateOf(Cat.OTHER_EXPENSE) }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var at by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val value = amount.toLongOrNull() ?: 0L

    // Rental money is recorded automatically from the rental screen; only manual categories are offered here.
    val categories = if (type == Types.INCOME) {
        listOf(Cat.SHOOT, Cat.OTHER_INCOME)
    } else {
        Cat.expense.filter { it != Cat.REFUND }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_transaction)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == Types.INCOME,
                        onClick = {
                            type = Types.INCOME
                            category = Cat.SHOOT
                        },
                        label = { Text(stringResource(R.string.income)) },
                    )
                    FilterChip(
                        selected = type == Types.EXPENSE,
                        onClick = {
                            type = Types.EXPENSE
                            category = Cat.OTHER_EXPENSE
                        },
                        label = { Text(stringResource(R.string.expense)) },
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    categories.forEach { c ->
                        FilterChip(
                            selected = category == c,
                            onClick = { category = c },
                            label = { Text(categoryLabel(c)) },
                        )
                    }
                }
                AmountField(amount, { amount = it }, stringResource(R.string.amount))
                DateField(stringResource(R.string.date), at, { at = it })
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.note)) },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(type, category, value, at, note) }, enabled = value > 0) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
