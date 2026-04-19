package com.example.householdledger.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.RemoveRedEye
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
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
import com.example.householdledger.ui.components.AppCard
import com.example.householdledger.ui.components.Avatar
import com.example.householdledger.ui.components.CategoryPill
import com.example.householdledger.ui.components.CyclePulseHero
import com.example.householdledger.ui.components.CycleRibbon
import com.example.householdledger.ui.components.EmptyState
import com.example.householdledger.ui.components.MoneyText
import com.example.householdledger.ui.components.MoneyTone
import com.example.householdledger.ui.components.SectionHeader
import com.example.householdledger.ui.components.Skeleton
import com.example.householdledger.ui.components.WalletPocketCard
import com.example.householdledger.util.DateUtil
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun HomeScreen(
    onAddTransaction: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToInsights: () -> Unit,
    onNavigateToDairy: () -> Unit,
    onNavigateToRecurring: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToPeople: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var cardsIn by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { cardsIn = true }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                CycleRibbon(
                    dayIndex = state.cycleDayIndex,
                    cycleLengthDays = state.cycleLengthDays,
                    endLabel = state.cycleEndInclusive.format(DateTimeFormatter.ofPattern("d MMM"))
                )
            }
            item {
                TopBar(
                    userName = state.userName,
                    today = state.today,
                    unread = state.unreadMessages,
                    isOffline = state.isOffline,
                    onChat = onNavigateToChat
                )
            }
            if (state.isOffline) item { OfflineBanner() }

            item {
                AnimatedVisibility(
                    visible = cardsIn,
                    enter = fadeIn(tween(380)) + slideInVertically(tween(380)) { it / 4 }
                ) {
                    val daysLeft = (state.cycleLengthDays - state.cycleDayIndex - 1).coerceAtLeast(0)
                    CyclePulseHero(
                        cycleLabel = state.monthLabel,
                        daysLeft = daysLeft,
                        dayIndex = state.cycleDayIndex,
                        cycleLengthDays = state.cycleLengthDays,
                        expenseSoFar = state.expense,
                        budgetCap = state.budgetCap,
                        projectedExpense = state.projectedExpense,
                        projectedOverrunPercent = state.projectedOverrunPercent,
                        income = state.income,
                        transfers = state.transfers
                    )
                }
            }

            if (state.role == "admin") {
                item {
                    HomeFilterTabs(
                        selected = state.filter,
                        onChange = viewModel::setFilter
                    )
                }
            }

            item {
                EssentialsRow(
                    onDairy = onNavigateToDairy,
                    onRecurring = onNavigateToRecurring,
                    onChat = onNavigateToChat,
                    onCategories = onNavigateToCategories,
                    onPeople = onNavigateToPeople,
                    isAdmin = state.role == "admin"
                )
            }

            if (state.aiInsight != null) {
                item { AiInsightCard(text = state.aiInsight!!, onTap = onNavigateToInsights) }
            }

            if (state.wallets.isNotEmpty()) {
                item { SectionHeader(title = "Wallets") }
                item { WalletsRow(state.wallets) }
            }

            if (state.upcomingBills.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Upcoming bills",
                        actionLabel = "See all",
                        onActionClick = onNavigateToRecurring
                    )
                }
                item { UpcomingBillsRow(state.upcomingBills) }
            }

            item {
                SectionHeader(
                    title = "Recent Activity",
                    actionLabel = if (state.recent.size >= 5) "View all" else null,
                    onActionClick = onNavigateToTransactions
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
                    TransactionRowItem(row)
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    userName: String,
    today: String,
    unread: Int,
    isOffline: Boolean,
    onChat: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(name = userName, size = 44.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Hi, ${userName.replaceFirstChar { it.uppercase() }}!",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (isOffline) {
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.Filled.CloudOff,
                        null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(text = today, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (unread > 0) {
            BadgedBox(badge = { Badge { Text(unread.toString()) } }) {
                IconButtonRound(Icons.Outlined.ChatBubbleOutline, onChat)
            }
        } else {
            IconButtonRound(Icons.Outlined.ChatBubbleOutline, onChat)
        }
        Spacer(Modifier.width(8.dp))
        IconButtonRound(Icons.Outlined.NotificationsNone, {})
    }
}

@Composable
private fun IconButtonRound(icon: ImageVector, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(42.dp).background(MaterialTheme.colorScheme.surface, CircleShape)
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun OfflineBanner() {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.CloudOff, null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                "Offline — changes will sync when you're back online",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun HomeFilterTabs(selected: HomeFilter, onChange: (HomeFilter) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HomeFilter.values().forEach { opt ->
            Surface(
                onClick = { onChange(opt) },
                shape = RoundedCornerShape(999.dp),
                color = if (opt == selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surface,
                contentColor = if (opt == selected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    opt.name,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = androidx.compose.ui.graphics.Color.Unspecified
                )
            }
        }
    }
}

@Composable
private fun EssentialsRow(
    onDairy: () -> Unit,
    onRecurring: () -> Unit,
    onChat: () -> Unit,
    onCategories: () -> Unit,
    onPeople: () -> Unit,
    isAdmin: Boolean
) {
    AppCard(contentPadding = PaddingValues(14.dp)) {
        Column {
            Text("Household Essentials", style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()) {
                EssentialTile("Dairy", Icons.Outlined.LocalDrink, Modifier.weight(1f), onDairy)
                EssentialTile("Chat", Icons.Outlined.ChatBubbleOutline, Modifier.weight(1f), onChat)
                if (isAdmin) {
                    EssentialTile("People", Icons.Outlined.Groups, Modifier.weight(1f), onPeople)
                    EssentialTile("Categories", Icons.Outlined.AutoAwesome, Modifier.weight(1f), onCategories)
                }
            }
            if (isAdmin) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()) {
                    EssentialTile("Recurring", Icons.Outlined.Repeat, Modifier.weight(1f), onRecurring)
                    Spacer(Modifier.weight(3f))
                }
            }
        }
    }
}

@Composable
private fun EssentialTile(label: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.height(78.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize().padding(8.dp)
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
        }
    }
}

@Composable
private fun AiInsightCard(text: String, onTap: () -> Unit) {
    AppCard(onClick = onTap, contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier.size(40.dp)
                    .background(MaterialTheme.colorScheme.tertiaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.AutoAwesome, null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("AI Insight", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary)
                Spacer(Modifier.height(2.dp))
                Text(text, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface, maxLines = 3)
            }
        }
    }
}

@Composable
private fun WalletsRow(wallets: List<WalletSummary>) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(horizontal = 2.dp)
    ) {
        items(wallets, key = { it.kind + it.id }) { w ->
            WalletPocketCard(
                name = w.name,
                kindLabel = w.kind,
                monthSpend = w.monthlySpend,
                transferredIn = w.transferredIn,
                netBalance = w.netBalance,
                allocation = w.allocation,
                identityKey = w.kind + w.id
            )
        }
    }
}

@Composable
private fun UpcomingBillsRow(bills: List<UpcomingBill>) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(bills, key = { it.id }) { b -> UpcomingBillCard(b) }
    }
}

@Composable
private fun UpcomingBillCard(b: UpcomingBill) {
    val urgent = b.daysUntil <= 1
    AppCard(
        modifier = Modifier.width(180.dp),
        contentPadding = PaddingValues(14.dp),
        containerColor = if (urgent) MaterialTheme.colorScheme.errorContainer
        else MaterialTheme.colorScheme.surface,
        borderColor = if (urgent) null else MaterialTheme.colorScheme.outlineVariant
    ) {
        Column {
            Text(
                b.dueDate,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (urgent) MaterialTheme.colorScheme.onErrorContainer
                else MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                b.name,
                style = MaterialTheme.typography.titleSmall,
                color = if (urgent) MaterialTheme.colorScheme.onErrorContainer
                else MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Spacer(Modifier.height(10.dp))
            MoneyText(
                amount = b.amount,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = if (urgent) MaterialTheme.colorScheme.onErrorContainer
                else MaterialTheme.colorScheme.onSurface
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
    AppCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
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
            MoneyText(amount = txn.amount, tone = tone, showSign = true)
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
    val date = DateUtil.parseDate(raw) ?: return raw
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
