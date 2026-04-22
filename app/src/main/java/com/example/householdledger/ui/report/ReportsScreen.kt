package com.example.householdledger.ui.report

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.householdledger.ui.components.AppCard
import com.example.householdledger.ui.components.CategoryBar
import com.example.householdledger.ui.components.EmptyState
import com.example.householdledger.ui.components.LineTrendChart
import com.example.householdledger.ui.components.MultiLineTrendChart
import com.example.householdledger.ui.components.MoneyText
import com.example.householdledger.ui.components.MoneyTone
import com.example.householdledger.ui.components.SectionHeader
import com.example.householdledger.ui.components.Skeleton
import com.example.householdledger.ui.theme.EyebrowCaps
import com.example.householdledger.ui.theme.MoneyDisplay
import com.example.householdledger.ui.theme.MoneyHeadline
import com.example.householdledger.ui.theme.appColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.absoluteValue

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
                AppCard(
                    contentPadding = PaddingValues(16.dp),
                    containerColor = MaterialTheme.colorScheme.surface,
                    borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    elevation = 4.dp,
                    cornerRadius = 24.dp
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "DAILY SPEND",
                                    style = EyebrowCaps,
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
                        LineTrendChart(
                            values = state.dailyExpense,
                            modifier = Modifier.fillMaxWidth(),
                            height = 160.dp,
                            lineColor = appColors.expense,
                            highlightIndex = todayIndexFor(state),
                            startLabel = shortDayLabel(state.cycleStart),
                            midLabel = shortDayLabel(state.cycleStart.plusDays(((state.dailyExpense.size - 1).coerceAtLeast(0) / 2).toLong())),
                            endLabel = shortDayLabel(state.cycleEndInclusive)
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
    val positive = state.balance >= 0
    val accent = if (positive) appColors.warning else appColors.expense
    val onHero = Color(0xFFFDF8EF)
    val secondaryText = onHero.copy(alpha = 0.74f)
    val outline = onHero.copy(alpha = 0.14f)
    val cardShape = RoundedCornerShape(28.dp)
    val balanceDescriptor = if (positive) "In a healthy surplus this cycle" else "Spending is ahead of income this cycle"
    val primaryMetricLabel = if (positive) "Saved" else "Gap"
    val primaryMetricAmount = state.balance.absoluteValue
    val spendRatio = if (state.income > 0.0) (state.expense / state.income) * 100 else null
    val spendRatioText = spendRatio?.let { "${it.toInt()}%" } ?: "--"
    val spendRatioLabel = "Spend ratio"
    val heroGradient = if (positive) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF173D36),
                Color(0xFF0F5C55),
                Color(0xFFB08448)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF421B1B),
                Color(0xFF7A2424),
                Color(0xFFB56A42)
            )
        )
    }

    Surface(
        shape = cardShape,
        color = Color.Transparent,
        shadowElevation = 10.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .clip(cardShape)
                .background(heroGradient)
                .border(1.dp, outline, cardShape)
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(118.dp)
                    .background(onHero.copy(alpha = 0.08f), CircleShape)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 16.dp)
                    .size(64.dp)
                    .background(accent.copy(alpha = 0.16f), CircleShape)
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "NET BALANCE",
                            style = EyebrowCaps,
                            color = secondaryText
                        )
                        Text(
                            text = balanceDescriptor,
                            style = MaterialTheme.typography.bodyMedium,
                            color = onHero,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    StatusPill(
                        text = if (positive) "Surplus" else "Watchlist",
                        textColor = onHero,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }

                if (state.isLoading) {
                    Skeleton(
                        height = 52.dp,
                        cornerRadius = 14.dp,
                        modifier = Modifier.fillMaxWidth(0.62f)
                    )
                } else {
                    MoneyText(
                        amount = state.balance,
                        style = MoneyDisplay,
                        color = onHero,
                        showSign = true
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HeroMetricPill(
                        label = primaryMetricLabel,
                        value = if (state.isLoading) "..." else moneySummary(primaryMetricAmount),
                        modifier = Modifier.weight(1f),
                        contentColor = onHero
                    )
                    HeroMetricPill(
                        label = spendRatioLabel,
                        value = spendRatioText,
                        modifier = Modifier.weight(1f),
                        contentColor = onHero
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PremiumFlowStat(
                        label = "Income",
                        amount = state.income,
                        tone = MoneyTone.Income,
                        modifier = Modifier.weight(1f),
                        loading = state.isLoading,
                        contentColor = onHero,
                        secondaryColor = secondaryText
                    )
                    PremiumFlowStat(
                        label = "Expense",
                        amount = state.expense,
                        tone = MoneyTone.Expense,
                        modifier = Modifier.weight(1f),
                        loading = state.isLoading,
                        contentColor = onHero,
                        secondaryColor = secondaryText
                    )
                    PremiumFlowStat(
                        label = "Transfers",
                        amount = state.transfers,
                        tone = MoneyTone.Neutral,
                        modifier = Modifier.weight(1f),
                        loading = state.isLoading,
                        contentColor = onHero,
                        secondaryColor = secondaryText
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusPill(
    text: String,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.12f))
            .border(1.dp, textColor.copy(alpha = 0.14f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = textColor
        )
    }
}

@Composable
private fun HeroMetricPill(
    label: String,
    value: String,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.10f))
            .border(1.dp, contentColor.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            style = EyebrowCaps,
            color = contentColor.copy(alpha = 0.68f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = contentColor
        )
    }
}

@Composable
private fun PremiumFlowStat(
    label: String,
    amount: Double,
    tone: MoneyTone,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    contentColor: Color,
    secondaryColor: Color
) {
    val dotColor = when (tone) {
        MoneyTone.Income -> appColors.income
        MoneyTone.Expense -> appColors.expense
        MoneyTone.Neutral -> contentColor.copy(alpha = 0.88f)
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, contentColor.copy(alpha = 0.10f), RoundedCornerShape(18.dp))
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(dotColor, CircleShape)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                style = EyebrowCaps,
                color = secondaryColor
            )
        }
        if (loading) {
            Skeleton(height = 18.dp)
        } else {
            MoneyText(
                amount = amount,
                tone = tone,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = if (tone == MoneyTone.Neutral) contentColor else Color.Unspecified
            )
        }
    }
}

private fun moneySummary(amount: Double): String = when {
    amount >= 1000000 -> String.format("%.1fM", amount / 1000000)
    amount >= 1000 -> String.format("%.0fK", amount / 1000)
    else -> amount.toInt().toString()
}

private fun todayIndexFor(state: ReportsUiState): Int? {
    val today = LocalDate.now()
    return if (!today.isBefore(state.cycleStart) && !today.isAfter(state.cycleEndInclusive)) {
        java.time.temporal.ChronoUnit.DAYS.between(state.cycleStart, today).toInt()
    } else null
}

@Composable
private fun TrendCard(state: ReportsUiState, onRange: (TrendRange) -> Unit) {
    val latestBucket = state.trend.lastOrNull()?.label ?: "Latest period"
    val isSingleBucket = state.trend.size <= 1
    val startLabel = state.trend.firstOrNull()?.label ?: "Start"
    val midLabel = if (isSingleBucket) "" else state.trend.getOrNull(state.trend.lastIndex.coerceAtLeast(0) / 2)?.label ?: "Mid"
    val endLabel = if (isSingleBucket) "" else state.trend.lastOrNull()?.label ?: "End"
    val detailLabels = state.trend.map { it.detailLabel }

    AppCard(
        contentPadding = PaddingValues(16.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        elevation = 4.dp,
        cornerRadius = 24.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "FLOW TREND",
                        style = EyebrowCaps,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = latestBucket,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                ) {
                    Text(
                        text = when (state.trendRange) {
                            TrendRange.OneMonth -> "1 month"
                            TrendRange.SixMonths -> "6 months"
                            TrendRange.OneYear -> "12 months"
                        },
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TrendRange.values().forEach { r ->
                    val selected = r == state.trendRange
                    Surface(
                        onClick = { onRange(r) },
                        shape = com.example.householdledger.ui.theme.PillShape,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
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
            Spacer(Modifier.height(4.dp))
            MultiLineTrendChart(
                incomeValues = state.trend.map { it.income },
                expenseValues = state.trend.map { it.expense },
                transferValues = state.trend.map { it.transfers },
                modifier = Modifier.fillMaxWidth(),
                height = 200.dp,
                incomeColor = appColors.income,
                expenseColor = appColors.expense,
                transferColor = MaterialTheme.colorScheme.outline,
                startLabel = startLabel,
                midLabel = midLabel,
                endLabel = endLabel,
                detailLabels = detailLabels
            )
            
            Spacer(Modifier.height(12.dp))
            TrendLegend()
        }
    }
}

private val shortDayFormatter = DateTimeFormatter.ofPattern("d MMM")

private fun shortDayLabel(date: LocalDate): String = date.format(shortDayFormatter)

@Composable
private fun TrendLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendItem("Income", appColors.income)
        Spacer(Modifier.width(16.dp))
        LegendItem("Expense", appColors.expense)
        Spacer(Modifier.width(16.dp))
        LegendItem("Transfers", MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
