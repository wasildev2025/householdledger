package com.example.householdledger.ui.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.householdledger.ui.components.MoneyText
import com.example.householdledger.ui.components.MoneyTone
import com.example.householdledger.ui.theme.EyebrowCaps
import com.example.householdledger.util.DateUtil

/**
 * Git-graph style Transactions view. Each day has a banner with net total,
 * and each transaction under it is a colored dot on a vertical spine. The
 * spine is a thin 2dp line drawn through the dot column; dots are coloured
 * by the category (fallback: type tone).
 *
 *   ─────────────────────────────────────────
 *   APR 19 · TODAY                 −₨1,700
 *   │
 *   ●  Milk  ·  Groceries · 3:20 PM   −₨200
 *   │
 *   ●  Fuel  ·  Transport · 1:45 PM   −₨1,500
 *   ─────────────────────────────────────────
 */
fun LazyListScope.transactionsTimeline(
    sections: List<DaySection>,
    onRowClick: (com.example.householdledger.data.model.Transaction) -> Unit = {}
) {
    sections.forEachIndexed { sectionIndex, section ->
        item(key = "tl-band-${section.label}") {
            TimelineDayBand(section = section, first = sectionIndex == 0)
        }
        itemsIndexed(
            items = section.rows,
            keyFn = { row -> "tl-${row.transaction.id}" }
        ) { index, row ->
            TimelineEntry(
                row = row,
                isLastInDay = index == section.rows.lastIndex,
                onClick = { onRowClick(row.transaction) }
            )
        }
    }
}

/** Helper — LazyListScope.items(...) with explicit indexing while keeping a stable key. */
private fun <T> LazyListScope.itemsIndexed(
    items: List<T>,
    keyFn: (T) -> Any,
    itemContent: @Composable (Int, T) -> Unit
) {
    items(
        count = items.size,
        key = { idx -> keyFn(items[idx]) }
    ) { idx -> itemContent(idx, items[idx]) }
}

@Composable
private fun TimelineDayBand(section: DaySection, first: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = if (first) 4.dp else 20.dp, bottom = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // The band aligns with the dot column so the spine visually "slots" into it.
            Box(
                modifier = Modifier.width(SpineColumnWidth),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                section.label.uppercase(),
                style = EyebrowCaps,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            MoneyText(
                amount = section.total,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                showSign = true,
                tone = when {
                    section.total > 0 -> MoneyTone.Income
                    section.total < 0 -> MoneyTone.Expense
                    else -> MoneyTone.Neutral
                }
            )
        }
    }
}

@Composable
private fun TimelineEntry(row: TxnListRow, isLastInDay: Boolean, onClick: () -> Unit) {
    val txn = row.transaction
    val tone = when (txn.type) {
        "income" -> MoneyTone.Income
        "expense" -> MoneyTone.Expense
        else -> MoneyTone.Neutral
    }
    val dotColor = parseHexColor(row.category?.color) ?: when (tone) {
        MoneyTone.Income -> com.example.householdledger.ui.theme.appColors.income
        MoneyTone.Expense -> com.example.householdledger.ui.theme.appColors.expense
        MoneyTone.Neutral -> MaterialTheme.colorScheme.primary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.Top
    ) {
        // Spine column: vertical line + dot centered on the row.
        Box(
            modifier = Modifier
                .width(SpineColumnWidth)
                .fillMaxHeight()
        ) {
            // Vertical line — drawn through the entire row height.
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
            // Dot
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(14.dp)
                    .background(MaterialTheme.colorScheme.background, CircleShape)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(10.dp)
                    .background(dotColor, CircleShape)
            )
            // If last in day, mask the line below the dot by drawing a rectangle of the background
            if (isLastInDay) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .width(4.dp)
                        .fillMaxHeight(0.5f)
                        .background(MaterialTheme.colorScheme.background)
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 10.dp, horizontal = 4.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = txn.description.ifBlank {
                            row.category?.name ?: txn.type.replaceFirstChar { it.uppercase() }
                        },
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = buildString {
                            if (row.category != null) append(row.category.name).append(" · ")
                            append(DateUtil.formatLocalTime(txn.date))
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                MoneyText(
                    amount = txn.amount,
                    tone = tone,
                    showSign = tone != MoneyTone.Neutral,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }
}

private val SpineColumnWidth = 36.dp

private fun parseHexColor(hex: String?): Color? {
    if (hex.isNullOrBlank()) return null
    return try {
        val clean = hex.removePrefix("#")
        val value = when (clean.length) {
            6 -> 0xFF000000 or clean.toLong(16)
            8 -> clean.toLong(16)
            else -> return null
        }
        Color(value)
    } catch (_: Exception) {
        null
    }
}

