package com.example.householdledger.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CurrencyExchange
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.householdledger.ui.MainViewModel
import com.example.householdledger.ui.components.AppCard
import com.example.householdledger.ui.components.Avatar
import com.example.householdledger.ui.components.SectionHeader
import com.example.householdledger.util.BiometricHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    mainViewModel: MainViewModel,
    onOpenSetPin: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val currentUser by mainViewModel.currentUser.collectAsState()
    val state by viewModel.state.collectAsState()
    val exportMessage by viewModel.exportResult.collectAsState()
    val context = LocalContext.current

    var budgetDialog by remember { mutableStateOf(false) }
    var darkDialog by remember { mutableStateOf(false) }
    var currencyDialog by remember { mutableStateOf(false) }
    var cycleDialog by remember { mutableStateOf(false) }
    var householdDialog by remember { mutableStateOf(false) }
    val household by viewModel.household.collectAsState()

    LaunchedEffect(exportMessage) {
        if (exportMessage != null) {
            // Auto-clear after a moment; shown in a subtle status row below.
            kotlinx.coroutines.delay(3000)
            viewModel.clearExportResult()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                AppCard(contentPadding = PaddingValues(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(name = currentUser?.name ?: "?", size = 64.dp)
                        Spacer(Modifier.width(18.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "SIGNED IN",
                                style = com.example.householdledger.ui.theme.EyebrowCaps,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                currentUser?.name ?: "Unknown",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(Modifier.height(6.dp))
                            Surface(
                                shape = com.example.householdledger.ui.theme.PillShape,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    (currentUser?.role ?: "member").replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }

            item { SectionHeader("Preferences") }
            item {
                AppCard(contentPadding = PaddingValues(4.dp)) {
                    Column {
                        RowItem(
                            icon = Icons.Outlined.DarkMode,
                            title = "Appearance",
                            subtitle = state.darkMode.replaceFirstChar { it.uppercase() },
                            onClick = { darkDialog = true }
                        )
                        Divider()
                        RowItem(
                            icon = Icons.Outlined.CurrencyExchange,
                            title = "Currency",
                            subtitle = state.currency,
                            onClick = { currencyDialog = true }
                        )
                        Divider()
                        ToggleRow(
                            icon = Icons.Outlined.NotificationsNone,
                            title = "Notifications",
                            subtitle = "Reminders and alerts",
                            value = state.notificationsEnabled,
                            onChange = viewModel::setNotifications
                        )
                        Divider()
                        ToggleRow(
                            icon = Icons.Outlined.Fingerprint,
                            title = "Biometric unlock",
                            subtitle = if (BiometricHelper.canAuthenticate(context))
                                "Use fingerprint or face to unlock"
                            else "Not available on this device",
                            value = state.biometricEnabled,
                            onChange = viewModel::setBiometric,
                            enabled = BiometricHelper.canAuthenticate(context)
                        )
                        Divider()
                        RowItem(
                            icon = Icons.Outlined.Savings,
                            title = "Monthly budget",
                            subtitle = if (state.monthlyBudget > 0) "%,.0f".format(state.monthlyBudget)
                            else "Not set",
                            onClick = { budgetDialog = true }
                        )
                        Divider()
                        RowItem(
                            icon = Icons.Outlined.CalendarMonth,
                            title = "Cycle start day",
                            subtitle = "Transactions roll over on day ${state.cycleStartDay} of each month",
                            onClick = { cycleDialog = true }
                        )
                    }
                }
            }

            if (currentUser?.role == "admin") {
                item { SectionHeader("Household") }
                item {
                    AppCard(contentPadding = PaddingValues(4.dp)) {
                        RowItem(
                            icon = Icons.Outlined.Home,
                            title = "Household name",
                            subtitle = household?.name ?: "Not set",
                            onClick = { householdDialog = true }
                        )
                    }
                }
            }

            item { SectionHeader("Security") }
            item {
                AppCard(contentPadding = PaddingValues(4.dp)) {
                    RowItem(
                        icon = Icons.Outlined.Badge,
                        title = "Set PIN",
                        subtitle = "Verify before sensitive actions",
                        onClick = onOpenSetPin
                    )
                }
            }

            item { SectionHeader("Data") }
            item {
                AppCard(contentPadding = PaddingValues(4.dp)) {
                    Column {
                        RowItem(
                            icon = Icons.Outlined.Download,
                            title = "Export JSON",
                            subtitle = "Backup to Downloads folder",
                            onClick = viewModel::exportJson
                        )
                        Divider()
                        RowItem(
                            icon = Icons.Outlined.Download,
                            title = "Export CSV",
                            subtitle = "Spreadsheet of transactions",
                            onClick = viewModel::exportCsv
                        )
                    }
                }
            }

            if (exportMessage != null) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            exportMessage!!,
                            modifier = Modifier.padding(14.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            item { SectionHeader("About") }
            item {
                AppCard(contentPadding = PaddingValues(4.dp)) {
                    Column {
                        RowItem(
                            icon = Icons.Outlined.Info,
                            title = "App version",
                            subtitle = "1.0.0",
                            onClick = {}
                        )
                        Divider()
                        RowItem(
                            icon = Icons.AutoMirrored.Outlined.HelpOutline,
                            title = "Help & support",
                            subtitle = null,
                            onClick = {}
                        )
                    }
                }
            }

            item {
                AppCard(
                    onClick = { viewModel.signOut() },
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    borderColor = null,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.Logout, null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(Modifier.width(14.dp))
                        Text(
                            "Sign out",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }

    if (budgetDialog) BudgetDialog(
        initial = state.monthlyBudget,
        onSave = { viewModel.setMonthlyBudget(it); budgetDialog = false },
        onDismiss = { budgetDialog = false }
    )
    if (darkDialog) OptionDialog(
        title = "Appearance",
        options = listOf("system", "light", "dark"),
        selected = state.darkMode,
        onSelect = { viewModel.setDarkMode(it); darkDialog = false },
        onDismiss = { darkDialog = false }
    )
    if (currencyDialog) OptionDialog(
        title = "Currency",
        options = listOf("PKR", "INR", "USD", "EUR", "GBP", "AED", "SAR", "BDT"),
        selected = state.currency,
        onSelect = { viewModel.setCurrency(it); currencyDialog = false },
        onDismiss = { currencyDialog = false }
    )
    if (cycleDialog) CycleStartDayDialog(
        initial = state.cycleStartDay,
        onSave = { viewModel.setCycleStartDay(it); cycleDialog = false },
        onDismiss = { cycleDialog = false }
    )
    if (householdDialog) HouseholdNameDialog(
        initial = household?.name.orEmpty(),
        onSave = { viewModel.renameHousehold(it); householdDialog = false },
        onDismiss = { householdDialog = false }
    )
}

@Composable
private fun HouseholdNameDialog(initial: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename household") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(40) },
                label = { Text("Household name") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(text) },
                enabled = text.isNotBlank() && text != initial
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun CycleStartDayDialog(initial: Int, onSave: (Int) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(initial.toString()) }
    val parsed = text.toIntOrNull()
    val isValid = parsed != null && parsed in 1..31
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cycle start day") },
        text = {
            Column {
                Text(
                    "Choose the day your monthly cycle begins (e.g. 28 if you're paid on the 28th). " +
                    "For months without that day, the cycle ends on the last day instead.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.filter { c -> c.isDigit() }.take(2) },
                    label = { Text("Day (1–31)") },
                    singleLine = true,
                    isError = !isValid && text.isNotEmpty(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { parsed?.let { onSave(it.coerceIn(1, 31)) } },
                enabled = isValid
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun Divider() {
    androidx.compose.material3.HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.padding(start = 68.dp)
    )
}

@Composable
private fun RowItem(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                icon, null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(9.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (trailing != null) trailing()
    }
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    value: Boolean,
    onChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    RowItem(
        icon = icon,
        title = title,
        subtitle = subtitle,
        onClick = { if (enabled) onChange(!value) },
        trailing = { Switch(checked = value, onCheckedChange = onChange, enabled = enabled) }
    )
}

@Composable
private fun BudgetDialog(initial: Double, onSave: (Double) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(if (initial > 0) initial.toInt().toString() else "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Monthly budget") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Amount") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(text.toDoubleOrNull() ?: 0.0) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun OptionDialog(
    title: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { opt ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clickable { onSelect(opt) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = opt == selected,
                            onClick = { onSelect(opt) }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(opt.replaceFirstChar { it.uppercase() })
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
