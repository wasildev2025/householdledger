package com.example.householdledger.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.householdledger.data.model.Category
import com.example.householdledger.data.model.Household
import com.example.householdledger.data.model.Member
import com.example.householdledger.data.model.RecurringTemplate
import com.example.householdledger.data.model.Servant
import com.example.householdledger.data.model.Transaction
import com.example.householdledger.data.local.PreferenceManager
import com.example.householdledger.data.repository.AuthRepository
import com.example.householdledger.data.repository.CategoryRepository
import com.example.householdledger.data.repository.HouseholdRepository
import com.example.householdledger.data.repository.PeopleRepository
import com.example.householdledger.data.repository.RecurringRepository
import com.example.householdledger.data.repository.TransactionRepository
import com.example.householdledger.util.Cycle
import com.example.householdledger.util.DateUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

enum class HomeFilter { All, Servants, Members }

/**
 * Reconciliation snapshot for a single servant / member over the active cycle.
 *
 * - [transferredIn]: total transferred TO this person (admin loaded their wallet)
 * - [monthlySpend]: total this person spent from the household's tab
 * - [netBalance]:   transferredIn − monthlySpend
 *     > 0 → wallet has money left
 *     < 0 → person spent out of pocket; admin owes them (|netBalance|)
 */
data class WalletSummary(
    val id: String,
    val name: String,
    val balance: Double,           // DB-stored lifetime balance (reference only)
    val monthlySpend: Double,
    val transferredIn: Double,
    val netBalance: Double,
    val allocation: Double,
    val kind: String               // "servant" | "member"
)

data class HomeUiState(
    val userName: String = "",
    val role: String = "member",
    val today: String = "",
    val balance: Double = 0.0,
    val income: Double = 0.0,
    val expense: Double = 0.0,
    val transfers: Double = 0.0,
    val incomeDeltaPercent: Float = 0f,
    val expenseDeltaPercent: Float = 0f,
    val monthLabel: String = "",
    val budgetCap: Double = 0.0,
    val budgetUsedPercent: Float = 0f,
    val recent: List<TransactionRow> = emptyList(),
    val wallets: List<WalletSummary> = emptyList(),
    val upcomingBills: List<UpcomingBill> = emptyList(),
    val filter: HomeFilter = HomeFilter.All,
    val unreadMessages: Int = 0,
    val isOffline: Boolean = false,
    val isLoading: Boolean = true,
    val aiInsight: String? = null,
    // Cycle Pulse inputs
    val cycleStart: java.time.LocalDate = java.time.LocalDate.now(),
    val cycleEndInclusive: java.time.LocalDate = java.time.LocalDate.now(),
    val cycleDayIndex: Int = 0,       // 0 = first day of cycle
    val cycleLengthDays: Int = 30,
    val projectedExpense: Double = 0.0,
    val projectedOverrunPercent: Float = 0f  // signed: 0f safe, positive = over, negative = under
)

data class TransactionRow(
    val transaction: Transaction,
    val category: Category?
)

data class UpcomingBill(
    val id: String,
    val name: String,
    val amount: Double,
    val dueDate: String,           // "Tomorrow", "Thu, 24 Apr"
    val daysUntil: Int,
    val categoryColorHex: String?
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val peopleRepository: PeopleRepository,
    private val recurringRepository: RecurringRepository,
    private val authRepository: AuthRepository,
    private val householdRepository: HouseholdRepository,
    private val preferenceManager: PreferenceManager
) : ViewModel() {

    private val filter = MutableStateFlow(HomeFilter.All)
    val filterFlow: StateFlow<HomeFilter> = filter.asStateFlow()

    private val aiInsight = MutableStateFlow<String?>(null)
    private val isOffline = MutableStateFlow(false)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val upcomingBillsFlow: Flow<List<UpcomingBill>> = authRepository.currentUser
        .let { userFlow ->
            kotlinx.coroutines.flow.flow {
                userFlow.collect { user ->
                    if (user?.householdId == null) {
                        emit(emptyList())
                    } else {
                        recurringRepository.upcomingBills(user.householdId, days = 14)
                            .collect { templates -> emit(mapUpcoming(templates)) }
                    }
                }
            }
        }

    val uiState: StateFlow<HomeUiState> = combineN(
        transactionRepository.transactions,
        categoryRepository.categories,
        peopleRepository.servants,
        peopleRepository.members,
        authRepository.currentUser,
        filter,
        householdRepository.household,
        aiInsight,
        isOffline,
        upcomingBillsFlow
    ) { arr ->
        @Suppress("UNCHECKED_CAST") val transactions = arr[0] as List<Transaction>
        @Suppress("UNCHECKED_CAST") val categories = arr[1] as List<Category>
        @Suppress("UNCHECKED_CAST") val servants = arr[2] as List<Servant>
        @Suppress("UNCHECKED_CAST") val members = arr[3] as List<Member>
        val user = arr[4] as com.example.householdledger.data.model.UserProfile?
        val f = arr[5] as HomeFilter
        val household = arr[6] as Household?
        val insight = arr[7] as String?
        val offline = arr[8] as Boolean
        @Suppress("UNCHECKED_CAST") val upcoming = arr[9] as List<UpcomingBill>

        val cycleStartDay = household?.cycleStartDay ?: 1
        val userBudget = household?.monthlyBudget ?: 0.0

        val now = LocalDate.now()
        val thisCycle = Cycle.current(now, cycleStartDay)
        val prevCycle = Cycle.previous(now, cycleStartDay)
        val categoryById = categories.associateBy { it.id }

        // Role-based scoping
        val scoped = when (user?.role) {
            "servant" -> transactions.filter { it.servantId == user.servantId }
            "member" -> transactions.filter { it.memberId == user.memberId }
            else -> transactions
        }

        val byFilter = when (f) {
            HomeFilter.All -> scoped
            HomeFilter.Servants -> scoped.filter { it.servantId != null }
            HomeFilter.Members -> scoped.filter { it.memberId != null }
        }

        val thisCycleTxns = byFilter.filter { parseDate(it.date)?.let(thisCycle::contains) == true }
        val prevCycleTxns = byFilter.filter { parseDate(it.date)?.let(prevCycle::contains) == true }

        val income = thisCycleTxns.filter { it.type == "income" }.sumOf { it.amount }
        val expense = thisCycleTxns.filter { it.type == "expense" }.sumOf { it.amount }
        val transfers = thisCycleTxns.filter { it.type == "transfer" }.sumOf { it.amount }
        val prevIncome = prevCycleTxns.filter { it.type == "income" }.sumOf { it.amount }
        val prevExpense = prevCycleTxns.filter { it.type == "expense" }.sumOf { it.amount }

        val recent = byFilter
            .sortedByDescending { parseDate(it.date) ?: LocalDate.MIN.atStartOfDay().toLocalDate() }
            .take(8)
            .map { TransactionRow(it, it.categoryId?.let(categoryById::get)) }

        val budgetCap = if (userBudget > 0) userBudget else income.takeIf { it > 0 } ?: 0.0
        val budgetUsed = if (budgetCap > 0) (expense / budgetCap).toFloat().coerceIn(0f, 1.5f) else 0f

        val wallets = buildWallets(servants, members, thisCycleTxns, f)

        val cycleLengthDays = java.time.temporal.ChronoUnit.DAYS
            .between(thisCycle.start, thisCycle.endExclusive).toInt()
            .coerceAtLeast(1)
        val dayIndex = java.time.temporal.ChronoUnit.DAYS
            .between(thisCycle.start, now).toInt()
            .coerceIn(0, cycleLengthDays - 1)
        val daysElapsedInclusive = (dayIndex + 1).coerceAtLeast(1)
        val projectedExpense = expense / daysElapsedInclusive * cycleLengthDays
        val overrunPercent = if (budgetCap > 0) {
            ((projectedExpense - budgetCap) / budgetCap).toFloat()
        } else 0f

        HomeUiState(
            userName = user?.name?.substringBefore(' ') ?: "there",
            role = user?.role ?: "member",
            today = now.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy")),
            balance = income - expense,
            income = income,
            expense = expense,
            transfers = transfers,
            incomeDeltaPercent = deltaPercent(income, prevIncome),
            expenseDeltaPercent = deltaPercent(expense, prevExpense),
            monthLabel = buildCycleLabel(thisCycle.start, thisCycle.endExclusive.minusDays(1)),
            budgetCap = budgetCap,
            budgetUsedPercent = budgetUsed,
            recent = recent,
            wallets = wallets,
            upcomingBills = upcoming,
            filter = f,
            isOffline = offline,
            isLoading = false,
            aiInsight = insight,
            cycleStart = thisCycle.start,
            cycleEndInclusive = thisCycle.endExclusive.minusDays(1),
            cycleDayIndex = dayIndex,
            cycleLengthDays = cycleLengthDays,
            projectedExpense = projectedExpense,
            projectedOverrunPercent = overrunPercent
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    init {
        refresh()
    }

    fun setFilter(value: HomeFilter) { filter.value = value }
    fun setOffline(offline: Boolean) { isOffline.value = offline }
    fun setInsight(text: String?) { aiInsight.value = text }

    fun refresh() {
        viewModelScope.launch {
            transactionRepository.syncTransactions()
            categoryRepository.syncCategories()
            peopleRepository.syncPeople()
        }
    }

    private fun buildWallets(
        servants: List<Servant>,
        members: List<Member>,
        monthTxns: List<Transaction>,
        filter: HomeFilter
    ): List<WalletSummary> {
        // "Spent" is anything the person charged to the household tab this cycle —
        // expenses and transfers count, income obviously doesn't.
        val spendTxns = monthTxns.filter { it.type != "income" }
        val transferTxns = monthTxns.filter { it.type == "transfer" }

        val servantSpend = spendTxns.filter { it.type == "expense" }
            .groupBy { it.servantId }
            .mapValues { (_, v) -> v.sumOf { it.amount } }
        val servantTransfersIn = transferTxns
            .groupBy { it.servantId }
            .mapValues { (_, v) -> v.sumOf { it.amount } }

        val memberSpend = spendTxns.filter { it.type == "expense" }
            .groupBy { it.memberId }
            .mapValues { (_, v) -> v.sumOf { it.amount } }
        val memberTransfersIn = transferTxns
            .groupBy { it.memberId }
            .mapValues { (_, v) -> v.sumOf { it.amount } }

        val servantWallets = servants.map { s ->
            val transferred = servantTransfersIn[s.id] ?: 0.0
            val spent = servantSpend[s.id] ?: 0.0
            WalletSummary(
                id = s.id,
                name = s.name,
                balance = s.balance,
                monthlySpend = spent,
                transferredIn = transferred,
                netBalance = transferred - spent,
                allocation = s.budget ?: 0.0,
                kind = "servant"
            )
        }
        val memberWallets = members.map { m ->
            val transferred = memberTransfersIn[m.id] ?: 0.0
            val spent = memberSpend[m.id] ?: 0.0
            WalletSummary(
                id = m.id,
                name = m.name,
                balance = 0.0,
                monthlySpend = spent,
                transferredIn = transferred,
                netBalance = transferred - spent,
                allocation = 0.0,
                kind = "member"
            )
        }
        return when (filter) {
            HomeFilter.All -> servantWallets + memberWallets
            HomeFilter.Servants -> servantWallets
            HomeFilter.Members -> memberWallets
        }
    }

    private fun parseDate(raw: String): LocalDate? = DateUtil.parseDate(raw)

    private fun buildCycleLabel(start: LocalDate, endInclusive: LocalDate): String {
        val fmt = DateTimeFormatter.ofPattern("d MMM")
        return "${start.format(fmt)} – ${endInclusive.format(fmt)}"
    }

    private fun deltaPercent(current: Double, previous: Double): Float {
        if (previous <= 0) return if (current > 0) 100f else 0f
        return (((current - previous) / previous) * 100).toFloat()
    }

    private fun mapUpcoming(templates: List<RecurringTemplate>): List<UpcomingBill> {
        val today = LocalDate.now()
        return templates.mapNotNull { t ->
            val next = runCatching { LocalDate.parse(t.nextRun) }.getOrNull() ?: return@mapNotNull null
            val daysUntil = java.time.temporal.ChronoUnit.DAYS.between(today, next).toInt()
            val label = when (daysUntil) {
                0 -> "Today"
                1 -> "Tomorrow"
                in 2..6 -> next.format(DateTimeFormatter.ofPattern("EEE"))
                else -> next.format(DateTimeFormatter.ofPattern("d MMM"))
            }
            UpcomingBill(
                id = t.id,
                name = t.description.ifBlank { "Recurring ${t.type}" },
                amount = t.amount,
                dueDate = label,
                daysUntil = daysUntil,
                categoryColorHex = null
            )
        }
    }
}

@Suppress("UNCHECKED_CAST")
private fun <R> combineN(
    vararg flows: Flow<*>,
    transform: suspend (Array<Any?>) -> R
): Flow<R> {
    val typed: Array<Flow<Any?>> = flows.map { it as Flow<Any?> }.toTypedArray()
    return kotlinx.coroutines.flow.combine(*typed) { values: Array<Any?> ->
        transform(values)
    }
}
