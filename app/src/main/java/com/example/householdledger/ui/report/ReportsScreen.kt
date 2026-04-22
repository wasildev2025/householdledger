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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BarChart
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
                                    "DAILY SPEND",
                                    style = com.example.householdledger.ui.theme.EyebrowCaps,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "Peak day",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface
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
                            title = "No spending this cycle",
                            description = "When you log expenses, they'll group by category here."
                        )
                    }
                }
            } else {
                item {
                    AppCard(contentPadding = PaddingValues(0.dp)) {
                        Column {
                            state.breakdown.forEachIndexed { index, item ->
                                Box(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                    CategoryBar(
                                        label = item.categoryName,
                                        icon = item.icon,
                                        share = item.share,
                                        amount = item.total,
                                        colorHex = item.colorHex
                                    )
                                }
                                if (index < state.breakdown.lastIndex) {
                                    androidx.compose.material3.HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                        modifier = Modifier.padding(start = 16.dp, end = 16.dp)
                                    )
                                }
                            }
                        }
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        ChevronButton(icon = Icons.Default.ChevronLeft, description = "Previous cycle", enabled = true, onClick = onPrev)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "CYCLE",
                style = com.example.householdledger.ui.theme.EyebrowCaps,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        ChevronButton(
            icon = Icons.Default.ChevronRight,
            description = "Next cycle",
            enabled = canGoForward,
            onClick = onNext
        )
    }
}

@Composable
private fun ChevronButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        enabled = enabled,
        modifier = Modifier.size(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = description,
                tint = if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}

@Composable
private fun SummaryHero(state: ReportsUiState) {
    AppCard(contentPadding = PaddingValues(20.dp)) {
        Column {
            Text(
                "NET BALANCE",
                style = com.example.householdledger.ui.theme.EyebrowCaps,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            if (state.isLoading) {
                Skeleton(height = 40.dp, cornerRadius = 10.dp, modifier = Modifier.fillMaxWidth(0.6f))
            } else {
                MoneyText(
                    amount = state.balance,
                    style = com.example.householdledger.ui.theme.MoneyHero.copy(
                        fontSize = androidx.compose.ui.unit.TextUnit.Unspecified
                    ),
                    color = if (state.balance < 0) appColors.expense else MaterialTheme.colorScheme.onBackground,
                    showSign = true
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FlowStat(
                    label = "Income",
                    amount = state.income,
                    tone = MoneyTone.Income,
                    modifier = Modifier.weight(1f),
                    loading = state.isLoading
                )
                FlowStat(
                    label = "Expense",
                    amount = state.expense,
                    tone = MoneyTone.Expense,
                    modifier = Modifier.weight(1f),
                    loading = state.isLoading
                )
                FlowStat(
                    label = "Transfers",
                    amount = state.transfers,
                    tone = MoneyTone.Neutral,
                    modifier = Modifier.weight(1f),
                    loading = state.isLoading
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
    loading: Boolean = false
) {
    val dotColor = when (tone) {
        MoneyTone.Income -> appColors.income
        MoneyTone.Expense -> appColors.expense
        MoneyTone.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
    }
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
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (loading) {
                Skeleton(height = 18.dp)
            } else {
                MoneyText(
                    amount = amount,
                    tone = tone,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
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
                    val selected = r == state.trendRange
                    Surface(
                        onClick = { onRange(r) },
                        shape = com.example.householdledger.ui.theme.PillShape,
                        color = if (selected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent,
                        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        border = if (selected) null else androidx.compose.foundation.BorderStroke(
                            1.dp, MaterialTheme.colorScheme.outlineVariant
                        )
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
            Spacer(Modifier.height(20.dp))
            BarTrendChart(
                values = state.trend.map { it.expense },
                secondaryValues = state.trend.map { it.income },
                modifier = Modifier.fillMaxWidth(),
                height = 160.dp,
                barColor = appColors.expense,
                secondaryColor = appColors.income
            )
        }
    }
}

@Composable
private fun AiInsightCard(state: ReportsUiState) {
    AppCard(containerColor = MaterialTheme.colorScheme.tertiaryContainer, borderColor = null) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(MaterialTheme.colorScheme.tertiary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (state.aiLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onTertiary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = state.aiInsight ?: "Analyzing your habits...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}
