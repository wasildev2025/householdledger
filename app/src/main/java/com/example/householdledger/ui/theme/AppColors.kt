package com.example.householdledger.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic brand colors that don't fit Material's ColorScheme — income/expense/warning.
 * These persist meaning across brand repalettes; `error` is not a substitute for `expense`
 * because errors are UI problems, not financial ones.
 */
@Immutable
data class AppColors(
    val income: Color,
    val incomeContainer: Color,
    val onIncomeContainer: Color,
    val expense: Color,
    val expenseContainer: Color,
    val onExpenseContainer: Color,
    val warning: Color
)

val LightAppColors = AppColors(
    income = IncomeLight,
    incomeContainer = IncomeLightContainer,
    onIncomeContainer = IncomeOnContainerLight,
    expense = ExpenseLight,
    expenseContainer = ExpenseLightContainer,
    onExpenseContainer = ExpenseOnContainerLight,
    warning = WarningLight
)

val DarkAppColors = AppColors(
    income = IncomeDark,
    incomeContainer = IncomeDarkContainer,
    onIncomeContainer = IncomeOnContainerDark,
    expense = ExpenseDark,
    expenseContainer = ExpenseDarkContainer,
    onExpenseContainer = ExpenseOnContainerDark,
    warning = WarningDark
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }

val appColors: AppColors
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current
