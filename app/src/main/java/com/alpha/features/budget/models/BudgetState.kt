package com.alpha.features.budget.models

data class CategoryBudget(
    val category: TransactionCategory,
    val limitRs: Double = 0.0   // 0 = no limit set
)

data class BudgetState(
    val transactions: List<Transaction> = emptyList(),
    val categoryBudgets: List<CategoryBudget> = TransactionCategory.entries.map { CategoryBudget(it) },
    val netWorthRs: Double = 0.0,
    val isSyncing: Boolean = false,
    val syncError: String? = null,
    val selectedMonthEpochMs: Long = currentMonthStartMs()
)

fun currentMonthStartMs(): Long {
    val cal = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.DAY_OF_MONTH, 1)
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}

fun BudgetState.transactionsThisMonth() =
    transactions.filter { it.dateEpochMs >= selectedMonthEpochMs }

fun BudgetState.totalSpentThisMonth() =
    transactionsThisMonth().sumOf { it.amount }

fun BudgetState.spentByCategory() =
    transactionsThisMonth()
        .groupBy { it.category }
        .mapValues { (_, txns) -> txns.sumOf { it.amount } }

fun BudgetState.totalBudgetLimit() =
    categoryBudgets.sumOf { it.limitRs }

fun BudgetState.remainingBudget() =
    totalBudgetLimit() - totalSpentThisMonth()
