package com.alpha.features.budget

import com.alpha.features.budget.models.Transaction
import com.alpha.features.budget.models.TransactionCategory
import com.alpha.features.budget.models.TransactionSource

object GmailParser {

    // Matches: "Rs. 1,500.00" or "NPR 1500" or "रू 250"
    private val amountRegex = Regex(
        """(?:Rs\.?|NPR|रू)\s*([\d,]+(?:\.\d{1,2})?)""",
        RegexOption.IGNORE_CASE
    )

    // Matches merchant after "paid to" / "Payment to" / "sent to"
    private val merchantRegex = Regex(
        """(?:paid to|payment to|sent to|transferred to)\s*([A-Za-z0-9 &'\-\.]+)""",
        RegexOption.IGNORE_CASE
    )

    fun parse(
        gmailMessageId: String,
        subject: String,
        body: String,
        dateEpochMs: Long
    ): Transaction? {
        val amount = amountRegex.find(body)
            ?.groupValues?.get(1)
            ?.replace(",", "")
            ?.toDoubleOrNull()
            ?: return null   // can't parse amount → skip

        val merchant = merchantRegex.find(body)
            ?.groupValues?.get(1)
            ?.trim()
            ?: ""

        val category = guessCategory(subject, body, merchant)

        return Transaction(
            gmailMessageId = gmailMessageId,
            amount         = amount,
            category       = category,
            source         = TransactionSource.ESEWA,
            merchantName   = merchant,
            note           = subject,
            dateEpochMs    = dateEpochMs
        )
    }

    private fun guessCategory(subject: String, body: String, merchant: String): TransactionCategory {
        val text = "$subject $body $merchant".lowercase()
        return when {
            text.containsAny("service", "repair", "servicing", "maintenance",
                             "glass", "tempered", "cooling pad", "laptop", 
                             "mobile", "electronics", "upgrade", "fix")        -> TransactionCategory.ELECTRONICS_REPAIR
            text.containsAny("food", "restaurant", "cafe", "coffee", "pizza",
                             "burger", "momo", "lunch", "dinner", "breakfast") -> TransactionCategory.FOOD_DRINKS
            text.containsAny("bus", "taxi", "fuel", "petrol", "pathao",
                             "indrive", "transport", "ride")                   -> TransactionCategory.TRANSPORT
            text.containsAny("daraz", "shop", "store", "mart", "purchase",
                             "order", "delivery")                              -> TransactionCategory.SHOPPING
            text.containsAny("electricity", "water", "internet", "wifi",
                             "bill", "nea", "ntc", "ncell", "utility")        -> TransactionCategory.BILLS_UTILITIES
            text.containsAny("school", "college", "tuition", "course",
                             "book", "exam", "fee", "edu")                    -> TransactionCategory.EDUCATION
            text.containsAny("movie", "game", "netflix", "spotify",
                             "concert", "ticket", "entertainment")             -> TransactionCategory.ENTERTAINMENT
            else                                                               -> TransactionCategory.OTHER
        }
    }

    private fun String.containsAny(vararg keywords: String) =
        keywords.any { this.contains(it) }
}
