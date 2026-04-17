package com.example.householdledger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.householdledger.ui.MainViewModel
import com.example.householdledger.ui.home.HomeScreen
import com.example.householdledger.ui.household.JoinHouseholdScreen
import com.example.householdledger.ui.login.LoginScreen
import com.example.householdledger.ui.navigation.Screen
import com.example.householdledger.ui.people.AddMemberScreen
import com.example.householdledger.ui.people.AddServantScreen
import com.example.householdledger.ui.people.PeopleScreen
import com.example.householdledger.ui.report.ReportsScreen
import com.example.householdledger.ui.settings.SettingsScreen
import com.example.householdledger.ui.theme.HouseholdLedgerTheme
import com.example.householdledger.ui.transaction.AddTransactionScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HouseholdLedgerTheme {
                val navController = rememberNavController()
                val currentUser by viewModel.currentUser.collectAsState()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                val bottomBarScreens = listOf(Screen.Home, Screen.Reports, Screen.People, Screen.Settings)
                val showBottomBar = currentDestination?.route in bottomBarScreens.map { it.route }

                if (currentUser != null && currentUser?.householdId == null && currentDestination?.route != Screen.JoinHousehold.route) {
                    navController.navigate(Screen.JoinHousehold.route) {
                        popUpTo(0)
                    }
                }

                Scaffold(
                    bottomBar = {
                        if (showBottomBar && currentUser?.householdId != null) {
                            NavigationBar {
                                bottomBarScreens.forEach { screen ->
                                    NavigationBarItem(
                                        icon = {
                                            Icon(
                                                when (screen) {
                                                    Screen.Home -> Icons.Default.Home
                                                    Screen.Reports -> Icons.AutoMirrored.Filled.List
                                                    Screen.People -> Icons.Default.Person
                                                    Screen.Settings -> Icons.Default.Settings
                                                    else -> Icons.Default.Home
                                                },
                                                contentDescription = null
                                            )
                                        },
                                        label = { Text(screen.route.replaceFirstChar { it.uppercase() }) },
                                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                                        onClick = {
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = if (currentUser == null) Screen.Login.route else Screen.Home.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(Screen.Login.route) {
                            LoginScreen(
                                onLoginSuccess = {
                                    navController.navigate(Screen.Home.route) {
                                        popUpTo(Screen.Login.route) { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable(Screen.JoinHousehold.route) {
                            JoinHouseholdScreen(
                                onSuccess = {
                                    navController.navigate(Screen.Home.route) {
                                        popUpTo(Screen.JoinHousehold.route) { inclusive = true }
                                    }
                                },
                                mainViewModel = viewModel
                            )
                        }
                        composable(Screen.Home.route) {
                            HomeScreen(
                                onAddTransaction = {
                                    navController.navigate(Screen.AddTransaction.route)
                                }
                            )
                        }
                        composable(Screen.Reports.route) {
                            ReportsScreen()
                        }
                        composable(Screen.People.route) {
                            PeopleScreen(
                                onAddServant = { navController.navigate(Screen.AddServant.route) },
                                onAddMember = { navController.navigate(Screen.AddMember.route) }
                            )
                        }
                        composable(Screen.Settings.route) {
                            SettingsScreen(mainViewModel = viewModel)
                        }
                        composable(Screen.AddTransaction.route) {
                            AddTransactionScreen(
                                onBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                        composable(Screen.AddServant.route) {
                            AddServantScreen(onBack = { navController.popBackStack() })
                        }
                        composable(Screen.AddMember.route) {
                            AddMemberScreen(onBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}
