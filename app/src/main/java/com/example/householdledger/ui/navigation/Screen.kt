package com.example.householdledger.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Login : Screen("login")
    object JoinHousehold : Screen("join_household")
    object Home : Screen("home")
    object Reports : Screen("reports")
    object People : Screen("people")
    object Categories : Screen("categories")
    object AddCategory : Screen("add_category?id={id}") {
        fun withId(id: String? = null) = if (id == null) "add_category?id=" else "add_category?id=$id"
    }
    object Settings : Screen("settings")
    object AddTransaction : Screen("add_transaction")
    object Transactions : Screen("transactions")
    object AddServant : Screen("add_servant")
    object AddMember : Screen("add_member")
    object ServantDetail : Screen("servant_detail/{id}") {
        fun withId(id: String) = "servant_detail/$id"
    }
    object Chat : Screen("chat")
    object Insights : Screen("insights")
    object Dairy : Screen("dairy")
    object Recurring : Screen("recurring")
    object AddRecurring : Screen("add_recurring")
    object SetPin : Screen("set_pin")
}
