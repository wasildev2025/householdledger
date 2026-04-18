package com.example.householdledger.ui.dairy

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.householdledger.data.model.DairyLog
import com.example.householdledger.ui.components.AppCard
import com.example.householdledger.ui.components.EmptyState
import com.example.householdledger.ui.components.MoneyText
import com.example.householdledger.ui.components.SectionHeader
import com.example.householdledger.ui.theme.MoneyDisplay
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DairyTrackerScreen(
    onBack: () -> Unit,
    viewModel: DairyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val logs = uiState.logs

    var milkQty by remember { mutableStateOf("") }
    var yogurtQty by remember { mutableStateOf("") }

    val currentMonth = remember { YearMonth.now() }
    val monthLogs = remember(logs, currentMonth) {
        logs.filter { log ->
            parseDate(log.date)?.let { YearMonth.of(it.year, it.monthValue) == currentMonth } ?: false
        }
    }
    val monthBill = monthLogs.sumOf { it.totalBill }
    val monthMilk = monthLogs.sumOf { it.milkQty }
    val monthYogurt = monthLogs.sumOf { it.yogurtQty }

    val canSubmit = (milkQty.toDoubleOrNull() ?: 0.0) > 0 ||
        (yogurtQty.toDoubleOrNull() ?: 0.0) > 0

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Dairy Tracker", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                BillHero(
                    totalBill = monthBill,
                    milkLitres = monthMilk,
                    yogurtKg = monthYogurt,
                    monthLabel = currentMonth.format(DateTimeFormatter.ofPattern("MMMM"))
                )
            }

            item {
                SectionHeader(title = "Log Today's Entry")
            }

            item {
                AppCard(contentPadding = PaddingValues(16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        QuantityField(
                            label = "Milk",
                            unit = "L",
                            value = milkQty,
                            onChange = { milkQty = it },
                            icon = Icons.Outlined.LocalDrink,
                            unitPrice = uiState.milkPrice
                        )
                        QuantityField(
                            label = "Yogurt",
                            unit = "Kg",
                            value = yogurtQty,
                            onChange = { yogurtQty = it },
                            icon = Icons.Outlined.LocalCafe,
                            unitPrice = uiState.yogurtPrice
                        )
                        Button(
                            onClick = {
                                viewModel.addLog(
                                    milkQty.toDoubleOrNull() ?: 0.0,
                                    yogurtQty.toDoubleOrNull() ?: 0.0
                                )
                                milkQty = ""
                                yogurtQty = ""
                            },
                            enabled = canSubmit,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Save Entry", style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }
            }

            item {
                SectionHeader(title = "History")
            }

            if (logs.isEmpty()) {
                item {
                    AppCard(tonal = true) {
                        EmptyState(
                            icon = Icons.Outlined.WaterDrop,
                            title = "No entries yet",
                            description = "Log your first milk or yogurt delivery above."
                        )
                    }
                }
            } else {
                items(logs, key = { it.id }) { log ->
                    DairyLogRow(
                        log = log,
                        onDelete = { viewModel.deleteLog(log) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BillHero(
    totalBill: Double,
    milkLitres: Double,
    yogurtKg: Double,
    monthLabel: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.primary
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.82f)
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "$monthLabel Dairy Bill",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                )
                MoneyText(
                    amount = totalBill,
                    style = MoneyDisplay,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    BreakdownItem(
                        icon = Icons.Outlined.LocalDrink,
                        label = "Milk",
                        value = "${formatQty(milkLitres)} L"
                    )
                    BreakdownItem(
                        icon = Icons.Outlined.LocalCafe,
                        label = "Yogurt",
                        value = "${formatQty(yogurtKg)} Kg"
                    )
                }
            }
        }
    }
}

@Composable
private fun BreakdownItem(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(
                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(14.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
private fun QuantityField(
    label: String,
    unit: String,
    value: String,
    onChange: (String) -> Unit,
    icon: ImageVector,
    unitPrice: Double?
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.weight(1f))
            if (unitPrice != null && unitPrice > 0) {
                Text(
                    text = "₹${formatQty(unitPrice)} per $unit",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            placeholder = { Text("0") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            suffix = {
                Text(
                    unit,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun DairyLogRow(log: DairyLog, onDelete: () -> Unit) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.WaterDrop,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatDate(log.date),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = buildString {
                        if (log.milkQty > 0) append("${formatQty(log.milkQty)} L milk")
                        if (log.yogurtQty > 0) {
                            if (log.milkQty > 0) append(" · ")
                            append("${formatQty(log.yogurtQty)} Kg yogurt")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            MoneyText(
                amount = log.totalBill,
                style = MaterialTheme.typography.titleMedium
            )
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Delete entry",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatQty(v: Double): String = if (v % 1.0 == 0.0) "%.0f".format(v) else "%.2f".format(v)

private fun parseDate(raw: String): LocalDate? = try {
    LocalDateTime.parse(raw).toLocalDate()
} catch (_: DateTimeParseException) {
    try { LocalDate.parse(raw) } catch (_: DateTimeParseException) { null }
}

private fun formatDate(raw: String): String {
    val date = parseDate(raw) ?: return raw
    val today = LocalDate.now()
    return when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> date.format(DateTimeFormatter.ofPattern("d MMM"))
    }
}
