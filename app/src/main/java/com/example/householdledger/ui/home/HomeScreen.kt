package com.example.householdledger.ui.home

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.householdledger.ui.components.AppCard
import com.example.householdledger.ui.components.CategoryPill
import com.example.householdledger.ui.components.EmptyState
import com.example.householdledger.ui.components.MoneyText
import com.example.householdledger.ui.components.MoneyTone
import com.example.householdledger.ui.components.SectionHeader
import com.example.householdledger.ui.components.Skeleton
import com.example.householdledger.ui.theme.MoneyBody
import com.example.householdledger.ui.theme.MoneyDisplay
import com.example.householdledger.ui.theme.appColors
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

@Composable
fun HomeScreen(
    onAddTransaction: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddTransaction,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Greeting(userName = state.userName) }
            item { BalanceHero(state = state) }
            item { StatsRow(income = state.income, expense = state.expense) }
            item {
                SectionHeader(
                    title = "Recent Activity",
                    actionLabel = if (state.recent.size > 5) "See all" else null,
                    onActionClick = {}
                )
            }

            when {
                state.isLoading -> {
                    items(4) { SkeletonTransactionRow() }
                }
                state.recent.isEmpty() -> {
                    item {
                        AppCard(tonal = true) {
                            EmptyState(
                                icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                                title = "No transactions yet",
                                description = "Tap the Add button to record your first expense or income.",
                                actionLabel = "Add transaction",
                                onActionClick = onAddTransaction
                            )
                        }
                    }
                }
                else -> {
                    items(state.recent, key = { it.transaction.id }) { row ->
                        TransactionRowItem(row = row)
                    }
                }
            }
        }
    }
}

@Composable
private fun Greeting(userName: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = timeOfDayGreeting(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = userName.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        IconButton(
            onClick = {},
            modifier = Modifier
                .size(44.dp)
                .background(MaterialTheme.colorScheme.surface, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Outlined.NotificationsNone,
                contentDescription = "Notifications",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun BalanceHero(state: HomeUiState) {
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
                    text = "${state.monthLabel} Balance",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                )
                MoneyText(
                    amount = state.balance,
                    style = MoneyDisplay,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    InlineStat(
                        label = "Income",
                        amount = state.income,
                        icon = Icons.Default.ArrowDownward,
                        accent = MaterialTheme.colorScheme.onPrimary
                    )
                    InlineStat(
                        label = "Expense",
                        amount = state.expense,
                        icon = Icons.Default.ArrowUpward,
                        accent = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun InlineStat(
    label: String,
    amount: Double,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: androidx.compose.ui.graphics.Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(accent.copy(alpha = 0.18f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(14.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = accent.copy(alpha = 0.8f)
            )
            MoneyText(
                amount = amount,
                style = MoneyBody,
                color = accent
            )
        }
    }
}

@Composable
private fun StatsRow(income: Double, expense: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            label = "Income",
            amount = income,
            tone = MoneyTone.Income,
            modifier = Modifier.weight(1f),
            containerColor = appColors.incomeContainer
        )
        StatCard(
            label = "Expense",
            amount = expense,
            tone = MoneyTone.Expense,
            modifier = Modifier.weight(1f),
            containerColor = appColors.expenseContainer
        )
    }
}

@Composable
private fun StatCard(
    label: String,
    amount: Double,
    tone: MoneyTone,
    modifier: Modifier = Modifier,
    containerColor: androidx.compose.ui.graphics.Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = containerColor
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = when (tone) {
                    MoneyTone.Income -> appColors.onIncomeContainer
                    MoneyTone.Expense -> appColors.onExpenseContainer
                    MoneyTone.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            MoneyText(
                amount = amount,
                tone = tone,
                color = when (tone) {
                    MoneyTone.Income -> appColors.onIncomeContainer
                    MoneyTone.Expense -> appColors.onExpenseContainer
                    MoneyTone.Neutral -> MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

@Composable
private fun TransactionRowItem(row: TransactionRow) {
    val txn = row.transaction
    val tone = when (txn.type) {
        "income" -> MoneyTone.Income
        "expense" -> MoneyTone.Expense
        else -> MoneyTone.Neutral
    }
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        tonal = false,
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryPill(
                icon = row.category?.icon?.ifBlank { txn.description.take(1) } ?: txn.description.take(1).uppercase(),
                colorHex = row.category?.color
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = txn.description.ifBlank { row.category?.name ?: "Transaction" },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = buildString {
                        if (row.category != null) append(row.category.name).append(" • ")
                        append(formatRelativeDate(txn.date))
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Spacer(Modifier.width(12.dp))
            MoneyText(
                amount = txn.amount,
                tone = tone,
                showSign = true
            )
        }
    }
}

@Composable
private fun SkeletonTransactionRow() {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        tonal = false,
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Skeleton(modifier = Modifier.size(44.dp), height = 44.dp, cornerRadius = 22.dp)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Skeleton(height = 14.dp, modifier = Modifier.fillMaxWidth(0.7f))
                Spacer(Modifier.height(6.dp))
                Skeleton(height = 10.dp, modifier = Modifier.fillMaxWidth(0.4f))
            }
            Spacer(Modifier.width(12.dp))
            Skeleton(height = 16.dp, modifier = Modifier.width(70.dp))
        }
    }
}

private fun timeOfDayGreeting(): String {
    val hour = java.time.LocalTime.now().hour
    return when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..20 -> "Good evening"
        else -> "Good night"
    }
}

private fun formatRelativeDate(raw: String): String {
    val date = try {
        LocalDateTime.parse(raw).toLocalDate()
    } catch (_: DateTimeParseException) {
        try { LocalDate.parse(raw) } catch (_: DateTimeParseException) { return raw }
    }
    val today = LocalDate.now()
    val days = ChronoUnit.DAYS.between(date, today)
    return when {
        days == 0L -> "Today"
        days == 1L -> "Yesterday"
        days in 2..6 -> "$days days ago"
        date.year == today.year -> date.format(DateTimeFormatter.ofPattern("d MMM"))
        else -> date.format(DateTimeFormatter.ofPattern("d MMM yyyy"))
    }
}
