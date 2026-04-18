package com.example.householdledger

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.householdledger.ui.AppBootState
import com.example.householdledger.ui.MainViewModel
import com.example.householdledger.ui.category.AddCategoryScreen
import com.example.householdledger.ui.category.CategoryScreen
import com.example.householdledger.ui.chat.ChatScreen
import com.example.householdledger.ui.components.BottomNavItem
import com.example.householdledger.ui.components.BottomNavWithFab
import com.example.householdledger.ui.dairy.DairyScreen
import com.example.householdledger.ui.home.HomeScreen
import com.example.householdledger.ui.household.JoinHouseholdScreen
import com.example.householdledger.ui.insights.InsightsScreen
import com.example.householdledger.ui.login.LoginScreen
import com.example.householdledger.ui.navigation.Screen
import com.example.householdledger.ui.onboarding.OnboardingScreen
import com.example.householdledger.ui.people.AddMemberScreen
import com.example.householdledger.ui.people.AddServantScreen
import com.example.householdledger.ui.people.PeopleScreen
import com.example.householdledger.ui.pin.SetPinScreen
import com.example.householdledger.ui.recurring.AddRecurringScreen
import com.example.householdledger.ui.recurring.RecurringScreen
import com.example.householdledger.ui.report.ReportsScreen
import com.example.householdledger.ui.settings.SettingsScreen
import com.example.householdledger.ui.splash.SplashScreen
import com.example.householdledger.ui.theme.HouseholdLedgerTheme
import com.example.householdledger.ui.theme.currencyCodeToSymbol
import com.example.householdledger.ui.transaction.AddTransactionSheet
import com.example.householdledger.ui.transaction.TransactionListScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        // Hold the system splash until the ViewModel resolves the initial boot state,
        // then hand off to the in-app Compose SplashScreen for smooth continuity.
        splash.setKeepOnScreenCondition {
            viewModel.bootState.value == com.example.householdledger.ui.AppBootState.Loading
        }
        enableEdgeToEdge()
        setContent {
            val darkMode by viewModel.darkMode.collectAsState()
            val currency by viewModel.currency.collectAsState()

            HouseholdLedgerTheme(
                darkModePreference = darkMode,
                currencySymbol = currencyCodeToSymbol(currency)
            ) {
                AppContent(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppContent(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val bootState by viewModel.bootState.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val snackbarHostState = remember { SnackbarHostState() }

    val navItems = remember {
        listOf(
            BottomNavItem(Screen.Home.route, "Home", Icons.Filled.Home, Icons.Outlined.Home),
            BottomNavItem(Screen.Transactions.route, "Activity",
                Icons.AutoMirrored.Filled.ReceiptLong, Icons.AutoMirrored.Outlined.ReceiptLong),
            BottomNavItem(Screen.Reports.route, "Reports", Icons.Filled.BarChart, Icons.Outlined.BarChart),
            BottomNavItem(Screen.Settings.route, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
        )
    }
    val showBottomBar = currentRoute in navItems.map { it.route }

    var showAddSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sheetScope = androidx.compose.runtime.rememberCoroutineScope()

    // Boot routing: when boot state changes, push the right root.
    LaunchedEffect(bootState) {
        when (bootState) {
            AppBootState.Onboarding -> navController.navigate(Screen.Onboarding.route) {
                popUpTo(0); launchSingleTop = true
            }
            AppBootState.Auth -> navController.navigate(Screen.Login.route) {
                popUpTo(0); launchSingleTop = true
            }
            AppBootState.JoinHousehold -> navController.navigate(Screen.JoinHousehold.route) {
                popUpTo(0); launchSingleTop = true
            }
            AppBootState.Ready -> {
                if (currentRoute in setOf(
                        Screen.Splash.route, Screen.Login.route,
                        Screen.Onboarding.route, Screen.JoinHousehold.route
                    )
                ) {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(0); launchSingleTop = true
                    }
                }
            }
            AppBootState.Loading -> Unit
        }
    }

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
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onFabClick = { showAddSheet = true }
                )
            }
        }
    ) { innerPadding ->
        val slideIn: AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>.() -> androidx.compose.animation.EnterTransition = {
            slideInHorizontally(animationSpec = tween(260)) { it / 6 } + fadeIn(tween(220))
        }
        val slideOut: AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>.() -> androidx.compose.animation.ExitTransition = {
            slideOutHorizontally(animationSpec = tween(220)) { -it / 8 } + fadeOut(tween(180))
        }
        val popIn: AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>.() -> androidx.compose.animation.EnterTransition = {
            slideInHorizontally(animationSpec = tween(240)) { -it / 8 } + fadeIn(tween(200))
        }
        val popOut: AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>.() -> androidx.compose.animation.ExitTransition = {
            slideOutHorizontally(animationSpec = tween(200)) { it / 6 } + fadeOut(tween(180))
        }

        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = slideIn,
            exitTransition = slideOut,
            popEnterTransition = popIn,
            popExitTransition = popOut
        ) {
            composable(Screen.Splash.route) { SplashScreen() }
            composable(Screen.Onboarding.route) {
                OnboardingScreen(onFinish = { viewModel.completeOnboarding() })
            }
            composable(Screen.Login.route) {
                LoginScreen(onLoginSuccess = { /* boot state will route */ })
            }
            composable(Screen.JoinHousehold.route) {
                JoinHouseholdScreen(
                    onSuccess = { /* boot state will route */ },
                    mainViewModel = viewModel
                )
            }
            composable(Screen.Home.route) {
                HomeScreen(
                    onAddTransaction = { showAddSheet = true },
                    onNavigateToChat = { navController.navigate(Screen.Chat.route) },
                    onNavigateToInsights = { navController.navigate(Screen.Insights.route) },
                    onNavigateToDairy = { navController.navigate(Screen.Dairy.route) },
                    onNavigateToRecurring = { navController.navigate(Screen.Recurring.route) },
                    onNavigateToTransactions = { navController.navigate(Screen.Transactions.route) },
                    onNavigateToCategories = { navController.navigate(Screen.Categories.route) },
                    onNavigateToPeople = { navController.navigate(Screen.People.route) }
                )
            }
            composable(Screen.Reports.route) { ReportsScreen() }
            composable(Screen.People.route) {
                PeopleScreen(
                    onAddServant = { navController.navigate(Screen.AddServant.route) },
                    onAddMember = { navController.navigate(Screen.AddMember.route) }
                )
            }
            composable(Screen.Categories.route) {
                CategoryScreen(
                    onBack = { navController.popBackStack() },
                    onAdd = { navController.navigate(Screen.AddCategory.withId()) },
                    onEdit = { id -> navController.navigate(Screen.AddCategory.withId(id)) }
                )
            }
            composable(
                route = Screen.AddCategory.route,
                arguments = listOf(navArgument("id") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id")?.takeIf { it.isNotBlank() }
                AddCategoryScreen(
                    categoryId = id,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    mainViewModel = viewModel,
                    onOpenSetPin = { navController.navigate(Screen.SetPin.route) }
                )
            }
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
            composable(Screen.Recurring.route) {
                RecurringScreen(
                    onBack = { navController.popBackStack() },
                    onAdd = { navController.navigate(Screen.AddRecurring.route) }
                )
            }
            composable(Screen.AddRecurring.route) {
                AddRecurringScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Transactions.route) {
                TransactionListScreen(
                    onBack = null,
                    onAdd = { showAddSheet = true }
                )
            }
            composable(Screen.SetPin.route) {
                SetPinScreen(onBack = { navController.popBackStack() })
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
