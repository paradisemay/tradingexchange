package com.tradingexchange.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tradingexchange.app.ui.auth.AuthScreen
import com.tradingexchange.app.ui.chart.InstrumentChartScreen
import com.tradingexchange.app.ui.instruments.InstrumentsScreen
import com.tradingexchange.app.ui.navigation.Route
import com.tradingexchange.app.ui.orders.OrdersScreen
import com.tradingexchange.app.ui.portfolio.PortfolioScreen
import com.tradingexchange.app.ui.profile.ProfileScreen
import com.tradingexchange.app.ui.transactions.TransactionsScreen

@Composable
fun TradingExchangeApp(sessionViewModel: SessionViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val isLoggedIn by sessionViewModel.isLoggedIn.collectAsState()
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route

    LaunchedEffect(isLoggedIn) {
        val target = if (isLoggedIn) Route.Portfolio.value else Route.Auth.value
        navController.navigate(target) {
            popUpTo(0)
            launchSingleTop = true
        }
    }

    val tabs = listOf(
        Route.Portfolio to Icons.Default.Home,
        Route.Instruments to Icons.Default.Search,
        Route.Orders to Icons.Default.ShoppingCart,
        Route.Transactions to Icons.Default.History,
        Route.Profile to Icons.Default.AccountCircle,
    )

    Scaffold(
        bottomBar = {
            if (isLoggedIn && currentRoute != Route.Auth.value) {
                NavigationBar {
                    tabs.forEach { (route, icon) ->
                        NavigationBarItem(
                            selected = currentRoute == route.value,
                            onClick = { navController.navigate(route.value) { launchSingleTop = true } },
                            icon = { Icon(icon, contentDescription = route.value) },
                            label = { Text(route.value.replaceFirstChar { it.uppercase() }) },
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Route.Auth.value,
            modifier = Modifier.padding(padding),
        ) {
            composable(Route.Auth.value) { AuthScreen() }
            composable(Route.Portfolio.value) { PortfolioScreen(onOpenInstrument = { navController.navigate(Route.Instruments.value) }) }
            composable(Route.Instruments.value) {
                InstrumentsScreen(onOpenInstrument = { ticker -> navController.navigate(Route.InstrumentChart.create(ticker)) })
            }
            composable(
                route = Route.InstrumentChart.value,
                arguments = listOf(navArgument("ticker") { nullable = false }),
            ) { entry ->
                InstrumentChartScreen(
                    ticker = entry.arguments?.getString("ticker").orEmpty(),
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Route.Orders.value) { OrdersScreen() }
            composable(Route.Transactions.value) { TransactionsScreen() }
            composable(Route.Profile.value) { ProfileScreen() }
        }
    }
}
