package com.example.householdledger.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Home : Screen("home")
    object Reports : Screen("reports")
    object People : Screen("people")
    object Settings : Screen("settings")
    object AddTransaction : Screen("add_transaction")
    object Chat : Screen("chat")
    object Insights : Screen("insights")
    object Dairy : Screen("dairy")
}
