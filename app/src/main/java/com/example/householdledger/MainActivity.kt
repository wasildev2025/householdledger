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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.householdledger.ui.components.BottomNavItem
import com.example.householdledger.ui.components.BottomNavWithFab
import com.example.householdledger.ui.home.HomeScreen
import com.example.householdledger.ui.login.LoginScreen
import com.example.householdledger.ui.navigation.Screen
import com.example.householdledger.ui.people.PeopleScreen
import com.example.householdledger.ui.report.ReportsScreen
import com.example.householdledger.ui.settings.SettingsScreen
import com.example.householdledger.ui.theme.HouseholdLedgerTheme
import com.example.householdledger.ui.transaction.AddTransactionScreen
import com.example.householdledger.ui.chat.ChatScreen
import com.example.householdledger.ui.insights.InsightsScreen
import com.example.householdledger.ui.dairy.DairyScreen
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Settings
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.householdledger.ui.MainViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HouseholdLedgerTheme {
                        composable(Screen.Home.route) {
                            HomeScreen(onAddTransaction = { showAddSheet = true })
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
