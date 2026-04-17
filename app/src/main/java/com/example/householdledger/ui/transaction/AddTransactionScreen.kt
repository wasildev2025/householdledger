package com.example.householdledger.ui.transaction

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.householdledger.data.model.Category
import com.example.householdledger.ui.components.CategoryPill
import com.example.householdledger.ui.components.MoneyText
import com.example.householdledger.ui.components.MoneyTone
import com.example.householdledger.ui.theme.MoneyDisplay
import com.example.householdledger.ui.theme.appColors
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private enum class TxnType(val key: String, val label: String, val icon: ImageVector) {
    Expense("expense", "Expense", Icons.AutoMirrored.Outlined.TrendingDown),
    Income("income", "Income", Icons.AutoMirrored.Outlined.TrendingUp),
    Transfer("transfer", "Transfer", Icons.Outlined.SwapHoriz)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    onBack: () -> Unit,
    viewModel: AddTransactionViewModel = hiltViewModel()
) {
    var amount by remember { mutableStateOf("0") }
    var description by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(TxnType.Expense) }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }

    val categories by viewModel.categories.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()

    val amountDouble = amount.toDoubleOrNull() ?: 0.0
    val canSubmit = amountDouble > 0 && !isSaving

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Add Transaction",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Spacer(Modifier.height(8.dp))

            TypeSelector(
                selected = type,
                onSelect = { type = it },
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(Modifier.height(28.dp))

            AmountDisplay(
                amount = amountDouble,
                tone = when (type) {
                    TxnType.Income -> MoneyTone.Income
                    TxnType.Expense -> MoneyTone.Expense
                    TxnType.Transfer -> MoneyTone.Neutral
                }
            )

            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                placeholder = { Text("What's this for?") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            )

            if (type == TxnType.Expense && categories.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(Modifier.height(10.dp))
                CategoryGrid(
                    categories = categories,
                    selectedId = selectedCategoryId,
                    onSelect = { selectedCategoryId = it }
                )
            }

            Spacer(Modifier.weight(1f))

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

            Button(
                onClick = {
                    val now = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
                    viewModel.addTransaction(
                        amount = amountDouble,
                        description = description,
                        type = type.key,
                        categoryId = selectedCategoryId,
                        date = now
                    )
                    onBack()
                },
                enabled = canSubmit,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .height(56.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Save", style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }
}

@Composable
private fun TypeSelector(
    selected: TxnType,
    onSelect: (TxnType) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(modifier = Modifier.padding(4.dp)) {
            TxnType.values().forEach { option ->
                val isSelected = option == selected
                val bg = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                val fg = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = bg,
                    onClick = { onSelect(option) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = option.icon,
                            contentDescription = null,
                            tint = fg,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = fg
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AmountDisplay(amount: Double, tone: MoneyTone) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Amount",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        MoneyText(
            amount = amount,
            style = MoneyDisplay,
            tone = tone
        )
    }
}

@Composable
private fun CategoryGrid(
    categories: List<Category>,
    selectedId: String?,
    onSelect: (String) -> Unit
) {
    LazyHorizontalGrid(
        rows = GridCells.Fixed(1),
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(categories, key = { it.id }) { category ->
            val isSelected = selectedId == category.id
            Column(
                modifier = Modifier.width(76.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        onClick = { onSelect(category.id) },
                        shape = CircleShape,
                        color = Color.Transparent
                    ) {
                        CategoryPill(
                            icon = category.icon.ifBlank { category.name.take(1).uppercase() },
                            colorHex = category.color,
                            size = 48.dp
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
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
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(14.dp),
        color = Color.Transparent
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
            if (key == "⌫") {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Backspace",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Text(
                    text = key,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
