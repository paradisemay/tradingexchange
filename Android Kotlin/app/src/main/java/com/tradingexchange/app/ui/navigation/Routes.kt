package com.tradingexchange.app.ui.navigation

sealed class Route(val value: String) {
    data object Auth : Route("auth")
    data object Portfolio : Route("portfolio")
    data object Instruments : Route("instruments")
    data object Orders : Route("orders")
    data object Transactions : Route("transactions")
    data object Profile : Route("profile")
}
