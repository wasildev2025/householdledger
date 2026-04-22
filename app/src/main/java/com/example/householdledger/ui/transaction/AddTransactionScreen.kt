package com.example.householdledger.ui.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.example.householdledger.data.model.Category
import com.example.householdledger.data.model.Transaction
import com.example.householdledger.ui.components.MoneyText
import com.example.householdledger.ui.components.MoneyTone
import com.example.householdledger.ui.theme.MoneyDisplay
import com.example.householdledger.util.DateUtil
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private enum class TxnType(val key: String, val label: String, val icon: ImageVector) {
    Income("income", "Income", Icons.AutoMirrored.Outlined.TrendingUp),
    Expense("expense", "Expense", Icons.AutoMirrored.Outlined.TrendingDown),
    Transfer("transfer", "Transfer", Icons.Outlined.SwapHoriz)
}

/** Names that mark a category as "income-side" in our heuristic filter. */
private val INCOME_KEYWORDS = listOf(
    "salary", "income", "bonus", "gift", "refund", "dividend",
    "interest", "rental", "rent in", "freelance", "commission",
    "tankhwa", "investment", "reimbursement", "cashback", "tip"
)

private fun Category.looksLikeIncome(): Boolean =
    INCOME_KEYWORDS.any { k -> name.contains(k, ignoreCase = true) }

/** Curated quick-add suggestions for the income dropdown — each maps to a name,
 *  a matching Ionicons key, and a palette colour. Tapping a suggestion creates
 *  the category (if it doesn't already exist) and selects it. */
private data class IncomeSuggestion(val name: String, val icon: String, val color: String)

private val INCOME_SUGGESTIONS = listOf(
    IncomeSuggestion("Salary",        "cash",     "#2F8F6A"),
    IncomeSuggestion("Bonus",         "gift",     "#E8833A"),
    IncomeSuggestion("Freelance",     "wallet",   "#0F766E"),
    IncomeSuggestion("Commission",    "cash",     "#B08448"),
    IncomeSuggestion("Rental income", "home",     "#6366F1"),
    IncomeSuggestion("Investment",    "cash",     "#3B82F6"),
    IncomeSuggestion("Dividend",      "cash",     "#8B5CF6"),
    IncomeSuggestion("Interest",      "cash",     "#EC4899"),
    IncomeSuggestion("Refund",        "receipt",  "#F59E0B"),
    IncomeSuggestion("Reimbursement", "receipt",  "#D14343"),
    IncomeSuggestion("Gift received", "gift",     "#E8833A"),
    IncomeSuggestion("Cashback",      "wallet",   "#2F8F6A"),
    IncomeSuggestion("Tip",           "cash",     "#0EA5E9")
)

/**
 * Recipient of a transfer. Either a servant or a member; persisted into
 * the transaction's [Transaction.servantId] / [Transaction.memberId] slot.
 */
private sealed class Recipient {
    abstract val id: String
    abstract val name: String
    data class ServantR(override val id: String, override val name: String) : Recipient()
    data class MemberR(override val id: String, override val name: String) : Recipient()
}

/**
 * Optional pre-fill for the sheet when it's opened from an "owed amount" flow
 * (e.g. tapping the TOP UP pill on a servant's wallet). Forces Transfer mode
 * and populates amount + recipient up-front.
 */
data class TransferPrefill(
    val servantId: String? = null,
    val memberId: String? = null,
    val amount: Double
)

/**
 * Body of the Add/Edit Transaction modal bottom sheet. The sheet chrome (scrim,
 * drag handle, rounded top) is provided by the ModalBottomSheet in MainActivity.
 *
 * Pass [editing] to operate on an existing transaction — the form prefills from
 * it, save becomes an update, and a Delete button appears.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionSheet(
    onClose: () -> Unit,
    editing: Transaction? = null,
    prefill: TransferPrefill? = null,
    viewModel: AddTransactionViewModel = hiltViewModel()
) {
    // Prefill from `editing` first, then `prefill` (mutually exclusive in practice —
    // the caller never uses both). A prefill forces Transfer mode with a locked amount.
    val keying = editing?.id ?: prefill?.let { "prefill-${it.servantId}-${it.memberId}-${it.amount}" }
    var amount by remember(keying) {
        mutableStateOf(
            editing?.amount?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }
                ?: prefill?.amount?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }
                ?: "0"
        )
    }
    var type by remember(keying) {
        mutableStateOf(
            when {
                prefill != null -> TxnType.Transfer
                editing?.type == "income" -> TxnType.Income
                editing?.type == "transfer" -> TxnType.Transfer
                else -> TxnType.Expense
            }
        )
    }
    val addTxnScope = androidx.compose.runtime.rememberCoroutineScope()

    var selectedCategoryId by remember(keying) { mutableStateOf(editing?.categoryId) }
    var description by remember(keying) { mutableStateOf(editing?.description.orEmpty()) }
    var categoryMenuOpen by remember { mutableStateOf(false) }
    var recipientMenuOpen by remember { mutableStateOf(false) }
    var recipient by remember(keying) {
        mutableStateOf<Recipient?>(null) // resolved once data arrives
    }

    var selectedDate by remember(keying) {
        mutableStateOf(editing?.date?.let { DateUtil.parseDate(it) } ?: LocalDate.now())
    }
    var showDatePicker by remember { mutableStateOf(false) }

    val categories by viewModel.categories.collectAsState()
    val servants by viewModel.servants.collectAsState()
    val members by viewModel.members.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()

    // Resolve recipient once servants/members load — for both edit mode and the
    // Top-Up prefill flow.
    androidx.compose.runtime.LaunchedEffect(editing?.id, prefill, servants, members) {
        val sid = editing?.servantId ?: prefill?.servantId
        val mid = editing?.memberId ?: prefill?.memberId
        recipient = sid?.let { servants.firstOrNull { s -> s.id == it } }
            ?.let { Recipient.ServantR(it.id, it.name) }
            ?: mid?.let { members.firstOrNull { m -> m.id == it } }
                ?.let { Recipient.MemberR(it.id, it.name) }
    }

    val selectedCategory = remember(selectedCategoryId, categories) {
        selectedCategoryId?.let { id -> categories.firstOrNull { it.id == id } }
    }

    val visibleCategories = remember(categories, type) {
        when (type) {
            TxnType.Income -> categories.filter { it.looksLikeIncome() }
            TxnType.Expense -> categories.filter { !it.looksLikeIncome() }
            TxnType.Transfer -> emptyList()
        }
    }

    val amountDouble = amount.toDoubleOrNull() ?: 0.0
    val transferNeedsRecipient = type == TxnType.Transfer && recipient == null
    val canSubmit = amountDouble > 0 && !isSaving && !transferNeedsRecipient
    val dateLabel = remember(selectedDate) { 
        if (selectedDate == LocalDate.now()) "Today" 
        else selectedDate.format(DateTimeFormatter.ofPattern("d MMM yyyy")) 
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 16.dp)
    ) {
        Spacer(Modifier.height(4.dp))
        SegmentedPillTabs(
            selected = type,
            onSelect = {
                type = it
                // Reset category on type change if it no longer fits.
                if (it == TxnType.Income && selectedCategory?.looksLikeIncome() != true) selectedCategoryId = null
                if (it == TxnType.Expense && selectedCategory?.looksLikeIncome() == true) selectedCategoryId = null
                if (it == TxnType.Transfer) selectedCategoryId = null
            }
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Amount",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        MoneyText(
            amount = amountDouble,
            style = MoneyDisplay,
            tone = when (type) {
                TxnType.Income -> MoneyTone.Income
                TxnType.Expense -> MoneyTone.Expense
                TxnType.Transfer -> MoneyTone.Neutral
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        )

        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (type != TxnType.Transfer) {
                Box(modifier = Modifier.weight(1f)) {
                    ChipButton(
                        icon = Icons.Outlined.Category,
                        label = selectedCategory?.name
                            ?: (if (type == TxnType.Income) "Income category" else "Category"),
                        onClick = { categoryMenuOpen = true }
                    )
                    DropdownMenu(
                        expanded = categoryMenuOpen,
                        onDismissRequest = { categoryMenuOpen = false }
                    ) {
                        if (type == TxnType.Income) {
                            // Existing income categories first…
                            visibleCategories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category.name) },
                                    onClick = {
                                        selectedCategoryId = category.id
                                        categoryMenuOpen = false
                                    }
                                )
                            }
                            // …then quick-add suggestions for any that don't exist yet.
                            val existingNames = visibleCategories.map { it.name.lowercase() }.toSet()
                            val missing = INCOME_SUGGESTIONS.filter {
                                it.name.lowercase() !in existingNames
                            }
                            if (missing.isNotEmpty()) {
                                if (visibleCategories.isNotEmpty()) {
                                    androidx.compose.material3.HorizontalDivider()
                                }
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "SUGGESTED",
                                            style = com.example.householdledger.ui.theme.EyebrowCaps,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    onClick = {}, enabled = false
                                )
                                missing.forEach { suggestion ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("+ ", color = MaterialTheme.colorScheme.primary,
                                                    style = MaterialTheme.typography.titleSmall)
                                                Text(suggestion.name)
                                            }
                                        },
                                        onClick = {
                                            categoryMenuOpen = false
                                            addTxnScope.launch {
                                                val created = viewModel.ensureCategory(
                                                    name = suggestion.name,
                                                    icon = suggestion.icon,
                                                    colorHex = suggestion.color
                                                )
                                                if (created != null) selectedCategoryId = created.id
                                            }
                                        }
                                    )
                                }
                            }
                        } else if (visibleCategories.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No matching categories") },
                                onClick = { categoryMenuOpen = false }
                            )
                        } else {
                            visibleCategories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category.name) },
                                    onClick = {
                                        selectedCategoryId = category.id
                                        categoryMenuOpen = false
                                    }
                                )
                            }
                        }
                    }
                }
            } else {
                // Transfer: required recipient picker replaces the category slot.
                Box(modifier = Modifier.weight(1f)) {
                    ChipButton(
                        icon = Icons.Outlined.Person,
                        label = recipient?.name ?: "Transfer to…",
                        onClick = { recipientMenuOpen = true },
                        highlight = transferNeedsRecipient
                    )
                    DropdownMenu(
                        expanded = recipientMenuOpen,
                        onDismissRequest = { recipientMenuOpen = false }
                    ) {
                        if (servants.isEmpty() && members.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Add a servant or member first") },
                                onClick = { recipientMenuOpen = false }
                            )
                        } else {
                            if (servants.isNotEmpty()) {
                                DropdownMenuItem(
                                    text = {
                                        Text("Staff", style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    },
                                    onClick = {}, enabled = false
                                )
                                servants.forEach { s ->
                                    DropdownMenuItem(
                                        text = { Text(s.name) },
                                        onClick = {
                                            recipient = Recipient.ServantR(s.id, s.name)
                                            recipientMenuOpen = false
                                        }
                                    )
                                }
                            }
                            if (members.isNotEmpty()) {
                                DropdownMenuItem(
                                    text = {
                                        Text("Family", style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    },
                                    onClick = {}, enabled = false
                                )
                                members.forEach { m ->
                                    DropdownMenuItem(
                                        text = { Text(m.name) },
                                        onClick = {
                                            recipient = Recipient.MemberR(m.id, m.name)
                                            recipientMenuOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            ChipButton(
                icon = Icons.Outlined.CalendarToday,
                label = dateLabel,
                onClick = { showDatePicker = true },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(14.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it.take(140) },
            placeholder = { Text("Where did it go? e.g. Atif grocery shop") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedBorderColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        NumberPad(
            onDigit = { d ->
                amount = when {
                    amount == "0" && d != "." -> d
                    d == "." && amount.contains(".") -> amount
                    else -> amount + d
                }
            },
            onBackspace = {
                amount = if (amount.length <= 1) "0" else amount.dropLast(1)
            }
        )

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                val isoDate = selectedDate.atStartOfDay().format(DateTimeFormatter.ISO_DATE_TIME)
                val sid = (recipient as? Recipient.ServantR)?.id
                val mid = (recipient as? Recipient.MemberR)?.id
                // Description falls back to the category name if the user didn't type one,
                // so legacy behaviour is preserved for people who don't care about notes.
                val finalDescription = description.trim().ifBlank {
                    selectedCategory?.name.orEmpty()
                }
                if (editing != null) {
                    viewModel.updateTransaction(
                        original = editing,
                        amount = amountDouble,
                        description = finalDescription,
                        type = type.key,
                        categoryId = selectedCategory?.id,
                        date = isoDate,
                        servantId = sid ?: editing.servantId,
                        memberId = mid ?: editing.memberId
                    )
                } else {
                    viewModel.addTransaction(
                        amount = amountDouble,
                        description = finalDescription,
                        type = type.key,
                        categoryId = selectedCategory?.id,
                        date = isoDate,
                        servantId = sid,
                        memberId = mid
                    )
                }
                onClose()
            },
            enabled = canSubmit,
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = if (editing != null) "Save changes" else "Add transaction",
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }

        if (editing != null) {
            Spacer(Modifier.height(10.dp))
            androidx.compose.material3.OutlinedButton(
                onClick = {
                    viewModel.deleteTransaction(editing)
                    onClose()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(999.dp)
            ) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(end = 8.dp).size(18.dp)
                )
                Text(
                    "Delete transaction",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        selectedDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun SegmentedPillTabs(
    selected: TxnType,
    onSelect: (TxnType) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        TxnType.values().forEachIndexed { i, option ->
            val isSelected = option == selected
            val bg = if (isSelected) MaterialTheme.colorScheme.secondary else Color.Transparent
            val fg = if (isSelected) MaterialTheme.colorScheme.onSecondary
            else MaterialTheme.colorScheme.onSurfaceVariant
            Surface(
                onClick = { onSelect(option) },
                shape = RoundedCornerShape(999.dp),
                color = bg,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Text(
                    text = option.label,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = fg,
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp)
                )
            }
            if (i < TxnType.values().lastIndex) Spacer(Modifier.width(0.dp))
        }
    }
}

@Composable
private fun BudgetAlertBanner() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(MaterialTheme.colorScheme.tertiary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Budget tip",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Log every transaction to keep your monthly totals accurate.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ChipButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlight: Boolean = false
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = if (highlight) MaterialTheme.colorScheme.errorContainer
        else MaterialTheme.colorScheme.surfaceVariant,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun NumberPad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf(".", "0", "⌫")
    )
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { key ->
                    NumberKey(
                        key = key,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (key == "⌫") onBackspace() else onDigit(key)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NumberKey(
    key: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
            if (key == "⌫") {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Backspace",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Text(
                    text = key,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
