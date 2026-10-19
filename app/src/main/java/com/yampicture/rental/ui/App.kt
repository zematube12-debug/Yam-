package com.yampicture.rental.ui

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.yampicture.rental.AppViewModel
import com.yampicture.rental.R
import com.yampicture.rental.util.Fmt
import com.yampicture.rental.util.LocalizedContext

private class Tab(val route: String, val label: Int, val icon: Int)

private val tabs = listOf(
    Tab("home", R.string.tab_home, R.drawable.ic_home),
    Tab("rentals", R.string.rentals, R.drawable.ic_list),
    Tab("money", R.string.money, R.drawable.ic_wallet),
    Tab("reports", R.string.reports, R.drawable.ic_chart),
)

@Composable
fun YamApp(vm: AppViewModel, pendingRoute: MutableState<String?>) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val base = LocalContext.current
    val localized = remember(base, settings.language) { LocalizedContext(base, settings.language) }
    val fmt = remember(settings.language, settings.ethiopianDates) {
        Fmt(settings.language, settings.ethiopianDates)
    }

    CompositionLocalProvider(
        LocalContext provides localized,
        LocalConfiguration provides localized.resources.configuration,
        LocalSettings provides settings,
        LocalFmt provides fmt,
    ) {
        YamTheme {
            AppNav(vm, pendingRoute)
        }
    }
}

@Composable
private fun AppNav(vm: AppViewModel, pendingRoute: MutableState<String?>) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val showBar = tabs.any { it.route == route }

    fun goTab(target: String) {
        nav.navigate(target) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = { goTab(tab.route) },
                            icon = { Icon(painterResource(tab.icon), contentDescription = null) },
                            label = { Text(stringResource(tab.label)) },
                        )
                    }
                }
            }
        },
    ) { inner ->
        NavHost(
            navController = nav,
            startDestination = "home",
            modifier = Modifier.padding(inner).consumeWindowInsets(inner),
        ) {
            composable("home") {
                HomeScreen(
                    vm = vm,
                    onNewRental = { nav.navigate("new_rental") },
                    onOpenRental = { nav.navigate("rental/$it") },
                    onOpenEquipment = { nav.navigate("equipment") },
                    onOpenSettings = { nav.navigate("settings") },
                )
            }
            composable("rentals") {
                RentalsScreen(
                    vm = vm,
                    onNewRental = { nav.navigate("new_rental") },
                    onOpenRental = { nav.navigate("rental/$it") },
                )
            }
            composable("money") {
                MoneyScreen(vm = vm, onOpenRental = { nav.navigate("rental/$it") })
            }
            composable("reports") {
                ReportsScreen(vm = vm)
            }
            composable("new_rental") {
                NewRentalScreen(
                    vm = vm,
                    onBack = { nav.popBackStack() },
                    onCreated = { id ->
                        nav.navigate("rental/$id") {
                            popUpTo("new_rental") { inclusive = true }
                        }
                    },
                )
            }
            composable(
                route = "rental/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                RentalDetailScreen(
                    vm = vm,
                    rentalId = entry.arguments?.getLong("id") ?: 0L,
                    onBack = { nav.popBackStack() },
                )
            }
            composable("equipment") {
                EquipmentScreen(vm = vm, onBack = { nav.popBackStack() })
            }
            composable("settings") {
                SettingsScreen(vm = vm, onBack = { nav.popBackStack() })
            }
        }
    }

    // Opening the app from a notification jumps straight to the rental or the report.
    val pending = pendingRoute.value
    LaunchedEffect(pending) {
        if (pending != null) {
            when {
                pending == "reports" -> goTab("reports")
                pending.startsWith("rental/") -> nav.navigate(pending)
            }
            pendingRoute.value = null
        }
    }
}
