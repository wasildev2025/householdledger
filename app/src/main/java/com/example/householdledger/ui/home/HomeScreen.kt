package com.example.householdledger.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.RemoveRedEye
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.householdledger.ui.components.AppCard
import com.example.householdledger.ui.components.Avatar
import com.example.householdledger.ui.components.CategoryPill
import com.example.householdledger.ui.components.EmptyState
import com.example.householdledger.ui.components.HeroCard
import com.example.householdledger.ui.components.MoneyText
import com.example.householdledger.ui.components.MoneyTone
import com.example.householdledger.ui.components.SectionHeader
import com.example.householdledger.ui.components.Skeleton
import com.example.householdledger.ui.components.TrendChip
import com.example.householdledger.ui.components.TrendDirection
import com.example.householdledger.ui.theme.MoneyBody
import com.example.householdledger.ui.theme.MoneyDisplay
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
    var balanceVisible by remember { mutableStateOf(true) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { TopBar(userName = state.userName, today = state.today) }
            item {
                BalanceHero(
                    state = state,
                    balanceVisible = balanceVisible,
                    onToggleVisibility = { balanceVisible = !balanceVisible }
                )
            }
            item { BudgetCard(state = state) }
            item {
                SectionHeader(
                    title = "Recent Activity",
                    actionLabel = if (state.recent.size > 5) "View all" else null,
                    onActionClick = {}
                )
            }

            when {
                state.isLoading -> items(4) { SkeletonTransactionRow() }
                state.recent.isEmpty() -> item {
                    AppCard(tonal = true) {
                        EmptyState(
                            icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                            title = "No transactions yet",
                            description = "Tap the + button to record your first expense or income.",
                            actionLabel = "Add transaction",
                            onActionClick = onAddTransaction
                        )
                    }
                }
                else -> items(state.recent, key = { it.transaction.id }) { row ->
                    TransactionRowItem(row = row)
                }
            }
        }
    }
}

@Composable
private fun TopBar(userName: String, today: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(name = userName, size = 44.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Hi, ${userName.replaceFirstChar { it.uppercase() }}!",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = today,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButtonRound(icon = Icons.Outlined.NotificationsNone, onClick = {})
        Spacer(Modifier.width(8.dp))
        IconButtonRound(icon = Icons.Outlined.Settings, onClick = {})
    }
}

@Composable
private fun IconButtonRound(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(42.dp)
            .background(MaterialTheme.colorScheme.surface, CircleShape)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun BalanceHero(
    state: HomeUiState,
    balanceVisible: Boolean,
    onToggleVisibility: () -> Unit
) {
    HeroCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(22.dp)
    ) {
        Column {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = Color.White.copy(alpha = 0.15f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Main Account",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            Text(
                text = "Total Balance",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.8f)
            )
            Spacer(Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (balanceVisible) {
                    MoneyText(
                        amount = state.balance,
                        style = MoneyDisplay,
                        color = Color.White
                    )
                } else {
                    Text(
                        text = "••••••",
                        style = MoneyDisplay,
                        color = Color.White
                    )
                }
                Spacer(Modifier.width(10.dp))
                IconButton(
                    onClick = onToggleVisibility,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (balanceVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.RemoveRedEye,
                        contentDescription = if (balanceVisible) "Hide balance" else "Show balance",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                HeroStat(
                    label = "Monthly Income",
                    amount = state.income,
                    deltaPercent = state.incomeDeltaPercent,
                    positiveIsGood = true
                )
                HeroStat(
                    label = "Monthly Expense",
                    amount = state.expense,
                    deltaPercent = state.expenseDeltaPercent,
                    positiveIsGood = false
                )
            }
        }
    }
}

@Composable
private fun HeroStat(
    label: String,
    amount: Double,
    deltaPercent: Float,
    positiveIsGood: Boolean
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.7f)
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            MoneyText(
                amount = amount,
                style = MoneyBody.copy(fontSize = androidx.compose.ui.unit.TextUnit.Unspecified),
                color = Color.White
            )
            Spacer(Modifier.width(8.dp))
            if (deltaPercent != 0f) {
                TrendChip(
                    percent = deltaPercent,
                    positiveIsGood = positiveIsGood,
                    surface = Color.White.copy(alpha = 0.18f),
                    content = Color.White
                )
            }
        }
    }
}

@Composable
private fun BudgetCard(state: HomeUiState) {
    AppCard(contentPadding = PaddingValues(18.dp)) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Monthly Budget",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${(state.budgetUsedPercent * 100).toInt()}%",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(999.dp)
                    )
            ) {
                val clipped = state.budgetUsedPercent.coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(clipped)
                        .fillMaxHeight()
                        .background(
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(999.dp)
                        )
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Spent ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                MoneyText(
                    amount = state.expense,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "  of  ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                MoneyText(
                    amount = if (state.budgetCap > 0) state.budgetCap else 0.0,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryPill(
                icon = row.category?.icon?.ifBlank { txn.description.take(1).uppercase() }
                    ?: txn.description.take(1).uppercase(),
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
    AppCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
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
