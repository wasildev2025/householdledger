package com.example.householdledger.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object Transactions : Screen("transactions")
    object Reports : Screen("reports")
    object People : Screen("people")
    object Categories : Screen("categories")
    object Settings : Screen("settings")
    object AddTransaction : Screen("add_transaction")
    object AddServant : Screen("add_servant")
    object AddMember : Screen("add_member")
    object AddCategory : Screen("add_category")
    object JoinHousehold : Screen("join_household")
    object DairyTracker : Screen("dairy_tracker")
    object Messages : Screen("messages")
}
