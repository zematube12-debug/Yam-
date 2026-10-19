package com.yampicture.rental.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yampicture.rental.AppViewModel
import com.yampicture.rental.R
import com.yampicture.rental.data.RentalFull
import com.yampicture.rental.logic.RentalStatus
import com.yampicture.rental.logic.status

private enum class RentalFilter { ALL, ACTIVE, OVERDUE, RESERVED, RETURNED }

@Composable
fun RentalsScreen(vm: AppViewModel, onNewRental: () -> Unit, onOpenRental: (Long) -> Unit) {
    val rentals by vm.rentals.collectAsStateWithLifecycle()
    val now by vm.now.collectAsStateWithLifecycle()

    var filterName by rememberSaveable { mutableStateOf(RentalFilter.ALL.name) }
    var query by rememberSaveable { mutableStateOf("") }
    val filter = RentalFilter.valueOf(filterName)

    fun matches(f: RentalFull, flt: RentalFilter): Boolean = when (flt) {
        RentalFilter.ALL -> true
        RentalFilter.ACTIVE -> f.status(now) == RentalStatus.ACTIVE
        RentalFilter.OVERDUE -> f.status(now) == RentalStatus.OVERDUE
        RentalFilter.RESERVED -> f.status(now) == RentalStatus.RESERVED
        RentalFilter.RETURNED -> f.status(now) == RentalStatus.RETURNED
    }

    val q = query.trim()
    val visible = rentals
        .filter { matches(it, filter) }
        .filter {
            q.isEmpty() ||
                it.customer.name.contains(q, ignoreCase = true) ||
                it.customer.phone.contains(q) ||
                it.items.any { item -> item.name.contains(q, ignoreCase = true) || item.nameAm.contains(q) }
        }
        .sortedWith(
            compareBy<RentalFull> {
                // Overdue first, then active, reserved, and finally the returned history.
                when (it.status(now)) {
                    RentalStatus.OVERDUE -> 0
                    RentalStatus.ACTIVE -> 1
                    RentalStatus.RESERVED -> 2
                    RentalStatus.RETURNED -> 3
                }
            }.thenByDescending { it.rental.startAt },
        )

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader(title = stringResource(R.string.rentals))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                singleLine = true,
                label = { Text(stringResource(R.string.search_hint)) },
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items(RentalFilter.values().toList()) { flt ->
                    val label = when (flt) {
                        RentalFilter.ALL -> stringResource(R.string.filter_all)
                        RentalFilter.ACTIVE -> stringResource(R.string.status_active)
                        RentalFilter.OVERDUE -> stringResource(R.string.status_overdue)
                        RentalFilter.RESERVED -> stringResource(R.string.status_reserved)
                        RentalFilter.RETURNED -> stringResource(R.string.status_returned)
                    }
                    val count = rentals.count { matches(it, flt) }
                    FilterChip(
                        selected = filter == flt,
                        onClick = { filterName = flt.name },
                        label = { Text("$label · $count") },
                    )
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (visible.isEmpty()) {
                    item { EmptyState(stringResource(R.string.no_rentals)) }
                } else {
                    items(visible, key = { it.rental.id }) { f ->
                        RentalCard(f, now, onClick = { onOpenRental(f.rental.id) })
                    }
                }
            }
        }
        NewRentalFab(onNewRental, Modifier.align(Alignment.BottomEnd).padding(16.dp))
    }
}
