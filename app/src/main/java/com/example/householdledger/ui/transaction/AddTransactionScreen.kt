package com.example.householdledger.ui.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.householdledger.data.model.Category
import com.example.householdledger.ui.components.MoneyText
import com.example.householdledger.ui.components.MoneyTone
import com.example.householdledger.ui.theme.MoneyDisplay
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private enum class TxnType(val key: String, val label: String, val icon: ImageVector) {
    Income("income", "Income", Icons.AutoMirrored.Outlined.TrendingUp),
    Expense("expense", "Expense", Icons.AutoMirrored.Outlined.TrendingDown),
    Transfer("transfer", "Transfer", Icons.Outlined.SwapHoriz)
}

/**
 * Body of the Add Transaction modal bottom sheet. The sheet chrome (scrim, drag
 * handle, rounded top) is provided by the ModalBottomSheet in MainActivity — this
 * composable only renders the content.
 */
@Composable
fun AddTransactionSheet(
    onClose: () -> Unit,
    viewModel: AddTransactionViewModel = hiltViewModel()
) {
    var amount by remember { mutableStateOf("0") }
    var type by remember { mutableStateOf(TxnType.Expense) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var categoryMenuOpen by remember { mutableStateOf(false) }

    val categories by viewModel.categories.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()

    val amountDouble = amount.toDoubleOrNull() ?: 0.0
    val canSubmit = amountDouble > 0 && !isSaving
    val todayLabel = remember { LocalDate.now().format(DateTimeFormatter.ofPattern("d MMM yyyy")) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 16.dp)
    ) {
        Spacer(Modifier.height(4.dp))
        SegmentedPillTabs(
            selected = type,
            onSelect = { type = it }
        )

        Spacer(Modifier.height(16.dp))

        BudgetAlertBanner()

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Amount",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        MoneyText(
            amount = amountDouble,
            style = MoneyDisplay,
            tone = when (type) {
                TxnType.Income -> MoneyTone.Income
                TxnType.Expense -> MoneyTone.Expense
                TxnType.Transfer -> MoneyTone.Neutral
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        )

        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                ChipButton(
                    icon = Icons.Outlined.Category,
                    label = selectedCategory?.name ?: "Category",
                    onClick = { categoryMenuOpen = true }
                )
                DropdownMenu(
                    expanded = categoryMenuOpen,
                    onDismissRequest = { categoryMenuOpen = false }
                ) {
                    categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.name) },
                            onClick = {
                                selectedCategory = category
                                categoryMenuOpen = false
                            }
                        )
                    }
                }
            }
            ChipButton(
                icon = Icons.Outlined.CalendarToday,
                label = todayLabel,
                onClick = {},
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(20.dp))

        NumberPad(
            onDigit = { d ->
                amount = when {
                    amount == "0" && d != "." -> d
                    d == "." && amount.contains(".") -> amount
                    else -> amount + d
                }
            },
            onBackspace = {
                amount = if (amount.length <= 1) "0" else amount.dropLast(1)
            }
        )

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                val now = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
                viewModel.addTransaction(
                    amount = amountDouble,
                    description = selectedCategory?.name.orEmpty(),
                    type = type.key,
                    categoryId = selectedCategory?.id,
                    date = now
                )
                onClose()
            },
            enabled = canSubmit,
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Add Transaction", style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

@Composable
private fun SegmentedPillTabs(
    selected: TxnType,
    onSelect: (TxnType) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        TxnType.values().forEachIndexed { i, option ->
            val isSelected = option == selected
            val bg = if (isSelected) MaterialTheme.colorScheme.secondary else Color.Transparent
            val fg = if (isSelected) MaterialTheme.colorScheme.onSecondary
            else MaterialTheme.colorScheme.onSurfaceVariant
            Surface(
                onClick = { onSelect(option) },
                shape = RoundedCornerShape(999.dp),
                color = bg,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Text(
                    text = option.label,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = fg,
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp)
                )
            }
            if (i < TxnType.values().lastIndex) Spacer(Modifier.width(0.dp))
        }
    }
}

@Composable
private fun BudgetAlertBanner() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(MaterialTheme.colorScheme.tertiary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Budget tip",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Log every transaction to keep your monthly totals accurate.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ChipButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun NumberPad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf(".", "0", "⌫")
    )
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { key ->
                    NumberKey(
                        key = key,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (key == "⌫") onBackspace() else onDigit(key)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NumberKey(
    key: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
            if (key == "⌫") {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Backspace",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Text(
                    text = key,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
