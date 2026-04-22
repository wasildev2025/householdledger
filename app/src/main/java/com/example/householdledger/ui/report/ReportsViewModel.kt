package com.example.householdledger.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.householdledger.data.model.Category
import com.example.householdledger.data.model.Household
import com.example.householdledger.data.model.Transaction
import com.example.householdledger.data.local.PreferenceManager
import com.example.householdledger.data.repository.AuthRepository
import com.example.householdledger.data.repository.CategoryRepository
import com.example.householdledger.data.repository.HouseholdRepository
import com.example.householdledger.data.repository.TransactionRepository
import com.example.householdledger.util.Cycle
import com.example.householdledger.util.DateUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.ktor.client.call.body
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.math.max

data class CategoryBreakdown(
    val category: Category?,
    val categoryName: String,
    val total: Double,
    val share: Float,
    val colorHex: String?,
    val icon: String
)

enum class TrendRange(val months: Long) { OneMonth(1), SixMonths(6), OneYear(12) }

data class TrendBucket(val label: String, val income: Double, val expense: Double)

data class ReportsUiState(
    val month: YearMonth = YearMonth.now(),
    val cycleStart: LocalDate = LocalDate.now().withDayOfMonth(1),
    val cycleEndInclusive: LocalDate = LocalDate.now(),
    val canGoForward: Boolean = false,
    val income: Double = 0.0,
    val expense: Double = 0.0,
    val transfers: Double = 0.0,
    val balance: Double = 0.0,
    val prevIncome: Double = 0.0,
    val prevExpense: Double = 0.0,
    val dailyExpense: List<Double> = emptyList(),
    val peakDailyExpense: Double = 0.0,
    val breakdown: List<CategoryBreakdown> = emptyList(),
    val transactionCount: Int = 0,
    val trend: List<TrendBucket> = emptyList(),
    val trendRange: TrendRange = TrendRange.SixMonths,
    val aiInsight: String? = null,
    val aiLoading: Boolean = false,
    val isLoading: Boolean = true
)

@Serializable
private data class InsightRequest(val householdId: String)

/** Response shape matches the `generate-insight` edge function exactly. */
@Serializable
private data class InsightResponse(
    val title: String = "",
    val insight: String = "",
    val type: String = "tip"
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val authRepository: AuthRepository,
    private val householdRepository: HouseholdRepository,
    private val supabaseClient: SupabaseClient,
    private val preferenceManager: PreferenceManager
) : ViewModel() {

    // `selectedCycleAnchor` is any LocalDate inside the cycle the user is currently viewing.
    // Cycle boundaries are computed from this anchor + the user's cycleStartDay preference.
    private val selectedCycleAnchor = MutableStateFlow(LocalDate.now())
    private val trendRange = MutableStateFlow(TrendRange.SixMonths)
    private val aiInsight = MutableStateFlow<String?>(null)
    private val aiLoading = MutableStateFlow(false)

    val uiState: StateFlow<ReportsUiState> = combineSix(
        transactionRepository.transactions,
        categoryRepository.categories,
        selectedCycleAnchor,
        trendRange,
        aiInsight,
        householdRepository.household
    ) { transactions, categories, anchor, range, insight, household ->
        val categoryById = categories.associateBy { it.id }
        val cycleStartDay = household?.cycleStartDay ?: 1
        val cycle = Cycle.current(anchor, cycleStartDay)
        val prevCycle = Cycle.previous(anchor, cycleStartDay)
        val lengthDays = java.time.temporal.ChronoUnit.DAYS.between(cycle.start, cycle.endExclusive).toInt()

        val cycleTxns = transactions.filter { parseDate(it.date)?.let(cycle::contains) == true }
        val prevTxns = transactions.filter { parseDate(it.date)?.let(prevCycle::contains) == true }

        val income = cycleTxns.filter { it.type == "income" }.sumOf { it.amount }
        val expense = cycleTxns.filter { it.type == "expense" }.sumOf { it.amount }
        val transfersSum = cycleTxns.filter { it.type == "transfer" }.sumOf { it.amount }
        val prevIncome = prevTxns.filter { it.type == "income" }.sumOf { it.amount }
        val prevExpense = prevTxns.filter { it.type == "expense" }.sumOf { it.amount }

        val daily = DoubleArray(lengthDays)
        cycleTxns.filter { it.type == "expense" }.forEach { txn ->
            parseDate(txn.date)?.let { date ->
                val idx = java.time.temporal.ChronoUnit.DAYS.between(cycle.start, date).toInt()
                if (idx in 0 until lengthDays) daily[idx] += txn.amount
            }
        }

        val totalExpense = max(expense, 1.0)
        val breakdown = cycleTxns
            .filter { it.type == "expense" }
            .groupBy { it.categoryId }
            .map { (catId, txns) ->
                val cat = catId?.let(categoryById::get)
                val total = txns.sumOf { it.amount }
                CategoryBreakdown(
                    category = cat,
                    categoryName = cat?.name ?: "Uncategorized",
                    total = total,
                    share = (total / totalExpense).toFloat().coerceIn(0f, 1f),
                    colorHex = cat?.color,
                    icon = cat?.icon?.ifBlank { (cat.name.take(1).uppercase()) } ?: "•"
                )
            }
            .sortedByDescending { it.total }

        val trend = buildTrend(transactions, range, cycleStartDay)

        ReportsUiState(
            month = YearMonth.from(cycle.start),
            cycleStart = cycle.start,
            cycleEndInclusive = cycle.endExclusive.minusDays(1),
            canGoForward = cycle.endExclusive.isBefore(LocalDate.now()) || cycle.endExclusive.isEqual(LocalDate.now()),
            income = income,
            expense = expense,
            transfers = transfersSum,
            balance = income - expense,
            prevIncome = prevIncome,
            prevExpense = prevExpense,
            dailyExpense = daily.toList(),
            peakDailyExpense = daily.maxOrNull() ?: 0.0,
            breakdown = breakdown,
            transactionCount = cycleTxns.size,
            trend = trend,
            trendRange = range,
            aiInsight = insight,
            aiLoading = aiLoading.value,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReportsUiState())

    fun shiftMonth(delta: Int) {
        val household = householdRepository.household.value
        val startDay = household?.cycleStartDay ?: 1
        val cur = Cycle.current(selectedCycleAnchor.value, startDay)
        val target = cur.start.plusMonths(delta.toLong())
        val today = LocalDate.now()
        val targetCycle = Cycle.current(target, startDay)
        if (!targetCycle.start.isAfter(today)) {
            selectedCycleAnchor.value = target
        }
    }

    fun setTrendRange(range: TrendRange) { trendRange.value = range }

    fun refreshAiInsight() {
        val householdId = authRepository.currentUser.value?.householdId ?: return
        if (aiLoading.value) return
        viewModelScope.launch {
            aiLoading.value = true
            try {
                val response = supabaseClient.functions.invoke(
                    function = "generate-insight",
                    body = InsightRequest(householdId)
                )
                val data = response.body<InsightResponse>()
                val copy = data.insight.ifBlank { data.title }
                aiInsight.value = copy.ifBlank { buildLocalInsight() }
            } catch (e: Exception) {
                // Edge function missing / offline / rate-limited — fall back to a
                // deterministic rule-based insight built from the current state.
                // Users still get value; they just don't get Gemini-quality prose.
                android.util.Log.w("ReportsVM", "AI insight unavailable, using local fallback", e)
                aiInsight.value = buildLocalInsight()
            } finally {
                aiLoading.value = false
            }
        }
    }

    /**
     * Heuristic insight assembled from the current [ReportsUiState] — used
     * whenever the Gemini edge function isn't reachable.
     *
     * Looks at: net balance, top category, vs-previous-cycle delta, peak day.
     * Combines the most informative finding into a single sentence.
     */
    private fun buildLocalInsight(): String {
        val s = uiState.value
        if (s.transactionCount == 0) {
            return "Nothing logged this cycle yet. Add a transaction and I'll spot a pattern."
        }

        val expense = s.expense
        val prevExpense = s.prevExpense
        val income = s.income
        val balance = s.balance

        val lines = mutableListOf<String>()

        // 1. Delta vs previous cycle.
        if (prevExpense > 0 && expense > 0) {
            val change = (expense - prevExpense) / prevExpense
            val pct = kotlin.math.abs(change * 100).toInt()
            when {
                change >= 0.15 -> lines += "You're spending $pct% more than last cycle."
                change <= -0.15 -> lines += "You're spending $pct% less than last cycle — nice."
            }
        }

        // 2. Top category.
        val top = s.breakdown.maxByOrNull { it.total }
        if (top != null && expense > 0) {
            val pct = ((top.total / expense) * 100).toInt()
            if (pct >= 25) {
                lines += "${top.categoryName} alone accounts for $pct% of expenses."
            }
        }

        // 3. Peak day.
        if (s.peakDailyExpense > 0 && expense > 0 && s.peakDailyExpense / expense > 0.25) {
            val pct = ((s.peakDailyExpense / expense) * 100).toInt()
            lines += "Your heaviest day was $pct% of the cycle's spend."
        }

        // 4. Healthy fallback.
        if (lines.isEmpty()) {
            lines += when {
                balance > 0 -> "You're net positive by %.0f this cycle.".format(balance)
                income == 0.0 -> "No income logged this cycle yet — budget will flag as over-pace until you add some."
                else -> "Steady cycle so far — nothing out of the ordinary."
            }
        }

        return lines.joinToString(" ")
    }

    fun formatCycleLabel(start: LocalDate, endInclusive: LocalDate): String {
        val fmt = DateTimeFormatter.ofPattern("d MMM")
        return "${start.format(fmt)} – ${endInclusive.format(fmt)}"
    }

    private fun buildTrend(
        transactions: List<Transaction>,
        range: TrendRange,
        cycleStartDay: Int
    ): List<TrendBucket> {
        val today = LocalDate.now()
        val buckets = mutableListOf<TrendBucket>()
        val count = range.months.toInt()
        for (i in count - 1 downTo 0) {
            val anchor = today.minusMonths(i.toLong())
            val cycle = Cycle.current(anchor, cycleStartDay)
            val txns = transactions.filter { parseDate(it.date)?.let(cycle::contains) == true }
            val inc = txns.filter { it.type == "income" }.sumOf { it.amount }
            val exp = txns.filter { it.type == "expense" }.sumOf { it.amount }
            val label = YearMonth.from(cycle.start).format(DateTimeFormatter.ofPattern("MMM"))
            buckets += TrendBucket(label, inc, exp)
        }
        return buckets
    }

    private fun parseDate(raw: String): LocalDate? = DateUtil.parseDate(raw)
}

@Suppress("UNCHECKED_CAST")
private fun <T1, T2, T3, T4, T5, T6, R> combineSix(
    f1: kotlinx.coroutines.flow.Flow<T1>,
    f2: kotlinx.coroutines.flow.Flow<T2>,
    f3: kotlinx.coroutines.flow.Flow<T3>,
    f4: kotlinx.coroutines.flow.Flow<T4>,
    f5: kotlinx.coroutines.flow.Flow<T5>,
    f6: kotlinx.coroutines.flow.Flow<T6>,
    transform: suspend (T1, T2, T3, T4, T5, T6) -> R
) = combine(f1, f2, f3, f4, f5, f6) { values ->
    transform(
        values[0] as T1,
        values[1] as T2,
        values[2] as T3,
        values[3] as T4,
        values[4] as T5,
        values[5] as T6
    )
}
