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
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.SwapHoriz
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
import androidx.compose.runtime.remember
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    onBack: (() -> Unit)? = null,
    onAdd: () -> Unit,
    viewModel: TransactionListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()

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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAdd,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) { Icon(Icons.Filled.Add, "Add") }
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
                        Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                            EmptyState(
                                icon = Icons.Outlined.Receipt,
                                title = "No transactions",
                                description = "Add your first transaction to get started."
                            )
                        }
                    }
                } else {
                    state.sections.forEach { section ->
                        item(key = "header-${section.label}") { DayHeader(section) }
                        items(section.rows, key = { it.transaction.id }) { row ->
                            TransactionCard(row)
                        }
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
    AppCard(contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("This month",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(2.dp))
                Text("Spending", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                MoneyText(amount = state.monthSpend,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    tone = MoneyTone.Expense)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Income", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                MoneyText(amount = state.monthIncome,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    tone = MoneyTone.Income)
            }
        }
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
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            section.label,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        MoneyText(
            amount = section.total,
            style = MaterialTheme.typography.labelMedium,
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
private fun FilterBar(filter: TxnFilter, onFilter: (TxnFilter) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TxnFilter.values().forEach { opt ->
            FilterChipPill(opt.name, opt == filter) { onFilter(opt) }
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
            FilterChipPill(opt.name, opt == filter) { onFilter(opt) }
        }
    }
}

@Composable
private fun FilterChipPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun TransactionCard(row: TxnListRow) {
    val txn = row.transaction
    val (icon, tone) = when (txn.type) {
        "income" -> Icons.AutoMirrored.Outlined.TrendingUp to MoneyTone.Income
        "transfer" -> Icons.Outlined.SwapHoriz to MoneyTone.Neutral
        else -> Icons.AutoMirrored.Outlined.TrendingDown to MoneyTone.Expense
    }
    AppCard(contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryLogo(
                name = row.category?.name ?: txn.description,
                colorHex = row.category?.color,
                fallbackIcon = icon
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = row.category?.name ?: txn.description.ifBlank {
                        txn.type.replaceFirstChar { c -> c.uppercase() }
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    formatTime(txn.date),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            MoneyText(
                amount = txn.amount,
                tone = tone,
                showSign = tone != MoneyTone.Neutral,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

private fun parseHex(hex: String?): Color? {
    if (hex.isNullOrBlank()) return null
    return runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrNull()
}

private fun formatTime(raw: String): String {
    val dt = try {
        LocalDateTime.parse(raw)
    } catch (_: DateTimeParseException) {
        try { LocalDate.parse(raw).atStartOfDay() } catch (_: DateTimeParseException) { return raw }
    }
    return dt.format(DateTimeFormatter.ofPattern("h:mm a"))
}
