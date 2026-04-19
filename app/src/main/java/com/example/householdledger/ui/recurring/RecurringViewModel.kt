package com.example.householdledger.ui.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.householdledger.data.model.Category
import com.example.householdledger.data.model.RecurringTemplate
import com.example.householdledger.data.repository.AuthRepository
import com.example.householdledger.data.repository.CategoryRepository
import com.example.householdledger.data.repository.RecurringRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

data class RecurringRow(
    val template: RecurringTemplate,
    val category: Category?
)

data class RecurringListState(
    val items: List<RecurringRow> = emptyList(),
    val isAdmin: Boolean = false,
    val isLoading: Boolean = true
)

@HiltViewModel
class RecurringViewModel @Inject constructor(
    private val repo: RecurringRepository,
    private val categoryRepository: CategoryRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val state: StateFlow<RecurringListState> = authRepository.currentUser
        .flatMapLatest { user ->
            if (user?.householdId == null) flowOf(RecurringListState(isLoading = false))
            else kotlinx.coroutines.flow.combine(
                repo.getTemplates(user.householdId),
                categoryRepository.categories
            ) { templates, cats ->
                val byId = cats.associateBy { it.id }
                RecurringListState(
                    items = templates.map { RecurringRow(it, it.categoryId?.let(byId::get)) }
                        .sortedBy { it.template.nextRun },
                    isAdmin = user.role == "admin",
                    isLoading = false
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RecurringListState())

    init {
        viewModelScope.launch { repo.syncTemplates() }
    }

    fun delete(template: RecurringTemplate) {
        viewModelScope.launch { repo.deleteTemplate(template) }
    }
}

data class AddRecurringState(
    val amount: String = "",
    val description: String = "",
    val type: String = "expense",
    val frequency: String = "monthly",
    val categoryId: String? = null,
    val startDate: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
    val isSaving: Boolean = false,
    val error: String? = null,
    val done: Boolean = false
)

@HiltViewModel
class AddRecurringViewModel @Inject constructor(
    private val repo: RecurringRepository,
    categoryRepository: CategoryRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    val categories: StateFlow<List<Category>> = categoryRepository.categories.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    private val _state = MutableStateFlow(AddRecurringState())
    val state: StateFlow<AddRecurringState> = _state.asStateFlow()

    fun setAmount(v: String) { _state.value = _state.value.copy(amount = v.filter { it.isDigit() || it == '.' }) }
    fun setDescription(v: String) { _state.value = _state.value.copy(description = v) }
    fun setType(v: String) { _state.value = _state.value.copy(type = v) }
    fun setFrequency(v: String) { _state.value = _state.value.copy(frequency = v) }
    fun setCategory(v: String?) { _state.value = _state.value.copy(categoryId = v) }
    fun setStartDate(v: String) { _state.value = _state.value.copy(startDate = v) }

    fun save() {
        val s = _state.value
        val amount = s.amount.toDoubleOrNull() ?: 0.0
        if (amount <= 0) {
            _state.value = s.copy(error = "Enter an amount")
            return
        }
        val householdId = authRepository.currentUser.value?.householdId
        if (householdId == null) {
            _state.value = s.copy(error = "No household")
            return
        }
        viewModelScope.launch {
            _state.value = s.copy(isSaving = true, error = null)
            val template = RecurringTemplate(
                id = UUID.randomUUID().toString(),
                householdId = householdId,
                amount = amount,
                type = s.type,
                categoryId = s.categoryId,
                description = s.description,
                frequency = s.frequency,
                startDate = s.startDate,
                isActive = true,
                createdAt = java.time.Instant.now().toString()
            )
            repo.addTemplate(template)
            _state.value = _state.value.copy(isSaving = false, done = true)
        }
    }
}
