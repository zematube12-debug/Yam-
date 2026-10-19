package com.yampicture.rental.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.yampicture.rental.data.Equipment

@Composable
fun EquipmentScreen(vm: AppViewModel, onBack: () -> Unit) {
    val equipment by vm.equipment.collectAsStateWithLifecycle()
    val settings = LocalSettings.current

    // null = dialog closed, Equipment(id = 0) = add new
    var editing by remember { mutableStateOf<Equipment?>(null) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            BackBar(stringResource(R.string.equipment), onBack)
            Text(
                stringResource(R.string.equipment_hint),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(equipment, key = { it.id }) { e ->
                    YamCard(onClick = { editing = e }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    e.label(settings.isAmharic),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (e.active) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                                Text(
                                    stringResource(R.string.per_day, moneyText(e.dailyRate)),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = e.active,
                                onCheckedChange = { vm.saveEquipment(e.id, e.name, e.nameAm, e.dailyRate, it) },
                            )
                        }
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { editing = Equipment(id = 0, name = "", dailyRate = 0) },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
            text = { Text(stringResource(R.string.add_equipment), fontWeight = FontWeight.SemiBold) },
        )
    }

    val current = editing
    if (current != null) {
        EquipmentDialog(
            initial = current,
            onDismiss = { editing = null },
            onSave = { name, nameAm, rate ->
                vm.saveEquipment(current.id, name, nameAm, rate, current.active || current.id == 0L)
                editing = null
            },
        )
    }
}

@Composable
private fun EquipmentDialog(initial: Equipment, onDismiss: () -> Unit, onSave: (String, String, Long) -> Unit) {
    var name by remember { mutableStateOf(initial.name) }
    var nameAm by remember { mutableStateOf(initial.nameAm) }
    var rate by remember { mutableStateOf(if (initial.dailyRate > 0) initial.dailyRate.toString() else "") }
    val value = rate.toLongOrNull() ?: 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(if (initial.id == 0L) R.string.add_equipment else R.string.edit_equipment))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.name_english)) },
                )
                OutlinedTextField(
                    value = nameAm,
                    onValueChange = { nameAm = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.name_amharic)) },
                )
                AmountField(rate, { rate = it }, stringResource(R.string.daily_rate))
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, nameAm, value) }, enabled = name.isNotBlank() && value > 0) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
