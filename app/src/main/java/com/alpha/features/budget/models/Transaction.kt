package com.alpha.features.budget.models

import java.util.UUID

enum class TransactionCategory {
    FOOD_DRINKS, TRANSPORT, SHOPPING, BILLS_UTILITIES, EDUCATION, ENTERTAINMENT, ELECTRONICS_REPAIR, OTHER;

    fun displayName() = when (this) {
        FOOD_DRINKS        -> "Food & Drinks"
        TRANSPORT          -> "Transport"
        SHOPPING           -> "Shopping"
        BILLS_UTILITIES    -> "Bills & Utilities"
        EDUCATION          -> "Education"
        ENTERTAINMENT      -> "Entertainment"
        ELECTRONICS_REPAIR -> "Electronics & Repair"
        OTHER              -> "Other"
    }

    fun emoji() = when (this) {
        FOOD_DRINKS        -> "🍜"
        TRANSPORT          -> "🚌"
        SHOPPING           -> "🛍️"
        BILLS_UTILITIES    -> "💡"
        EDUCATION          -> "📚"
        ENTERTAINMENT      -> "🎮"
        ELECTRONICS_REPAIR -> "💻"
        OTHER              -> "📦"
    }
}

enum class TransactionSource { ESEWA, MANUAL }

data class Transaction(
    val id: String = UUID.randomUUID().toString(),
    val gmailMessageId: String? = null,   // for dedup of eSewa emails
    val amount: Double,
    val category: TransactionCategory,
    val source: TransactionSource,
    val merchantName: String = "",
    val note: String = "",
    val dateEpochMs: Long = System.currentTimeMillis(),
    val billPhotoPath: String? = null   // absolute path to local jpg, null if no photo
)
