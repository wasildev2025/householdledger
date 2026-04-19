package com.example.householdledger.util

import com.example.householdledger.data.model.Category
import com.example.householdledger.data.model.Member
import com.example.householdledger.data.model.Servant
import java.time.LocalDate

/**
 * Result of parsing a free-form transaction utterance.
 * Fields are nullable so the caller can show a preview and let the user fill
 * gaps that the parser didn't resolve.
 */
data class ParsedTransaction(
    val amount: Double? = null,
    val type: String = "expense",       // "income" | "expense" | "transfer"
    val description: String = "",
    val categoryId: String? = null,
    val categoryName: String? = null,   // for preview UI when categoryId wasn't matched
    val servantId: String? = null,
    val memberId: String? = null,
    val personName: String? = null,
    val date: LocalDate = LocalDate.now(),
    val raw: String = ""
)

/**
 * Deterministic utterance parser. Built to handle Pakistani household speech
 * (English + romanized Urdu) without an LLM. Not AI — intentionally simple and
 * predictable. The heavy lifting is done by `SpeechRecognizer`; this just
 * slices the recognized text.
 *
 * Supported patterns (examples):
 *   "200 for milk"
 *   "500 rupees groceries from Nasir"
 *   "100 ka petrol"
 *   "salary 50000"
 *   "income 30000 rent"
 *   "transfer 200 to Ali"
 *   "yesterday 400 for fuel"
 */
object TransactionParser {

    private val INCOME_WORDS = setOf(
        "income", "salary", "earned", "received", "credit", "paid me",
        "tankhwa", "maasha"
    )
    private val TRANSFER_WORDS = setOf(
        "transfer", "transferred", "sent to", "gave to", "paid to",
        "diya", "dye"
    )
    private val EXPENSE_WORDS = setOf(
        "spent", "paid", "bought", "expense", "kharch", "kharcha"
    )

    // Money-word suffixes we strip when amount is adjacent to them.
    private val MONEY_WORDS = listOf(
        "rupees", "rupee", "rs", "rs.", "rupay", "rupaye", "₨",
        "ka", "kay", "ke"
    )

    private val AMOUNT_REGEX = Regex("""(?<!\w)(\d[\d,]*)(?:\.(\d+))?(?!\w)""")

    fun parse(
        utterance: String,
        categories: List<Category>,
        servants: List<Servant>,
        members: List<Member>
    ): ParsedTransaction {
        val raw = utterance.trim()
        val lower = raw.lowercase()

        // ---- Amount ----
        val amount = firstAmount(lower)

        // ---- Type ----
        val type = when {
            TRANSFER_WORDS.any { lower.contains(it) } -> "transfer"
            INCOME_WORDS.any { lower.contains(it) } -> "income"
            EXPENSE_WORDS.any { lower.contains(it) } -> "expense"
            else -> "expense"
        }

        // ---- Date ----
        val date = parseDate(lower)

        // ---- Person (servant / member) ----
        val servant = servants.firstOrNull { s ->
            s.name.split(' ', limit = 2).firstOrNull()?.takeIf { it.length >= 3 }
                ?.let { lower.contains(it.lowercase()) } == true
        }
        val member = if (servant == null) members.firstOrNull { m ->
            m.name.split(' ', limit = 2).firstOrNull()?.takeIf { it.length >= 3 }
                ?.let { lower.contains(it.lowercase()) } == true
        } else null

        // ---- Category (by name, then by keyword map) ----
        val nameMatch = categories.firstOrNull { c ->
            c.name.takeIf { it.isNotBlank() }?.let { name ->
                lower.contains(name.lowercase())
            } == true
        }
        val keywordMatch = nameMatch ?: categories.firstOrNull { c ->
            categoryKeywordsFor(c.name).any { kw -> lower.contains(kw) }
        }

        // ---- Description (the non-numeric, non-money tail) ----
        val description = deriveDescription(raw, keywordMatch?.name)

        return ParsedTransaction(
            amount = amount,
            type = type,
            description = description,
            categoryId = keywordMatch?.id,
            categoryName = keywordMatch?.name,
            servantId = servant?.id,
            memberId = member?.id,
            personName = servant?.name ?: member?.name,
            date = date,
            raw = raw
        )
    }

    private fun firstAmount(lower: String): Double? {
        val m = AMOUNT_REGEX.find(lower) ?: return null
        val whole = m.groupValues[1].replace(",", "")
        val frac = m.groupValues.getOrNull(2).orEmpty()
        val raw = if (frac.isBlank()) whole else "$whole.$frac"
        return raw.toDoubleOrNull()
    }

    private fun parseDate(lower: String): LocalDate {
        val today = LocalDate.now()
        return when {
            lower.contains("yesterday") || lower.contains("kal") -> today.minusDays(1)
            lower.contains("day before yesterday") -> today.minusDays(2)
            lower.contains("tomorrow") -> today.plusDays(1)
            lower.contains("last week") -> today.minusWeeks(1)
            else -> today
        }
    }

    private fun deriveDescription(raw: String, knownCategory: String?): String {
        var s = raw
        // Strip a leading/trailing amount token.
        s = s.replace(AMOUNT_REGEX, "").trim()
        // Strip money words.
        MONEY_WORDS.forEach { mw ->
            s = s.replace(Regex("""\b${Regex.escape(mw)}\b""", RegexOption.IGNORE_CASE), "")
        }
        // Strip type markers.
        (INCOME_WORDS + TRANSFER_WORDS + EXPENSE_WORDS).forEach { w ->
            s = s.replace(Regex("""\b${Regex.escape(w)}\b""", RegexOption.IGNORE_CASE), "")
        }
        // Strip common connectors.
        s = s.replace(Regex("""\b(for|to|from|se|ko)\b""", RegexOption.IGNORE_CASE), "")
        s = s.replace(Regex("""\s+"""), " ").trim()
        // If we matched a category, drop it from the description too (avoid duplication).
        if (!knownCategory.isNullOrBlank()) {
            s = s.replace(knownCategory, "", ignoreCase = true).trim()
        }
        return s
    }

    // Built-in keyword → category-name fallback map. Matches common categories
    // even when they're not spelled exactly.
    private fun categoryKeywordsFor(categoryName: String): List<String> {
        val n = categoryName.lowercase()
        return when {
            n.contains("food") || n.contains("grocery") || n.contains("grocer") ->
                listOf("groceries", "grocery", "food", "vegetable", "veg", "sabzi", "fruits")
            n.contains("milk") || n.contains("dairy") ->
                listOf("milk", "yogurt", "dahi", "doodh")
            n.contains("transport") || n.contains("fuel") || n.contains("petrol") ->
                listOf("fuel", "petrol", "diesel", "uber", "careem", "rickshaw", "bus")
            n.contains("health") || n.contains("medic") ->
                listOf("medicine", "pharmacy", "doctor", "hospital", "dawai")
            n.contains("utility") || n.contains("bill") ->
                listOf("electric", "electricity", "gas bill", "water bill", "internet", "wifi")
            n.contains("rent") -> listOf("rent", "kiraya")
            n.contains("salary") || n.contains("income") -> listOf("salary", "tankhwa")
            n.contains("education") || n.contains("school") ->
                listOf("school", "tuition", "fees", "books")
            else -> emptyList()
        }
    }
}
