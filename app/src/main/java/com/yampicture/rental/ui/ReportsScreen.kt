package com.yampicture.rental.ui

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
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yampicture.rental.AppViewModel
import com.yampicture.rental.R
import com.yampicture.rental.export.PdfExport
import com.yampicture.rental.logic.DateUtil
import com.yampicture.rental.logic.Reports
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ReportsScreen(vm: AppViewModel) {
    val rentals by vm.rentals.collectAsStateWithLifecycle()
    val ledger by vm.ledger.collectAsStateWithLifecycle()
    val now by vm.now.collectAsStateWithLifecycle()
    val settings = LocalSettings.current
    val fmt = LocalFmt.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var monthIndex by rememberSaveable { mutableIntStateOf(DateUtil.monthIndexOf(System.currentTimeMillis())) }
    var busy by remember { mutableStateOf(false) }

    val report = remember(monthIndex, rentals, ledger, now, settings.graceHours) {
        Reports.build(monthIndex / 12, monthIndex % 12, rentals, ledger, now, settings.graceMs)
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(title = stringResource(R.string.reports))
        MonthSwitcher(monthIndex, { monthIndex = it }, Modifier.padding(horizontal = 8.dp))

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ---------------------------------------------------------- headline numbers
            YamCard {
                Text(
                    stringResource(R.string.net_profit),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    moneyText(report.net),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (report.net >= 0) goodColor() else badColor(),
                )
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile(stringResource(R.string.income), moneyText(report.income), Modifier.weight(1f), goodColor())
                    StatTile(stringResource(R.string.expense), moneyText(report.expense), Modifier.weight(1f), badColor())
                }
            }

            // ---------------------------------------------------------- rentals
            SectionTitle(stringResource(R.string.rentals))
            YamCard {
                InfoRow(stringResource(R.string.rentals_count), report.rentalCount.toString())
                InfoRow(stringResource(R.string.billed), moneyText(report.billed))
                InfoRow(stringResource(R.string.collected), moneyText(report.paidOnRentals), goodColor())
                InfoRow(
                    stringResource(R.string.outstanding),
                    moneyText(report.outstanding),
                    if (report.outstanding > 0) badColor() else goodColor(),
                    bold = true,
                )
                InfoRow(stringResource(R.string.late_days), report.lateDays.toString())
            }

            // ---------------------------------------------------------- categories
            if (report.incomeByCategory.isNotEmpty()) {
                SectionTitle(stringResource(R.string.income_by_category))
                YamCard {
                    val max = report.incomeByCategory.maxOf { it.second }.toFloat()
                    report.incomeByCategory.forEach { (cat, value) ->
                        BarRow(categoryLabel(cat), moneyText(value), value / max, goodColor())
                    }
                }
            }
            if (report.expenseByCategory.isNotEmpty()) {
                SectionTitle(stringResource(R.string.expense_by_category))
                YamCard {
                    val max = report.expenseByCategory.maxOf { it.second }.toFloat()
                    report.expenseByCategory.forEach { (cat, value) ->
                        BarRow(categoryLabel(cat), moneyText(value), value / max, badColor())
                    }
                }
            }

            // ---------------------------------------------------------- equipment
            if (report.equipment.isNotEmpty()) {
                SectionTitle(stringResource(R.string.equipment_performance))
                YamCard {
                    val max = report.equipment.maxOf { it.revenue }.toFloat().coerceAtLeast(1f)
                    report.equipment.forEach { e ->
                        val name = if (settings.isAmharic && e.nameAm.isNotBlank()) e.nameAm else e.name
                        BarRow(
                            label = "$name · ${e.rentals}×",
                            valueText = moneyText(e.revenue),
                            fraction = e.revenue / max,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }

            // ---------------------------------------------------------- customers
            if (report.customers.isNotEmpty()) {
                SectionTitle(stringResource(R.string.top_customers))
                YamCard {
                    report.customers.take(8).forEachIndexed { index, c ->
                        if (index > 0) HorizontalDivider(Modifier.padding(vertical = 2.dp))
                        InfoRow("${c.name} · ${c.rentals}×", moneyText(c.billed))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        Button(
            onClick = {
                if (busy) return@Button
                busy = true
                scope.launch {
                    val (start, end) = DateUtil.monthBounds(report.year, report.month)
                    val entries = ledger.filter { it.at in start until end }
                    try {
                        val file = withContext(Dispatchers.IO) {
                            PdfExport.monthlyReport(context, settings, report, entries)
                        }
                        PdfExport.share(context, file, context.getString(R.string.share_report))
                    } finally {
                        busy = false
                    }
                }
            },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp),
        ) {
            Icon(painterResource(R.drawable.ic_share), contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.export_pdf, fmt.month(report.year, report.month)),
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
