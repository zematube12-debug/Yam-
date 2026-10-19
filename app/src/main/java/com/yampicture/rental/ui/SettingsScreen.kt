package com.yampicture.rental.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.yampicture.rental.AppViewModel
import com.yampicture.rental.R
import com.yampicture.rental.notify.Notifier

@Composable
fun SettingsScreen(vm: AppViewModel, onBack: () -> Unit) {
    val settings = LocalSettings.current
    val context = LocalContext.current
    var shopName by remember { mutableStateOf(settings.shopName) }
    var shopPhone by remember { mutableStateOf(settings.shopPhone) }

    Column(Modifier.fillMaxSize()) {
        BackBar(stringResource(R.string.settings), onBack)

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ---------------------------------------------------------- language
            SectionTitle(stringResource(R.string.language))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = settings.language == "am",
                    onClick = { vm.updateSettings { it.copy(language = "am") } },
                    label = { Text("አማርኛ") },
                )
                FilterChip(
                    selected = settings.language == "en",
                    onClick = { vm.updateSettings { it.copy(language = "en") } },
                    label = { Text("English") },
                )
            }

            // ---------------------------------------------------------- late rule
            SectionTitle(stringResource(R.string.late_rules))
            YamCard {
                Text(
                    stringResource(R.string.late_rules_explain, settings.graceHours),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.grace_hours), modifier = Modifier.weight(1f))
                    Stepper(settings.graceHours, { v -> vm.updateSettings { it.copy(graceHours = v) } }, 0, 24)
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.warn_hours), modifier = Modifier.weight(1f))
                    Stepper(settings.leadHours, { v -> vm.updateSettings { it.copy(leadHours = v) } }, 0, 12)
                }
                Text(
                    stringResource(R.string.hours_unit_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // ---------------------------------------------------------- dates
            SectionTitle(stringResource(R.string.dates))
            YamCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.show_ethiopian), fontWeight = FontWeight.Medium)
                        Text(
                            stringResource(R.string.show_ethiopian_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = settings.ethiopianDates,
                        onCheckedChange = { v -> vm.updateSettings { it.copy(ethiopianDates = v) } },
                    )
                }
            }

            // ---------------------------------------------------------- studio
            SectionTitle(stringResource(R.string.studio_info))
            OutlinedTextField(
                value = shopName,
                onValueChange = {
                    shopName = it
                    vm.updateSettings { s -> s.copy(shopName = it) }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.studio_name)) },
            )
            OutlinedTextField(
                value = shopPhone,
                onValueChange = {
                    shopPhone = it
                    vm.updateSettings { s -> s.copy(shopPhone = it) }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.studio_phone)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            )
            Text(
                stringResource(R.string.studio_info_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // ---------------------------------------------------------- notifications
            SectionTitle(stringResource(R.string.notifications))
            OutlinedButton(
                onClick = {
                    Notifier.ensureChannels(context)
                    Notifier.post(
                        context,
                        id = 1,
                        channel = Notifier.CH_ALERTS,
                        title = context.getString(R.string.test_notification_title),
                        text = context.getString(R.string.test_notification_text),
                        route = null,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.send_test_notification))
            }

            Text(
                stringResource(R.string.app_version, "1.0"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
            )
        }
    }
}
