package com.yampicture.rental.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yampicture.rental.AppViewModel
import com.yampicture.rental.R
import com.yampicture.rental.data.Equipment
import com.yampicture.rental.data.RentalFull
import com.yampicture.rental.logic.DateUtil
import com.yampicture.rental.logic.RentalStatus
import com.yampicture.rental.logic.Reports
import com.yampicture.rental.logic.status
import java.util.Calendar

@Composable
fun NewRentalFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
        text = { Text(stringResource(R.string.new_rental), fontWeight = FontWeight.SemiBold) },
    )
}

private class EquipmentState(val equipment: Equipment, val holder: RentalFull?, val upcoming: RentalFull?)

@Composable
fun HomeScreen(
    vm: AppViewModel,
    onNewRental: () -> Unit,
    onOpenRental: (Long) -> Unit,
    onOpenEquipment: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val rentals by vm.rentals.collectAsStateWithLifecycle()
    val equipment by vm.equipment.collectAsStateWithLifecycle()
    val ledger by vm.ledger.collectAsStateWithLifecycle()
    val now by vm.now.collectAsStateWithLifecycle()
    val settings = LocalSettings.current
    val fmt = LocalFmt.current

    val overdue = rentals.filter { it.status(now) == RentalStatus.OVERDUE }.sortedBy { it.rental.dueAt }
    val dueToday = rentals
        .filter { it.status(now) == RentalStatus.ACTIVE && it.rental.dueAt <= DateUtil.endOfDay(now) }
        .sortedBy { it.rental.dueAt }
    val outCount = rentals.count {
        val s = it.status(now)
        s == RentalStatus.ACTIVE || s == RentalStatus.OVERDUE
    }
    val reservedCount = rentals.count { it.status(now) == RentalStatus.RESERVED }
    val attention = overdue + dueToday

    val cal = Calendar.getInstance().apply { timeInMillis = now }
    val report = remember(rentals, ledger, now) {
        Reports.build(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), rentals, ledger, now, settings.graceMs)
    }

    val states = equipment.filter { it.active }.map { e ->
        EquipmentState(
            equipment = e,
            holder = rentals.firstOrNull { f ->
                f.rental.returnedAt == null && f.rental.startAt <= now && f.items.any { it.equipmentId == e.id }
            },
            upcoming = rentals
                .filter { f ->
                    f.rental.returnedAt == null && f.rental.startAt > now && f.items.any { it.equipmentId == e.id }
                }
                .minByOrNull { it.rental.startAt },
        )
    }

    val subtitle = fmt.day(now)

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader(
                title = settings.shopName,
                subtitle = subtitle,
                actions = {
                    IconButton(onClick = onOpenEquipment) {
                        Icon(painterResource(R.drawable.ic_camera), contentDescription = stringResource(R.string.equipment))
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(painterResource(R.drawable.ic_settings), contentDescription = stringResource(R.string.settings))
                    }
                },
            )
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatTile(stringResource(R.string.out_now), outCount.toString(), Modifier.weight(1f))
                        StatTile(
                            stringResource(R.string.status_overdue),
                            overdue.size.toString(),
                            Modifier.weight(1f),
                            valueColor = if (overdue.isNotEmpty()) badColor() else Color.Unspecified,
                        )
                        StatTile(stringResource(R.string.status_reserved), reservedCount.toString(), Modifier.weight(1f))
                    }
                }
                item {
                    SectionTitle(stringResource(R.string.this_month, fmt.month(report.year, report.month)))
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatTile(stringResource(R.string.income), moneyText(report.income), Modifier.weight(1f), goodColor())
                        StatTile(stringResource(R.string.expense), moneyText(report.expense), Modifier.weight(1f), badColor())
                        StatTile(
                            stringResource(R.string.net),
                            moneyText(report.net),
                            Modifier.weight(1f),
                            if (report.net >= 0) goodColor() else badColor(),
                        )
                    }
                }
                item { SectionTitle(stringResource(R.string.needs_attention)) }
                if (attention.isEmpty()) {
                    item {
                        YamCard {
                            Text(
                                stringResource(R.string.all_clear),
                                style = MaterialTheme.typography.bodyMedium,
                                color = goodColor(),
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                } else {
                    items(attention, key = { it.rental.id }) { f ->
                        RentalCard(f, now, onClick = { onOpenRental(f.rental.id) })
                    }
                }
                item { SectionTitle(stringResource(R.string.equipment_status)) }
                item {
                    YamCard {
                        if (states.isEmpty()) {
                            Text(stringResource(R.string.no_equipment), style = MaterialTheme.typography.bodyMedium)
                        }
                        states.forEachIndexed { index, s ->
                            if (index > 0) Spacer(Modifier.height(12.dp))
                            EquipmentStatusRow(s)
                        }
                    }
                }
            }
        }
        NewRentalFab(onNewRental, Modifier.align(Alignment.BottomEnd).padding(16.dp))
    }
}

@Composable
private fun EquipmentStatusRow(s: EquipmentState) {
    val settings = LocalSettings.current
    val fmt = LocalFmt.current
    val now = System.currentTimeMillis()
    val holder = s.holder
    val upcoming = s.upcoming

    val dotColor = when {
        holder == null -> goodColor()
        now > holder.rental.dueAt -> badColor()
        else -> MaterialTheme.colorScheme.secondary
    }
    val detail = when {
        holder != null && now > holder.rental.dueAt ->
            stringResource(R.string.eq_overdue, holder.customer.name, fmt.dateTime(holder.rental.dueAt))
        holder != null ->
            stringResource(R.string.eq_out, holder.customer.name, fmt.dateTime(holder.rental.dueAt))
        upcoming != null -> stringResource(R.string.eq_free_until, fmt.dateTime(upcoming.rental.startAt))
        else -> stringResource(R.string.eq_free)
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Dot(dotColor, 12)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                s.equipment.label(settings.isAmharic),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            moneyText(s.equipment.dailyRate),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
