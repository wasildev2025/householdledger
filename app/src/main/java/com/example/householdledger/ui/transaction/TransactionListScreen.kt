package com.example.householdledger.ui.transaction

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.householdledger.ui.components.AppCard
import com.example.householdledger.ui.components.CategoryLogo
import com.example.householdledger.ui.components.EmptyState
import com.example.householdledger.ui.components.MoneyText
import com.example.householdledger.ui.components.MoneyTone
import com.example.householdledger.util.DateUtil

private enum class ViewMode { List, Timeline }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    onBack: (() -> Unit)? = null,
    onAdd: () -> Unit,
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                    when (viewMode) {
                        ViewMode.List -> {
                            state.sections.forEach { section ->
                                item(key = "header-${section.label}") { DayHeader(section) }
                                item(key = "group-${section.label}") { DayGroupCard(section.rows) }
                            }
                        }
                        ViewMode.Timeline -> transactionsTimeline(state.sections)
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

private inline fun <T> androidx.compose.foundation.lazy.LazyListScope.items(
    items: List<T>,
    noinline key: (T) -> Any,
    crossinline itemContent: @Composable (T) -> Unit
) {
    items(count = items.size, key = { index -> key(items[index]) }) { index ->
        itemContent(items[index])
    }
}

@Composable
private fun MonthSummaryCard(state: TxnListState) {
    AppCard(contentPadding = PaddingValues(20.dp)) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "SPEND THIS CYCLE",
                    style = com.example.householdledger.ui.theme.EyebrowCaps,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    state.cycleLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            MoneyText(
                amount = state.monthSpend,
                style = com.example.householdledger.ui.theme.MoneyHero.copy(
                    fontSize = androidx.compose.ui.unit.TextUnit.Unspecified
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FlowStat(
                    label = "Income",
                    amount = state.monthIncome,
                    tone = MoneyTone.Income,
                    modifier = Modifier.weight(1f)
                )
                FlowStat(
                    label = "Transfers",
                    amount = state.monthTransfers,
                    tone = MoneyTone.Neutral,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Day ${state.cycleDayIndex + 1} / ${state.cycleLengthDays}",
                    style = com.example.householdledger.ui.theme.EyebrowCaps,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            CycleProgressBar(
                dayIndex = state.cycleDayIndex,
                lengthDays = state.cycleLengthDays
            )
        }
    }
}

@Composable
private fun FlowStat(
    label: String,
    amount: Double,
    tone: MoneyTone,
    modifier: Modifier = Modifier
) {
    val dotColor = when (tone) {
        MoneyTone.Income -> com.example.householdledger.ui.theme.appColors.income
        MoneyTone.Expense -> com.example.householdledger.ui.theme.appColors.expense
        MoneyTone.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(dotColor, androidx.compose.foundation.shape.CircleShape)
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            MoneyText(
                amount = amount,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                tone = tone
            )
        }
    }
}

@Composable
private fun CycleProgressBar(dayIndex: Int, lengthDays: Int) {
    val ratio = if (lengthDays > 0) ((dayIndex + 1f) / lengthDays).coerceIn(0f, 1f) else 0f
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(999.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(ratio)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(999.dp))
        )
    }
}

@Composable
private fun StackedCategoryBar(slices: List<CategorySliceVm>) {
    AppCard(contentPadding = PaddingValues(16.dp)) {
        Column {
            Text("By category", style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth()
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
                                color = MaterialTheme.colorScheme.onSurface
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
private fun DayHeader(section: DaySection) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 16.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            section.label.uppercase(),
            style = com.example.householdledger.ui.theme.EyebrowCaps,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        MoneyText(
            amount = section.total,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            showSign = true,
            tone = when {
                section.total > 0 -> MoneyTone.Income
                section.total < 0 -> MoneyTone.Expense
                else -> MoneyTone.Neutral
            }
        )
    }
}

@Composable
private fun DayGroupCard(rows: List<TxnListRow>) {
    AppCard(contentPadding = PaddingValues(0.dp)) {
        Column {
            rows.forEachIndexed { index, row ->
                TransactionRow(row)
                if (index < rows.lastIndex) {
                    androidx.compose.material3.HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(start = 68.dp)
                    )
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
    AppCard(tonal = true, contentPadding = PaddingValues(24.dp)) {
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
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TxnFilter.values().forEach { opt ->
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

@Composable
private fun PersonFilterBar(filter: PersonFilter, onFilter: (PersonFilter) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PersonFilter.values().forEach { opt ->
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

@Composable
private fun FilterChipPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = com.example.householdledger.ui.theme.PillShape,
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
private fun TransactionRow(row: TxnListRow) {
    val txn = row.transaction
    val (icon, tone) = when (txn.type) {
        "income" -> Icons.AutoMirrored.Outlined.TrendingUp to MoneyTone.Income
        "transfer" -> Icons.Outlined.SwapHoriz to MoneyTone.Neutral
        else -> Icons.AutoMirrored.Outlined.TrendingDown to MoneyTone.Expense
    }
    val title = txn.description.ifBlank {
        row.category?.name ?: txn.type.replaceFirstChar { it.uppercase() }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        CategoryLogo(
            name = row.category?.name ?: txn.description,
            colorHex = row.category?.color,
            iconName = row.category?.icon,
            fallbackIcon = icon
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = buildString {
                    if (row.category != null) append(row.category.name).append(" · ")
                    append(formatTime(txn.date))
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        MoneyText(
            amount = txn.amount,
            tone = tone,
            showSign = tone != MoneyTone.Neutral,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
    }
}

private fun parseHex(hex: String?): Color? {
    if (hex.isNullOrBlank()) return null
    return runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrNull()
}

private fun formatTime(raw: String): String = DateUtil.formatLocalTime(raw)
