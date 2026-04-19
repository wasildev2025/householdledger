package com.example.householdledger.ui.dairy

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.householdledger.data.model.DairyLog
import com.example.householdledger.ui.components.AppCard
import com.example.householdledger.ui.components.EmptyState
import com.example.householdledger.ui.components.LiquidFill
import com.example.householdledger.ui.components.MoneyText
import com.example.householdledger.ui.components.MoneyTone
import com.example.householdledger.ui.components.SectionHeader
import com.example.householdledger.ui.theme.EyebrowCaps
import com.example.householdledger.ui.theme.MoneyHero
import com.example.householdledger.ui.theme.PillShape
import com.example.householdledger.util.DateUtil
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val MilkColor = Color(0xFF5EA9D6)
private val YogurtColor = Color(0xFFE8C9A0)

private val dairyDateFormat = DateTimeFormatter.ofPattern("EEE, d MMM")
private val monthHeaderFormat = DateTimeFormatter.ofPattern("MMMM yyyy")

private fun formatLogDate(raw: String): String {
    val d = DateUtil.parseDate(raw) ?: return raw
    val today = LocalDate.now()
    return when (d) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> d.format(dairyDateFormat)
    }
}

private enum class EntryMode { Quantity, Amount }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DairyScreen(
    onNavigateBack: () -> Unit,
    viewModel: DairyViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    var entryMode by remember { mutableStateOf(EntryMode.Quantity) }
    var milkQty by remember { mutableDoubleStateOf(0.0) }
    var yogurtQty by remember { mutableDoubleStateOf(0.0) }
    var milkAmount by remember { mutableStateOf("") }
    var yogurtAmount by remember { mutableStateOf("") }

    var priceDialog by remember { mutableStateOf(false) }
    var editingLog by remember { mutableStateOf<DairyLog?>(null) }

    // Translate whichever mode is active into (milkQty, yogurtQty).
    val (effectiveMilk, effectiveYogurt) = when (entryMode) {
        EntryMode.Quantity -> milkQty to yogurtQty
        EntryMode.Amount -> {
            val mPrice = if (state.milkPrice > 0) state.milkPrice else 1.0
            val yPrice = if (state.yogurtPrice > 0) state.yogurtPrice else 1.0
            val m = (milkAmount.toDoubleOrNull() ?: 0.0) / mPrice
            val y = (yogurtAmount.toDoubleOrNull() ?: 0.0) / yPrice
            m to y
        }
    }
    val projectedBill =
        (effectiveMilk * state.milkPrice) + (effectiveYogurt * state.yogurtPrice)
    val canSubmit = effectiveMilk > 0 || effectiveYogurt > 0

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Dairy", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { priceDialog = true }) {
                        Icon(Icons.Outlined.Tune, contentDescription = "Adjust prices")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 4.dp,
                bottom = 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                MonthlyBillCard(
                    state = state,
                    onPrev = { viewModel.shiftMonth(-1) },
                    onNext = { viewModel.shiftMonth(1) }
                )
            }

            // Logging card is only shown when viewing the current month OR any earlier
            // month (so historical entries can be backfilled). Future months hidden
            // because canGoForward already blocks navigation there.
            item {
                LogEntryCard(
                    entryMode = entryMode,
                    onModeChange = { entryMode = it },
                    milkQty = milkQty,
                    yogurtQty = yogurtQty,
                    onMilkQtyChange = { milkQty = it },
                    onYogurtQtyChange = { yogurtQty = it },
                    milkAmount = milkAmount,
                    yogurtAmount = yogurtAmount,
                    onMilkAmountChange = { milkAmount = it.filter { c -> c.isDigit() || c == '.' } },
                    onYogurtAmountChange = { yogurtAmount = it.filter { c -> c.isDigit() || c == '.' } },
                    milkPrice = state.milkPrice,
                    yogurtPrice = state.yogurtPrice,
                    projectedBill = projectedBill,
                    canSubmit = canSubmit,
                    onSubmit = {
                        viewModel.addLog(effectiveMilk, effectiveYogurt)
                        milkQty = 0.0
                        yogurtQty = 0.0
                        milkAmount = ""
                        yogurtAmount = ""
                    }
                )
            }

            item { SectionHeader(title = "History") }

            if (state.monthLogs.isEmpty()) {
                item {
                    AppCard(tonal = true) {
                        EmptyState(
                            icon = Icons.Outlined.Check,
                            title = "No entries this month",
                            description = "Scroll back to browse prior months, or log a new entry above."
                        )
                    }
                }
            } else {
                item {
                    DairyHistoryCard(
                        logs = state.monthLogs,
                        onEdit = { editingLog = it }
                    )
                }
            }
        }
    }

    if (priceDialog) {
        PriceEditorDialog(
            initialMilk = state.milkPrice,
            initialYogurt = state.yogurtPrice,
            onSave = { m, y ->
                viewModel.updatePrices(m, y)
                priceDialog = false
            },
            onDismiss = { priceDialog = false }
        )
    }

    editingLog?.let { log ->
        EditEntryDialog(
            log = log,
            milkPrice = state.milkPrice,
            yogurtPrice = state.yogurtPrice,
            onSave = { milk, yog, date ->
                viewModel.updateLog(log, milk, yog, date)
                editingLog = null
            },
            onDelete = {
                viewModel.deleteLog(log)
                editingLog = null
            },
            onDismiss = { editingLog = null }
        )
    }
}

// ────────── Monthly Bill Card ──────────

@Composable
private fun MonthlyBillCard(
    state: DairyUiState,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    AppCard(contentPadding = PaddingValues(20.dp)) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MiniChevron(
                    icon = Icons.Default.ChevronLeft,
                    enabled = true,
                    description = "Previous month",
                    onClick = onPrev
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "MONTHLY DAIRY BILL",
                        style = EyebrowCaps,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        state.selectedMonth.format(monthHeaderFormat),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
                MiniChevron(
                    icon = Icons.Default.ChevronRight,
                    enabled = state.canGoForward,
                    description = "Next month",
                    onClick = onNext
                )
            }
            Spacer(Modifier.height(12.dp))
            MoneyText(
                amount = state.monthlyBill,
                style = MoneyHero.copy(
                    fontSize = androidx.compose.ui.unit.TextUnit.Unspecified
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DairyStat(
                    label = "Milk",
                    value = "%.1f L".format(state.monthlyMilkQty),
                    subtitle = "%.0f/L".format(state.milkPrice),
                    dotColor = MilkColor,
                    modifier = Modifier.weight(1f)
                )
                DairyStat(
                    label = "Yogurt",
                    value = "%.1f kg".format(state.monthlyYogurtQty),
                    subtitle = "%.0f/kg".format(state.yogurtPrice),
                    dotColor = YogurtColor,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MiniChevron(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        enabled = enabled,
        modifier = Modifier.size(36.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = description,
                tint = if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}

@Composable
private fun DairyStat(
    label: String,
    value: String,
    subtitle: String,
    dotColor: Color,
    modifier: Modifier = Modifier
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Box(modifier = Modifier.size(8.dp).background(dotColor, CircleShape))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                label.uppercase(),
                style = EyebrowCaps,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(value, style = MaterialTheme.typography.titleSmall)
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ────────── Log entry card ──────────

@Composable
private fun LogEntryCard(
    entryMode: EntryMode,
    onModeChange: (EntryMode) -> Unit,
    milkQty: Double,
    yogurtQty: Double,
    onMilkQtyChange: (Double) -> Unit,
    onYogurtQtyChange: (Double) -> Unit,
    milkAmount: String,
    yogurtAmount: String,
    onMilkAmountChange: (String) -> Unit,
    onYogurtAmountChange: (String) -> Unit,
    milkPrice: Double,
    yogurtPrice: Double,
    projectedBill: Double,
    canSubmit: Boolean,
    onSubmit: () -> Unit
) {
    AppCard(contentPadding = PaddingValues(20.dp)) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "LOG ENTRY",
                    style = EyebrowCaps,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ModeToggle(selected = entryMode, onChange = onModeChange)
            }
            Spacer(Modifier.height(16.dp))
            when (entryMode) {
                EntryMode.Quantity -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    LiquidFill(
                        label = "Milk",
                        unit = "L",
                        value = milkQty,
                        max = 5.0,
                        step = 0.25,
                        liquidColor = MilkColor,
                        onChange = onMilkQtyChange,
                        modifier = Modifier.weight(1f)
                    )
                    LiquidFill(
                        label = "Yogurt",
                        unit = "kg",
                        value = yogurtQty,
                        max = 2.0,
                        step = 0.1,
                        liquidColor = YogurtColor,
                        onChange = onYogurtQtyChange,
                        modifier = Modifier.weight(1f)
                    )
                }
                EntryMode.Amount -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AmountField(
                        label = "Milk",
                        priceHint = "%.0f per litre".format(milkPrice),
                        value = milkAmount,
                        onChange = onMilkAmountChange,
                        dotColor = MilkColor
                    )
                    AmountField(
                        label = "Yogurt",
                        priceHint = "%.0f per kg".format(yogurtPrice),
                        value = yogurtAmount,
                        onChange = onYogurtAmountChange,
                        dotColor = YogurtColor
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "ENTRY TOTAL",
                        style = EyebrowCaps,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    MoneyText(
                        amount = projectedBill,
                        style = MaterialTheme.typography.titleLarge,
                        tone = MoneyTone.Expense
                    )
                }
                Button(
                    onClick = onSubmit,
                    enabled = canSubmit,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        Icons.Outlined.Check,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Save entry", style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }
}

@Composable
private fun ModeToggle(selected: EntryMode, onChange: (EntryMode) -> Unit) {
    Surface(
        shape = PillShape,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(modifier = Modifier.padding(3.dp)) {
            EntryMode.values().forEach { mode ->
                val isSel = mode == selected
                Surface(
                    onClick = { onChange(mode) },
                    shape = PillShape,
                    color = if (isSel) MaterialTheme.colorScheme.primary
                    else Color.Transparent,
                    contentColor = if (isSel) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Text(
                        when (mode) {
                            EntryMode.Quantity -> "Quantity"
                            EntryMode.Amount -> "Amount"
                        },
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AmountField(
    label: String,
    priceHint: String,
    value: String,
    onChange: (String) -> Unit,
    dotColor: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).background(dotColor, CircleShape))
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            Text(
                priceHint,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(12.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            placeholder = { Text("0") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.width(120.dp)
        )
    }
}

// ────────── History ──────────

@Composable
private fun DairyHistoryCard(
    logs: List<DairyLog>,
    onEdit: (DairyLog) -> Unit
) {
    AppCard(contentPadding = PaddingValues(0.dp)) {
        Column {
            logs.forEachIndexed { index, log ->
                DairyLogRow(log = log, onClick = { onEdit(log) })
                if (index < logs.lastIndex) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DairyLogRow(log: DairyLog, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = formatLogDate(log.date),
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = buildString {
                    if (log.milkQty > 0) append("Milk %.2fL".format(log.milkQty))
                    if (log.milkQty > 0 && log.yogurtQty > 0) append("  ·  ")
                    if (log.yogurtQty > 0) append("Yogurt %.2fkg".format(log.yogurtQty))
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        MoneyText(
            amount = log.totalBill,
            style = MaterialTheme.typography.titleSmall,
            tone = MoneyTone.Expense
        )
    }
}

// ────────── Dialogs ──────────

@Composable
private fun PriceEditorDialog(
    initialMilk: Double,
    initialYogurt: Double,
    onSave: (Double, Double) -> Unit,
    onDismiss: () -> Unit
) {
    var milk by remember { mutableStateOf(initialMilk.toInt().toString()) }
    var yogurt by remember { mutableStateOf(initialYogurt.toInt().toString()) }
    val milkValid = milk.toDoubleOrNull() != null && (milk.toDoubleOrNull() ?: 0.0) > 0
    val yogurtValid = yogurt.toDoubleOrNull() != null && (yogurt.toDoubleOrNull() ?: 0.0) > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adjust dairy prices") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "New prices apply only to future entries. Past entries keep the price they were logged with.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = milk,
                    onValueChange = { milk = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Milk · per litre") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = yogurt,
                    onValueChange = { yogurt = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Yogurt · per kg") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        milk.toDoubleOrNull() ?: initialMilk,
                        yogurt.toDoubleOrNull() ?: initialYogurt
                    )
                },
                enabled = milkValid && yogurtValid
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun EditEntryDialog(
    log: DairyLog,
    milkPrice: Double,
    yogurtPrice: Double,
    onSave: (milk: Double, yogurt: Double, date: String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var milk by remember { mutableStateOf(log.milkQty.toString()) }
    var yogurt by remember { mutableStateOf(log.yogurtQty.toString()) }
    val milkValue = milk.toDoubleOrNull() ?: 0.0
    val yogurtValue = yogurt.toDoubleOrNull() ?: 0.0
    val total = (milkValue * milkPrice) + (yogurtValue * yogurtPrice)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(formatLogDate(log.date)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = milk,
                    onValueChange = { milk = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Milk (L)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = yogurt,
                    onValueChange = { yogurt = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Yogurt (kg)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Total", style = MaterialTheme.typography.labelMedium)
                    MoneyText(
                        amount = total,
                        style = MaterialTheme.typography.titleMedium,
                        tone = MoneyTone.Expense
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(milkValue, yogurtValue, log.date) }) { Text("Save") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}
