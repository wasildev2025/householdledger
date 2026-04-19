package com.example.householdledger.ui.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.householdledger.data.model.Category
import com.example.householdledger.data.model.Transaction
import com.example.householdledger.data.repository.AuthRepository
import com.example.householdledger.data.repository.CategoryRepository
import com.example.householdledger.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeParseException
import java.util.UUID
import javax.inject.Inject

data class CategoryListItem(
    val category: Category,
    val monthSpent: Double,
    val budgetProgress: Float // 0..1.5 (>1 = over budget)
)

data class CategoryListState(
    val items: List<CategoryListItem> = emptyList(),
    val isAdmin: Boolean = false,
    val isLoading: Boolean = true
)

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    val state: StateFlow<CategoryListState> = combine(
        categoryRepository.categories,
        transactionRepository.transactions,
        authRepository.currentUser
    ) { cats, txns, user ->
        val month = YearMonth.now()
        val spentByCat = txns
            .filter { it.type == "expense" && parsedMonth(it.date) == month }
            .groupBy { it.categoryId }
            .mapValues { (_, list) -> list.sumOf { it.amount } }

        val items = cats.map { cat ->
            val spent = spentByCat[cat.id] ?: 0.0
            val progress = if (cat.budget > 0) (spent / cat.budget).toFloat() else 0f
            CategoryListItem(cat, spent, progress.coerceIn(0f, 1.5f))
        }.sortedByDescending { it.monthSpent }

        CategoryListState(
            items = items,
            isAdmin = user?.role == "admin",
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CategoryListState())

    init {
        viewModelScope.launch { categoryRepository.syncCategories() }
        categoryRepository.subscribeRealtime()
    }

    fun delete(category: Category) {
        viewModelScope.launch { categoryRepository.deleteCategory(category) }
    }

    private fun parseDate(raw: String): LocalDate? = try {
        LocalDateTime.parse(raw).toLocalDate()
    } catch (_: DateTimeParseException) {
        try { LocalDate.parse(raw) } catch (_: DateTimeParseException) { null }
    }

    private fun parsedMonth(raw: String): YearMonth? =
        parseDate(raw)?.let { YearMonth.of(it.year, it.monthValue) }
}

data class AddCategoryState(
    val name: String = "",
    val icon: String = "cart",    // Ionicons key, matches a default IconGroup entry
    val color: String = "#E8833A", // saffron — matches the app's primary accent

    val budget: String = "",
    val isSaving: Boolean = false,
    val error: String? = null,
    val done: Boolean = false
)

@HiltViewModel
class AddCategoryViewModel @Inject constructor(
    private val repo: CategoryRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AddCategoryState())
    val state: StateFlow<AddCategoryState> = _state.asStateFlow()
    private var editingId: String? = null

    fun startEditing(category: Category) {
        editingId = category.id
        _state.value = AddCategoryState(
            name = category.name,
            // Stored icon is an Ionicons key; keep as-is so the picker highlights
            // the same icon when reopened.
            icon = category.icon,
            color = category.color.ifBlank { "#E8833A" },
            budget = if (category.budget > 0) category.budget.toString() else ""
        )
    }

    fun setName(v: String) { _state.value = _state.value.copy(name = v, error = null) }
    fun setIcon(v: String) { _state.value = _state.value.copy(icon = v) }
    fun setColor(v: String) { _state.value = _state.value.copy(color = v) }
    fun setBudget(v: String) { _state.value = _state.value.copy(budget = v.filter { c -> c.isDigit() || c == '.' }) }

    fun save() {
        val s = _state.value
        if (s.name.isBlank()) {
            _state.value = s.copy(error = "Name is required")
            return
        }
        val householdId = authRepository.currentUser.value?.householdId
        if (householdId == null) {
            _state.value = s.copy(error = "No household")
            return
        }
        val budget = s.budget.toDoubleOrNull() ?: 0.0
        viewModelScope.launch {
            _state.value = s.copy(isSaving = true)
            val cat = Category(
                id = editingId ?: UUID.randomUUID().toString(),
                name = s.name.trim(),
                icon = s.icon,
                color = s.color,
                budget = budget,
                householdId = householdId
            )
            if (editingId != null) repo.updateCategory(cat) else repo.addCategory(cat)
            _state.value = _state.value.copy(isSaving = false, done = true)
        }
    }
}
