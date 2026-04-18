package com.example.householdledger.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.householdledger.data.model.Category
import com.example.householdledger.data.model.Transaction
import com.example.householdledger.data.repository.AuthRepository
import com.example.householdledger.data.repository.CategoryRepository
import com.example.householdledger.data.repository.TransactionRepository
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
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
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
    val canGoForward: Boolean = false,
    val income: Double = 0.0,
    val expense: Double = 0.0,
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
@Serializable
private data class InsightResponse(val insights: String)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    private val authRepository: AuthRepository,
    private val supabaseClient: SupabaseClient
) : ViewModel() {

    private val selectedMonth = MutableStateFlow(YearMonth.now())
    private val trendRange = MutableStateFlow(TrendRange.SixMonths)
    private val aiInsight = MutableStateFlow<String?>(null)
    private val aiLoading = MutableStateFlow(false)

    val uiState: StateFlow<ReportsUiState> = combineFive(
        transactionRepository.transactions,
        categoryRepository.categories,
        selectedMonth,
        trendRange,
        aiInsight
    ) { transactions, categories, month, range, insight ->
        val categoryById = categories.associateBy { it.id }
        val daysInMonth = month.lengthOfMonth()

        val monthTxns = transactions.filter { txn ->
            parseDate(txn.date)?.let { YearMonth.of(it.year, it.monthValue) == month } ?: false
        }
        val prevMonthTxns = transactions.filter { txn ->
            parseDate(txn.date)?.let { YearMonth.of(it.year, it.monthValue) == month.minusMonths(1) } ?: false
        }

        val income = monthTxns.filter { it.type == "income" }.sumOf { it.amount }
        val expense = monthTxns.filter { it.type == "expense" }.sumOf { it.amount }
        val prevIncome = prevMonthTxns.filter { it.type == "income" }.sumOf { it.amount }
        val prevExpense = prevMonthTxns.filter { it.type == "expense" }.sumOf { it.amount }

        val daily = DoubleArray(daysInMonth)
        monthTxns.filter { it.type == "expense" }.forEach { txn ->
            parseDate(txn.date)?.let { date ->
                val idx = date.dayOfMonth - 1
                if (idx in 0 until daysInMonth) daily[idx] += txn.amount
            }
        }

        val totalExpense = max(expense, 1.0)
        val breakdown = monthTxns
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

        val trend = buildTrend(transactions, range)

        ReportsUiState(
            month = month,
            canGoForward = month < YearMonth.now(),
            income = income,
            expense = expense,
            balance = income - expense,
            prevIncome = prevIncome,
            prevExpense = prevExpense,
            dailyExpense = daily.toList(),
            peakDailyExpense = daily.maxOrNull() ?: 0.0,
            breakdown = breakdown,
            transactionCount = monthTxns.size,
            trend = trend,
            trendRange = range,
            aiInsight = insight,
            aiLoading = aiLoading.value,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReportsUiState())

    fun shiftMonth(delta: Int) {
        val next = selectedMonth.value.plusMonths(delta.toLong())
        if (next <= YearMonth.now()) selectedMonth.value = next
    }

    fun setTrendRange(range: TrendRange) { trendRange.value = range }

    fun refreshAiInsight() {
        val householdId = authRepository.currentUser.value?.householdId ?: return
        if (aiLoading.value) return
        viewModelScope.launch {
            aiLoading.value = true
            try {
                val response = supabaseClient.functions.invoke(
                    function = "generate-insights",
                    body = InsightRequest(householdId)
                )
                val data = response.body<InsightResponse>()
                aiInsight.value = data.insights
            } catch (e: Exception) {
                aiInsight.value = "Offline — try again later to see AI insights."
            } finally {
                aiLoading.value = false
            }
        }
    }

    fun formatMonthLabel(month: YearMonth): String =
        month.format(DateTimeFormatter.ofPattern("MMMM yyyy"))

    private fun buildTrend(
        transactions: List<Transaction>,
        range: TrendRange
    ): List<TrendBucket> {
        val now = YearMonth.now()
        val buckets = mutableListOf<TrendBucket>()
        val count = range.months.toInt()
        for (i in count - 1 downTo 0) {
            val ym = now.minusMonths(i.toLong())
            val txns = transactions.filter { parseDate(it.date)?.let { d -> YearMonth.of(d.year, d.monthValue) == ym } ?: false }
            val inc = txns.filter { it.type == "income" }.sumOf { it.amount }
            val exp = txns.filter { it.type == "expense" }.sumOf { it.amount }
            val label = ym.format(DateTimeFormatter.ofPattern(if (count > 6) "MMM" else "MMM"))
            buckets += TrendBucket(label, inc, exp)
        }
        return buckets
    }

    private fun parseDate(raw: String): LocalDate? = try {
        LocalDateTime.parse(raw).toLocalDate()
    } catch (_: DateTimeParseException) {
        try { LocalDate.parse(raw) } catch (_: DateTimeParseException) { null }
    }
}

@Suppress("UNCHECKED_CAST")
private fun <T1, T2, T3, T4, T5, R> combineFive(
    f1: kotlinx.coroutines.flow.Flow<T1>,
    f2: kotlinx.coroutines.flow.Flow<T2>,
    f3: kotlinx.coroutines.flow.Flow<T3>,
    f4: kotlinx.coroutines.flow.Flow<T4>,
    f5: kotlinx.coroutines.flow.Flow<T5>,
    transform: suspend (T1, T2, T3, T4, T5) -> R
) = combine(f1, f2, f3, f4, f5, transform)
