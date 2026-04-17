package com.example.householdledger.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.example.householdledger.ui.theme.MoneyBody
import com.example.householdledger.ui.theme.appColors
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

enum class MoneyTone { Neutral, Income, Expense }

@Composable
fun MoneyText(
    amount: Double,
    modifier: Modifier = Modifier,
    tone: MoneyTone = MoneyTone.Neutral,
    showSign: Boolean = false,
    currencySymbol: String = "₹",
    style: TextStyle = MoneyBody,
    color: Color = Color.Unspecified
) {
    val formatter = remember {
        NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            maximumFractionDigits = 2
        }
    }
    val absAmount = abs(amount)
    val fractionDigits = if (absAmount % 1.0 == 0.0) 0 else 2
    formatter.minimumFractionDigits = fractionDigits

    val sign = when {
        !showSign -> ""
        tone == MoneyTone.Income -> "+"
        tone == MoneyTone.Expense -> "−"
        amount > 0 -> "+"
        amount < 0 -> "−"
        else -> ""
    }
    val formatted = "$sign$currencySymbol${formatter.format(absAmount)}"

    val resolvedColor = when {
        color != Color.Unspecified -> color
        tone == MoneyTone.Income -> appColors.income
        tone == MoneyTone.Expense -> appColors.expense
        else -> LocalTextStyle.current.color
    }

    Text(
        text = formatted,
        style = style,
        color = resolvedColor,
        modifier = modifier
    )
}
