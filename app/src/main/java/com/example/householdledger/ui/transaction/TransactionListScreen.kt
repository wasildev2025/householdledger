package com.example.householdledger.ui.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.householdledger.ui.components.AppCard
import com.example.householdledger.ui.components.CategoryLogo
import com.example.householdledger.ui.components.MoneyText
import com.example.householdledger.ui.components.MoneyTone
import com.example.householdledger.ui.theme.EyebrowCaps
import com.example.householdledger.ui.theme.MoneyHero
import com.example.householdledger.ui.theme.PillShape
import com.example.householdledger.ui.theme.appColors
import com.example.householdledger.util.DateUtil
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private enum class ViewMode { List, Timeline }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    onBack: (() -> Unit)? = null,
    onAdd: () -> Unit,
    onEditTransaction: (com.example.householdledger.data.model.Transaction) -> Unit = {},
    viewModel: TransactionListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    var viewMode by remember { mutableStateOf(ViewMode.List) }

    val reachedBottom by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= info.totalItemsCount - 3
        }
    }
    LaunchedEffect(reachedBottom, state.canLoadMore) {
        if (reachedBottom && state.canLoadMore) viewModel.loadMore()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, null)
                        }
                    }
                },
                title = { Text("Transactions", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    IconButton(
                        onClick = {
                            viewMode = if (viewMode == ViewMode.List) ViewMode.Timeline else ViewMode.List
                        }
                    ) {
                        Icon(
                            imageVector = if (viewMode == ViewMode.List)
                                Icons.Outlined.Timeline
                            else
                                Icons.AutoMirrored.Outlined.FormatListBulleted,
                            contentDescription = if (viewMode == ViewMode.List) "Switch to timeline" else "Switch to list"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item { MonthSummaryCard(state) }
                if (state.categorySlices.isNotEmpty()) {
                    item { StackedCategoryBar(state.categorySlices) }
                }
                item {
                    FilterBar(
                        filter = state.filter,
                        onFilter = viewModel::setFilter
                    )
                }
                if (state.isAdmin) {
                    item {
                        PersonFilterBar(
                            filter = state.personFilter,
                            onFilter = viewModel::setPersonFilter
                        )
                    }
                }

                if (state.sections.isEmpty() && !state.isRefreshing) {
                    item {
                        CycleAwareEmptyState(
                            dayIndex = state.cycleDayIndex,
                            cycleLengthDays = state.cycleLengthDays,
                            filter = state.filter
                        )
                    }
                } else {
                    if (viewMode == ViewMode.List) {
                        state.sections.forEach { section ->
                            item(key = "header-${section.label}") {
                                SectionBand(section = section)
                            }
                            items(section.rows.size) { index ->
                                val row = section.rows[index]
                                ModernTransactionCard(row, onClick = { onEditTransaction(row.transaction) })
                            }
                        }
                    } else {
                        transactionsTimeline(
                            sections = state.sections,
                            onRowClick = onEditTransaction
                        )
                    }

                    if (state.canLoadMore) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernTransactionCard(row: TxnListRow, onClick: () -> Unit) {
    val txn = row.transaction
    val tone = when (txn.type) {
        "income" -> MoneyTone.Income
        "expense" -> MoneyTone.Expense
        else -> MoneyTone.Neutral
    }

    AppCard(
        onClick = onClick,
        contentPadding = PaddingValues(16.dp),
        elevation = 2.dp,
        borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
        cornerRadius = 24.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            val accentColor = parseHex(row.category?.color) ?: MaterialTheme.colorScheme.primary
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .background(accentColor, RoundedCornerShape(2.dp))
            )

            Spacer(Modifier.width(12.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    CategoryLogo(
                        name = row.category?.name ?: txn.description,
                        colorHex = row.category?.color,
                        iconName = row.category?.icon,
                        size = 32.dp
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = txn.description.ifBlank { row.category?.name ?: "Transaction" },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = row.category?.name ?: "Uncategorized",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatRelativeTransactionDate(txn.date),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                MoneyText(
                    amount = txn.amount,
                    tone = tone,
                    showSign = false,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                )
            }
        }
    }
}

private fun formatRelativeTransactionDate(raw: String): String {
    val date = DateUtil.parseDate(raw) ?: return raw
    val today = LocalDate.now()
    return when {
        date == today -> "Today"
        date == today.minusDays(1) -> "Yesterday"
        date.year == today.year -> date.format(DateTimeFormatter.ofPattern("MMM d"))
        else -> date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
    }
}

@Composable
private fun MonthSummaryCard(state: TxnListState) {
    val (heroLabel, heroAmount, heroTone) = when (state.filter) {
        TxnFilter.Income -> Triple("INCOME THIS CYCLE", state.monthIncome, MoneyTone.Income)
        TxnFilter.Transfer -> Triple("TRANSFERS THIS CYCLE", state.monthTransfers, MoneyTone.Neutral)
        TxnFilter.Expense, TxnFilter.All -> Triple("SPEND THIS CYCLE", state.monthSpend, MoneyTone.Expense)
    }
    val supportingStats: List<Triple<String, Double, MoneyTone>> = when (state.filter) {
        TxnFilter.Income -> listOf(
            Triple("Expense", state.monthSpend, MoneyTone.Expense),
            Triple("Transfers", state.monthTransfers, MoneyTone.Neutral)
        )
        TxnFilter.Transfer -> listOf(
            Triple("Income", state.monthIncome, MoneyTone.Income),
            Triple("Expense", state.monthSpend, MoneyTone.Expense)
        )
        TxnFilter.Expense, TxnFilter.All -> listOf(
            Triple("Income", state.monthIncome, MoneyTone.Income),
            Triple("Transfers", state.monthTransfers, MoneyTone.Neutral)
        )
    }

    val heroShape = RoundedCornerShape(32.dp)
    val heroGradient = when (heroTone) {
        MoneyTone.Income -> Brush.linearGradient(
            listOf(Color(0xFF143A33), Color(0xFF0E5F57), Color(0xFF2F8F6A))
        )
        MoneyTone.Expense -> Brush.linearGradient(
            listOf(Color(0xFF3D1E1E), Color(0xFF6E2323), Color(0xFFD14343))
        )
        MoneyTone.Neutral -> Brush.linearGradient(
            listOf(Color(0xFF262A37), Color(0xFF3C475F), Color(0xFF6B7280))
        )
    }
    val heroText = Color(0xFFFFFAF2)

    Surface(
        shape = heroShape,
        color = Color.Transparent,
        shadowElevation = 10.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .clip(heroShape)
                .background(heroGradient)
                .border(1.dp, heroText.copy(alpha = 0.12f), heroShape)
                .padding(22.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(152.dp)
                    .background(heroText.copy(alpha = 0.06f), CircleShape)
            )
            Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    heroLabel,
                    style = EyebrowCaps,
                    color = heroText.copy(alpha = 0.72f)
                )
                    Surface(
                        shape = PillShape,
                        color = heroText.copy(alpha = 0.10f)
                    ) {
                        Text(
                            state.cycleLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = heroText,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
            }
            Spacer(Modifier.height(8.dp))
            MoneyText(
                amount = heroAmount,
                style = MoneyHero.copy(
                    fontSize = androidx.compose.ui.unit.TextUnit.Unspecified
                ),
                tone = heroTone,
                color = if (heroTone == MoneyTone.Neutral) heroText
                else Color.Unspecified
            )
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                supportingStats.forEach { (label, amount, tone) ->
                    FlowStat(
                        label = label,
                        amount = amount,
                        tone = tone,
                        modifier = Modifier.weight(1f),
                        onHero = true
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Day ${state.cycleDayIndex + 1} / ${state.cycleLengthDays}",
                    style = EyebrowCaps,
                    color = heroText.copy(alpha = 0.76f)
                )
            }
            Spacer(Modifier.height(8.dp))
            CycleProgressBar(
                dayIndex = state.cycleDayIndex,
                lengthDays = state.cycleLengthDays,
                trackColor = heroText.copy(alpha = 0.16f),
                progressColor = heroText
            )
            }
        }
    }
}

@Composable
private fun FlowStat(
    label: String,
    amount: Double,
    tone: MoneyTone,
    modifier: Modifier = Modifier,
    onHero: Boolean = false
) {
    val dotColor = when (tone) {
        MoneyTone.Income -> com.example.householdledger.ui.theme.appColors.income
        MoneyTone.Expense -> com.example.householdledger.ui.theme.appColors.expense
        MoneyTone.Neutral -> if (onHero) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    }
    val textColor = if (onHero) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(dotColor, CircleShape)
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = textColor.copy(alpha = 0.7f)
            )
            MoneyText(
                amount = amount,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                tone = tone,
                color = if (tone == MoneyTone.Neutral && onHero) textColor else Color.Unspecified
            )
        }
    }
}

@Composable
private fun CycleProgressBar(
    dayIndex: Int,
    lengthDays: Int,
    trackColor: Color = MaterialTheme.colorScheme.outlineVariant,
    progressColor: Color = MaterialTheme.colorScheme.primary
) {
    val ratio = if (lengthDays > 0) ((dayIndex + 1f) / lengthDays).coerceIn(0f, 1f) else 0f
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .background(trackColor, RoundedCornerShape(999.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(ratio)
                .fillMaxHeight()
                .background(progressColor, RoundedCornerShape(999.dp))
        )
    }
}

@Composable
private fun StackedCategoryBar(slices: List<CategorySliceVm>) {
    AppCard(
        contentPadding = PaddingValues(18.dp),
        borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        elevation = 5.dp
    ) {
        Column {
            Text(
                "By category",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(999.dp))
            ) {
                slices.take(6).forEach { slice ->
                    val color = parseHex(slice.colorHex) ?: MaterialTheme.colorScheme.primary
                    Box(
                        modifier = Modifier.fillMaxHeight().weight(slice.share.coerceAtLeast(0.01f))
                            .background(color)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            val top = slices.take(4)
            top.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    pair.forEach { slice ->
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(10.dp)
                                .background(parseHex(slice.colorHex) ?: MaterialTheme.colorScheme.primary,
                                    CircleShape))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                slice.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "${(slice.share * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CycleAwareEmptyState(
    dayIndex: Int,
    cycleLengthDays: Int,
    filter: TxnFilter
) {
    val dayNumber = dayIndex + 1
    val title = when (filter) {
        TxnFilter.Income -> "No income this cycle yet"
        TxnFilter.Expense -> "No expenses this cycle"
        TxnFilter.Transfer -> "No transfers this cycle"
        TxnFilter.All -> if (dayNumber <= 2) "Fresh cycle, fresh start" else "Nothing logged yet"
    }
    val body = when (filter) {
        TxnFilter.All -> "Day $dayNumber of $cycleLengthDays. Tap + to record your first transaction."
        else -> "Day $dayNumber of $cycleLengthDays. Switch filters to see other activity."
    }
    AppCard(
        tonal = true,
        contentPadding = PaddingValues(24.dp),
        cornerRadius = 30.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(6.dp))
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FilterBar(filter: TxnFilter, onFilter: (TxnFilter) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Type Filter",
            style = EyebrowCaps,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TxnFilter.entries.forEach { opt ->
                val label = when (opt) {
                    TxnFilter.All -> "All types"
                    TxnFilter.Expense -> "Expense"
                    TxnFilter.Income -> "Income"
                    TxnFilter.Transfer -> "Transfer"
                }
                FilterChipPill(label, opt == filter) { onFilter(opt) }
            }
        }
    }
}

@Composable
private fun PersonFilterBar(filter: PersonFilter, onFilter: (PersonFilter) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "People Filter",
            style = EyebrowCaps,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PersonFilter.entries.forEach { opt ->
                val label = when (opt) {
                    PersonFilter.All -> "Everyone"
                    PersonFilter.Admin -> "Admin"
                    PersonFilter.Servants -> "Servants"
                    PersonFilter.Members -> "Members"
                }
                FilterChipPill(label, opt == filter) { onFilter(opt) }
            }
        }
    }
}

@Composable
private fun FilterChipPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = PillShape,
        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        border = if (selected) null else androidx.compose.foundation.BorderStroke(
            1.dp, MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun SectionBand(section: DaySection) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 10.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = section.label.uppercase(),
            style = EyebrowCaps,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        MoneyText(
            amount = section.total,
            tone = when {
                section.transferOnly -> MoneyTone.Neutral
                section.total > 0 -> MoneyTone.Income
                section.total < 0 -> MoneyTone.Expense
                else -> MoneyTone.Neutral
            },
            showSign = !section.transferOnly,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (section.transferOnly) MaterialTheme.colorScheme.onSurfaceVariant else Color.Unspecified
        )
    }
}

private fun parseHex(hex: String?): Color? {
    if (hex.isNullOrBlank()) return null
    return runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrNull()
}
