package com.yampicture.rental.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yampicture.rental.AppViewModel
import com.yampicture.rental.R
import com.yampicture.rental.data.RentalFull
import com.yampicture.rental.data.Types
import com.yampicture.rental.export.PdfExport
import com.yampicture.rental.logic.Billing
import com.yampicture.rental.logic.RentalStatus
import com.yampicture.rental.logic.balance
import com.yampicture.rental.logic.billedDays
import com.yampicture.rental.logic.dailyTotal
import com.yampicture.rental.logic.extraDays
import com.yampicture.rental.logic.paid
import com.yampicture.rental.logic.status
import com.yampicture.rental.logic.total
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class DetailDialog { NONE, PAYMENT, RETURN, EXTEND, DELETE, REFUND }

@Composable
fun RentalDetailScreen(vm: AppViewModel, rentalId: Long, onBack: () -> Unit) {
    val rentals by vm.rentals.collectAsStateWithLifecycle()
    val now by vm.now.collectAsStateWithLifecycle()
    val settings = LocalSettings.current
    val fmt = LocalFmt.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val f = rentals.firstOrNull { it.rental.id == rentalId }
    var dialog by remember { mutableStateOf(DetailDialog.NONE) }
    var paymentToDelete by remember { mutableStateOf<Long?>(null) }

    if (f == null) {
        Column(Modifier.fillMaxSize()) {
            BackBar(stringResource(R.string.rentals), onBack)
            EmptyState(stringResource(R.string.not_found))
        }
        return
    }

    val r = f.rental
    val grace = settings.graceMs
    val status = f.status(now)
    val daily = f.dailyTotal
    val extra = f.extraDays(now, grace)
    val total = f.total(now, grace)
    val paid = f.paid
    val balance = f.balance(now, grace)
    val isOut = r.returnedAt == null

    fun shareReceipt() {
        scope.launch {
            val file = withContext(Dispatchers.IO) { PdfExport.receipt(context, settings, f, System.currentTimeMillis()) }
            PdfExport.share(context, file, context.getString(R.string.share_receipt))
        }
    }

    Column(Modifier.fillMaxSize()) {
        BackBar(
            title = stringResource(R.string.rental_number, r.id),
            onBack = onBack,
            actions = {
                IconButton(onClick = { shareReceipt() }) {
                    Icon(painterResource(R.drawable.ic_share), contentDescription = stringResource(R.string.share_receipt))
                }
                IconButton(onClick = { dialog = DetailDialog.DELETE }) {
                    Icon(painterResource(R.drawable.ic_delete), contentDescription = stringResource(R.string.delete))
                }
            },
        )

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ---------------------------------------------------------- customer
            YamCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            f.customer.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        if (f.customer.phone.isNotBlank()) {
                            Text(
                                f.customer.phone,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (f.customer.phone.isNotBlank()) {
                        IconButton(onClick = {
                            try {
                                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + f.customer.phone)))
                            } catch (_: ActivityNotFoundException) {
                            }
                        }) {
                            Icon(painterResource(R.drawable.ic_phone), contentDescription = stringResource(R.string.call))
                        }
                    }
                    StatusChip(status)
                }
            }

            if (status == RentalStatus.OVERDUE) {
                YamCard {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            painterResource(R.drawable.ic_warning),
                            contentDescription = null,
                            tint = badColor(),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                stringResource(R.string.overdue_by, durationText(now - r.dueAt)),
                                fontWeight = FontWeight.Bold,
                                color = badColor(),
                            )
                            Spacer(Modifier.height(4.dp))
                            if (extra == 0) {
                                Text(
                                    stringResource(
                                        R.string.grace_info,
                                        fmt.dateTime(r.dueAt + grace),
                                        moneyText(daily),
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            } else {
                                Text(
                                    stringResource(
                                        R.string.late_info,
                                        extra,
                                        moneyText(daily * extra),
                                        fmt.dateTime(Billing.extraDayThreshold(r.dueAt, grace, extra + 1)),
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                }
            }

            // ---------------------------------------------------------- dates
            YamCard {
                InfoRow(stringResource(R.string.picked_up), fmt.dateTime(r.startAt))
                InfoRow(stringResource(R.string.return_by), fmt.dateTime(r.dueAt))
                if (settings.ethiopianDates) {
                    InfoRow(stringResource(R.string.ethiopian_date), fmt.eth(r.dueAt))
                }
                if (r.returnedAt != null) {
                    InfoRow(stringResource(R.string.returned), fmt.dateTime(r.returnedAt), goodColor(), bold = true)
                }
            }

            // ---------------------------------------------------------- items
            SectionTitle(stringResource(R.string.equipment))
            YamCard {
                f.items.forEach { item ->
                    InfoRow(
                        item.label(settings.isAmharic),
                        stringResource(R.string.per_day, moneyText(item.dailyRate)),
                    )
                }
            }

            // ---------------------------------------------------------- billing
            SectionTitle(stringResource(R.string.billing))
            YamCard {
                InfoRow(
                    stringResource(R.string.booked_days, r.days),
                    moneyText(daily * r.days),
                )
                if (extra > 0) {
                    InfoRow(
                        stringResource(R.string.late_days_row, extra),
                        moneyText(daily * extra),
                        badColor(),
                    )
                }
                HorizontalDivider(Modifier.padding(vertical = 6.dp))
                InfoRow(stringResource(R.string.total), moneyText(total), bold = true)
                InfoRow(stringResource(R.string.paid), moneyText(paid), goodColor())
                InfoRow(
                    if (balance < 0) stringResource(R.string.overpaid) else stringResource(R.string.balance),
                    moneyText(kotlin.math.abs(balance)),
                    valueColor = when {
                        balance > 0 -> badColor()
                        else -> goodColor()
                    },
                    bold = true,
                )
            }

            // ---------------------------------------------------------- payments
            if (f.payments.isNotEmpty()) {
                SectionTitle(stringResource(R.string.payments))
                YamCard {
                    f.payments.sortedBy { it.at }.forEachIndexed { index, p ->
                        if (index > 0) HorizontalDivider(Modifier.padding(vertical = 2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(categoryLabel(p.category), style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    fmt.dateTime(p.at) + if (p.note.isNotBlank()) " · ${p.note}" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            val income = p.type == Types.INCOME
                            Text(
                                (if (income) "+" else "−") + moneyText(p.amount),
                                fontWeight = FontWeight.SemiBold,
                                color = if (income) goodColor() else badColor(),
                            )
                            IconButton(onClick = { paymentToDelete = p.id }) {
                                Icon(
                                    painterResource(R.drawable.ic_delete),
                                    contentDescription = stringResource(R.string.delete),
                                )
                            }
                        }
                    }
                }
            }

            if (r.notes.isNotBlank()) {
                SectionTitle(stringResource(R.string.notes))
                YamCard { Text(r.notes, style = MaterialTheme.typography.bodyMedium) }
            }
            if (r.returnNotes.isNotBlank()) {
                SectionTitle(stringResource(R.string.return_notes))
                YamCard { Text(r.returnNotes, style = MaterialTheme.typography.bodyMedium) }
            }
            Spacer(Modifier.height(8.dp))
        }

        // ---------------------------------------------------------- actions
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (isOut) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { dialog = DetailDialog.PAYMENT }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.add_payment))
                    }
                    OutlinedButton(onClick = { dialog = DetailDialog.EXTEND }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.change_days))
                    }
                }
                Button(
                    onClick = { dialog = DetailDialog.RETURN },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Icon(painterResource(R.drawable.ic_check), contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.mark_returned), fontWeight = FontWeight.SemiBold)
                }
            } else {
                if (balance > 0) {
                    Button(
                        onClick = { dialog = DetailDialog.PAYMENT },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    ) {
                        Text(stringResource(R.string.collect_balance, moneyText(balance)), fontWeight = FontWeight.SemiBold)
                    }
                } else if (balance < 0) {
                    OutlinedButton(
                        onClick = { dialog = DetailDialog.REFUND },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    ) {
                        Text(stringResource(R.string.refund_x, moneyText(-balance)))
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------- dialogs
    when (dialog) {
        DetailDialog.NONE -> Unit

        DetailDialog.PAYMENT -> PaymentDialog(
            initial = maxOf(0L, balance),
            onDismiss = { dialog = DetailDialog.NONE },
            onConfirm = { amount, note ->
                vm.addPayment(rentalId, amount, note)
                dialog = DetailDialog.NONE
            },
        )

        DetailDialog.RETURN -> ReturnDialog(
            f = f,
            total = total,
            paid = paid,
            balance = balance,
            onDismiss = { dialog = DetailDialog.NONE },
            onConfirm = { collect, notes ->
                vm.returnRental(rentalId, collect, notes)
                dialog = DetailDialog.NONE
            },
        )

        DetailDialog.EXTEND -> ExtendDialog(
            f = f,
            vm = vm,
            onDismiss = { dialog = DetailDialog.NONE },
        )

        DetailDialog.DELETE -> ConfirmDialog(
            title = stringResource(R.string.delete_rental_title),
            text = stringResource(R.string.delete_rental_text),
            confirmLabel = stringResource(R.string.delete),
            destructive = true,
            onConfirm = {
                dialog = DetailDialog.NONE
                vm.deleteRental(rentalId)
                onBack()
            },
            onDismiss = { dialog = DetailDialog.NONE },
        )

        DetailDialog.REFUND -> ConfirmDialog(
            title = stringResource(R.string.refund_title),
            text = stringResource(R.string.refund_text, moneyText(-balance)),
            confirmLabel = stringResource(R.string.confirm),
            onConfirm = {
                vm.refund(rentalId, -balance)
                dialog = DetailDialog.NONE
            },
            onDismiss = { dialog = DetailDialog.NONE },
        )
    }

    val deleteId = paymentToDelete
    if (deleteId != null) {
        ConfirmDialog(
            title = stringResource(R.string.delete_entry_title),
            text = stringResource(R.string.delete_entry_text),
            confirmLabel = stringResource(R.string.delete),
            destructive = true,
            onConfirm = {
                vm.deleteLedger(deleteId)
                paymentToDelete = null
            },
            onDismiss = { paymentToDelete = null },
        )
    }
}

@Composable
private fun PaymentDialog(initial: Long, onDismiss: () -> Unit, onConfirm: (Long, String) -> Unit) {
    var amount by remember { mutableStateOf(if (initial > 0) initial.toString() else "") }
    var note by remember { mutableStateOf("") }
    val value = amount.toLongOrNull() ?: 0L
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_payment)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AmountField(amount, { amount = it }, stringResource(R.string.amount))
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
            TextButton(onClick = { onConfirm(value, note) }, enabled = value > 0) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun ReturnDialog(
    f: RentalFull,
    total: Long,
    paid: Long,
    balance: Long,
    onDismiss: () -> Unit,
    onConfirm: (Boolean, String) -> Unit,
) {
    var collect by remember { mutableStateOf(balance > 0) }
    var notes by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.return_dialog_title, f.customer.name)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoRow(stringResource(R.string.total), moneyText(total), bold = true)
                InfoRow(stringResource(R.string.paid), moneyText(paid), goodColor())
                InfoRow(
                    if (balance < 0) stringResource(R.string.overpaid) else stringResource(R.string.balance),
                    moneyText(kotlin.math.abs(balance)),
                    if (balance > 0) badColor() else goodColor(),
                    bold = true,
                )
                if (balance > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = collect, onCheckedChange = { collect = it })
                        Text(stringResource(R.string.collect_now))
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    label = { Text(stringResource(R.string.return_notes)) },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(collect && balance > 0, notes) }) {
                Text(stringResource(R.string.mark_returned))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun ExtendDialog(f: RentalFull, vm: AppViewModel, onDismiss: () -> Unit) {
    val fmt = LocalFmt.current
    val scope = rememberCoroutineScope()
    var days by remember { mutableIntStateOf(f.rental.days) }
    var blocker by remember { mutableStateOf<String?>(null) }
    val newDue = f.rental.startAt + days * Billing.DAY_MS

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.change_days)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.number_of_days))
                    Stepper(days, { days = it; blocker = null }, 1, 60)
                }
                InfoRow(stringResource(R.string.return_by), fmt.dateTime(newDue), bold = true)
                InfoRow(stringResource(R.string.total), moneyText(f.dailyTotal * days))
                val b = blocker
                if (b != null) {
                    Text(
                        stringResource(R.string.extend_blocked, b),
                        color = badColor(),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    scope.launch {
                        val result = vm.extendRental(f.rental.id, days)
                        if (result == null) onDismiss() else blocker = result
                    }
                },
                enabled = days != f.rental.days,
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
