package com.example.householdledger.ui.report

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.householdledger.ui.components.AppCard
import com.example.householdledger.ui.components.BarTrendChart
import com.example.householdledger.ui.components.CategoryBar
import com.example.householdledger.ui.components.EmptyState
import com.example.householdledger.ui.components.MoneyText
import com.example.householdledger.ui.components.MoneyTone
import com.example.householdledger.ui.components.SectionHeader
import com.example.householdledger.ui.components.Skeleton
import com.example.householdledger.ui.theme.MoneyHeadline
import com.example.householdledger.ui.theme.appColors
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.refreshAiInsight() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Reports",
                        style = MaterialTheme.typography.titleLarge
                    )
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
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                MonthSelector(
                    label = viewModel.formatCycleLabel(state.cycleStart, state.cycleEndInclusive),
                    canGoForward = state.canGoForward,
                    onPrev = { viewModel.shiftMonth(-1) },
                    onNext = { viewModel.shiftMonth(1) }
                )
            }

            item { SummaryHero(state = state) }

            item {
                AppCard(contentPadding = PaddingValues(20.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Daily Spend",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "Peak",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            MoneyText(
                                amount = state.peakDailyExpense,
                                style = MoneyHeadline,
                                tone = MoneyTone.Expense
                            )
                        }
                        BarTrendChart(
                            values = state.dailyExpense,
                            modifier = Modifier.fillMaxWidth(),
                            height = 120.dp,
                            barColor = appColors.expense,
                            highlightIndex = todayIndexFor(state)
                        )
                    }
                }
            }

            item { SectionHeader(title = "Trend") }
            item { TrendCard(state, viewModel::setTrendRange) }

            item {
                SectionHeader(
                    title = "AI insight",
                    actionLabel = if (state.aiLoading) null else "Refresh",
                    onActionClick = { viewModel.refreshAiInsight() }
                )
            }
            item { AiInsightCard(state) }

            item { SectionHeader(title = "By Category") }

            if (state.breakdown.isEmpty()) {
                item {
                    AppCard(tonal = true) {
                        EmptyState(
                            icon = Icons.Outlined.BarChart,
                            title = "No spending this month",
                            description = "When you log expenses, they'll group by category here."
                        )
                    }
                }
            } else {
                items(state.breakdown, key = { it.categoryName }) { item ->
                    AppCard(contentPadding = PaddingValues(14.dp)) {
                        CategoryBar(
                            label = item.categoryName,
                            icon = item.icon,
                            share = item.share,
                            amount = item.total,
                            colorHex = item.colorHex
                        )
                    }
                }
            }

            item {
                Text(
                    text = "${state.transactionCount} transactions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun MonthSelector(
    label: String,
    canGoForward: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onPrev) {
                Icon(
                    Icons.Default.ChevronLeft,
                    contentDescription = "Previous month",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            IconButton(
                onClick = onNext,
                enabled = canGoForward
            ) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Next month",
                    tint = if (canGoForward) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.outlineVariant
                )
            }
        }
    }
}

@Composable
private fun SummaryHero(state: ReportsUiState) {
    AppCard(contentPadding = PaddingValues(20.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column {
                Text(
                    text = "Net Balance",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (state.isLoading) {
                    Spacer(Modifier.height(6.dp))
                    Skeleton(height = 34.dp, cornerRadius = 10.dp, modifier = Modifier.fillMaxWidth(0.5f))
                } else {
                    MoneyText(
                        amount = state.balance,
                        style = com.example.householdledger.ui.theme.MoneyDisplay,
                        color = if (state.balance < 0) appColors.expense else MaterialTheme.colorScheme.onBackground,
                        showSign = true
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SubStat(
                    label = "Income",
                    amount = state.income,
                    tone = MoneyTone.Income,
                    modifier = Modifier.weight(1f),
                    loading = state.isLoading
                )
                SubStat(
                    label = "Expense",
                    amount = state.expense,
                    tone = MoneyTone.Expense,
                    modifier = Modifier.weight(1f),
                    loading = state.isLoading
                )
            }
        }
    }
}

@Composable
private fun SubStat(
    label: String,
    amount: Double,
    tone: MoneyTone,
    modifier: Modifier = Modifier,
    loading: Boolean = false
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = when (tone) {
            MoneyTone.Income -> appColors.incomeContainer
            MoneyTone.Expense -> appColors.expenseContainer
            MoneyTone.Neutral -> MaterialTheme.colorScheme.surfaceVariant
        }
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
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
            if (loading) {
                Skeleton(height = 22.dp)
            } else {
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
}

private fun todayIndexFor(state: ReportsUiState): Int? {
    val today = LocalDate.now()
    return if (!today.isBefore(state.cycleStart) && !today.isAfter(state.cycleEndInclusive)) {
        java.time.temporal.ChronoUnit.DAYS.between(state.cycleStart, today).toInt()
    } else null
}

@Composable
private fun TrendCard(state: ReportsUiState, onRange: (TrendRange) -> Unit) {
    AppCard(contentPadding = PaddingValues(16.dp)) {
        Column {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TrendRange.values().forEach { r ->
                    Surface(
                        onClick = { onRange(r) },
                        shape = RoundedCornerShape(999.dp),
                        color = if (r == state.trendRange) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (r == state.trendRange) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    ) {
                        Text(
                            when (r) {
                                TrendRange.OneMonth -> "1M"
                                TrendRange.SixMonths -> "6M"
                                TrendRange.OneYear -> "1Y"
                            },
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            if (state.trend.isEmpty()) {
                Text("No data yet", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                TrendGraph(state.trend)
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendDot("Income", appColors.income)
                LegendDot("Expense", appColors.expense)
            }
        }
    }
}

@Composable
private fun TrendGraph(buckets: List<TrendBucket>) {
    val peak = buckets.maxOf { maxOf(it.income, it.expense) }.coerceAtLeast(1.0)
    Row(
        modifier = Modifier.fillMaxWidth().height(140.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        buckets.forEach { b ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    val incH = ((b.income / peak) * 110).toInt().coerceAtLeast(2)
                    val expH = ((b.expense / peak) * 110).toInt().coerceAtLeast(2)
                    Box(
                        modifier = Modifier
                            .width(9.dp)
                            .height(incH.dp)
                            .background(appColors.income, RoundedCornerShape(3.dp))
                    )
                    Box(
                        modifier = Modifier
                            .width(9.dp)
                            .height(expH.dp)
                            .background(appColors.expense, RoundedCornerShape(3.dp))
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    b.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LegendDot(label: String, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).background(color, CircleShape))
        Spacer(Modifier.size(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AiInsightCard(state: ReportsUiState) {
    AppCard(contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier.size(40.dp)
                    .background(MaterialTheme.colorScheme.tertiaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.AutoAwesome, null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.fillMaxWidth().padding(0.dp)) {
                Text("Monthly insight",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary)
                Spacer(Modifier.size(4.dp))
                when {
                    state.aiLoading -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.size(8.dp))
                        Text("Generating…", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    state.aiInsight != null -> Text(
                        state.aiInsight!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    else -> Text(
                        "Tap refresh to generate a summary of your month.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
