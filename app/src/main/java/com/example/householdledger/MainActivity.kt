package com.example.householdledger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.householdledger.ui.MainViewModel
import com.example.householdledger.ui.chat.ChatScreen
import com.example.householdledger.ui.components.BottomNavItem
import com.example.householdledger.ui.components.BottomNavWithFab
import com.example.householdledger.ui.dairy.DairyScreen
import com.example.householdledger.ui.home.HomeScreen
import com.example.householdledger.ui.household.JoinHouseholdScreen
import com.example.householdledger.ui.insights.InsightsScreen
import com.example.householdledger.ui.login.LoginScreen
import com.example.householdledger.ui.navigation.Screen
import com.example.householdledger.ui.people.AddMemberScreen
import com.example.householdledger.ui.people.AddServantScreen
import com.example.householdledger.ui.people.PeopleScreen
import com.example.householdledger.ui.report.ReportsScreen
import com.example.householdledger.ui.settings.SettingsScreen
import com.example.householdledger.ui.theme.HouseholdLedgerTheme
import com.example.householdledger.ui.transaction.AddTransactionSheet
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HouseholdLedgerTheme {
                val navController = rememberNavController()
                val currentUser by viewModel.currentUser.collectAsState()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val snackbarHostState = remember { SnackbarHostState() }

                val navItems = remember {
                    listOf(
                        BottomNavItem(Screen.Home.route, "Home", Icons.Filled.Home, Icons.Outlined.Home),
                        BottomNavItem(Screen.Reports.route, "Reports", Icons.Filled.BarChart, Icons.Outlined.BarChart),
                        BottomNavItem(Screen.People.route, "People", Icons.Filled.People, Icons.Outlined.People),
                        BottomNavItem(Screen.Settings.route, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
                    )
                }
                val showBottomBar = currentRoute in navItems.map { it.route }

                if (currentUser != null && currentUser?.householdId == null && currentRoute != Screen.JoinHousehold.route) {
                    navController.navigate(Screen.JoinHousehold.route) {
                        popUpTo(0)
                    }
                }

                var showAddSheet by remember { mutableStateOf(false) }
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                val sheetScope = rememberCoroutineScope()

                Scaffold(
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    containerColor = MaterialTheme.colorScheme.background,
                    bottomBar = {
                        if (showBottomBar) {
                            BottomNavWithFab(
                                items = navItems,
                                currentRoute = currentRoute,
                                onSelect = { route ->
                                    navController.navigate(route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                onFabClick = { showAddSheet = true }
                            )
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
                                onAddTransaction = { showAddSheet = true },
                                onNavigateToChat = { navController.navigate(Screen.Chat.route) },
                                onNavigateToInsights = { navController.navigate(Screen.Insights.route) },
                                onNavigateToDairy = { navController.navigate(Screen.Dairy.route) }
                            )
                        }
                        composable(Screen.Reports.route) { ReportsScreen() }
                        composable(Screen.People.route) {
                            PeopleScreen(
                                onAddServant = { navController.navigate(Screen.AddServant.route) },
                                onAddMember = { navController.navigate(Screen.AddMember.route) }
                            )
                        }
                        composable(Screen.Settings.route) { SettingsScreen(mainViewModel = viewModel) }
                        composable(Screen.AddServant.route) {
                            AddServantScreen(onBack = { navController.popBackStack() })
                        }
                        composable(Screen.AddMember.route) {
                            AddMemberScreen(onBack = { navController.popBackStack() })
                        }
                        composable(Screen.Chat.route) {
                            ChatScreen(onNavigateBack = { navController.popBackStack() })
                        }
                        composable(Screen.Insights.route) {
                            InsightsScreen(onNavigateBack = { navController.popBackStack() })
                        }
                        composable(Screen.Dairy.route) {
                            DairyScreen(onNavigateBack = { navController.popBackStack() })
                        }
                    }

                    if (showAddSheet) {
                        ModalBottomSheet(
                            onDismissRequest = { showAddSheet = false },
                            sheetState = sheetState,
                            containerColor = MaterialTheme.colorScheme.surface,
                            dragHandle = { androidx.compose.material3.BottomSheetDefaults.DragHandle() }
                        ) {
                            AddTransactionSheet(
                                onClose = {
                                    sheetScope.launch { sheetState.hide() }.invokeOnCompletion {
                                        showAddSheet = false
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
