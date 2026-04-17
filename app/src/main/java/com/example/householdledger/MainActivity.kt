package com.example.householdledger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
                    containerColor = MaterialTheme.colorScheme.background,
                    bottomBar = {
                        if (showBottomBar && currentUser?.householdId != null) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 0.dp
                            ) {
                                bottomBarScreens.forEach { screen ->
                                    val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                                    val (filledIcon, outlinedIcon, label) = when (screen) {
                                        Screen.Home -> Triple(Icons.Filled.Home, Icons.Outlined.Home, "Home")
                                        Screen.Reports -> Triple(Icons.Filled.PieChart, Icons.Outlined.PieChart, "Reports")
                                        Screen.People -> Triple(Icons.Filled.Person, Icons.Outlined.Person, "People")
                                        Screen.Settings -> Triple(Icons.Filled.Settings, Icons.Outlined.Settings, "Settings")
                                        else -> Triple(Icons.Filled.Home, Icons.Outlined.Home, "Home")
                                    }
                                    NavigationBarItem(
                                        icon = {
                                            Icon(
                                                if (isSelected) filledIcon else outlinedIcon,
                                                contentDescription = null
                                            )
                                        },
                                        label = {
                                            Text(
                                                label,
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        },
                                        selected = isSelected,
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
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
