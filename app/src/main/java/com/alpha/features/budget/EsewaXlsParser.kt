package com.alpha.features.budget

import android.content.Context
import android.net.Uri
import com.alpha.features.budget.models.Transaction
import com.alpha.features.budget.models.TransactionCategory
import com.alpha.features.budget.models.TransactionSource
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Row
import java.text.SimpleDateFormat
import java.util.Locale

data class XlsParseResult(
    val transactions: List<Transaction>,
    val skippedFailed: Int,       // rows with Status != COMPLETE
    val skippedCredits: Int       // Cr. rows (incoming money, not expenses)
)

object EsewaXlsParser {

    fun parse(context: Context, uri: Uri): XlsParseResult {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Cannot open file")

        // eSewa date format: "2026-03-22 14:28:51.0"
        val dateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        val workbook = HSSFWorkbook(inputStream)
        val sheet    = workbook.getSheetAt(0)

        // Row 7 (index 7) is the header row — data starts at row 8 (index 8)
        val transactions   = mutableListOf<Transaction>()
        var skippedFailed  = 0
        var skippedCredits = 0

        for (rowIdx in 8..sheet.lastRowNum) {
            val row = sheet.getRow(rowIdx) ?: continue

            val refCode     = row.getCellValue(0).trim()
            if (refCode.isBlank()) continue
            
            val dateTimeStr = row.getCellValue(1).trim()
            val description = row.getCellValue(2).trim()
            val drStr       = row.getCellValue(3).trim().ifEmpty { "0" }
            val crStr       = row.getCellValue(4).trim().ifEmpty { "0" }
            val status      = row.getCellValue(5).trim()

            // Skip failed transactions
            if (status.uppercase() != "COMPLETE") {
                skippedFailed++
                continue
            }

            val dr = drStr.toDoubleOrNull() ?: 0.0
            val cr = crStr.toDoubleOrNull() ?: 0.0

            // Skip pure credits (incoming money) — not an expense
            if (cr > 0 && dr == 0.0) {
                skippedCredits++
                continue
            }

            // Skip zero-amount rows
            if (dr == 0.0) continue

            // Parse date — strip trailing ".0" that eSewa appends
            val cleanDate = dateTimeStr.replace(Regex("\\.\\d+$"), "")
            val dateMs    = runCatching { dateFmt.parse(cleanDate)?.time }
                .getOrNull() ?: System.currentTimeMillis()

            val category = guessCategory(description)

            transactions.add(
                Transaction(
                    id             = refCode,          // use Reference Code as ID for dedup
                    gmailMessageId = null,
                    amount         = dr,
                    category       = category,
                    source         = TransactionSource.ESEWA,
                    merchantName   = extractMerchant(description),
                    note           = description,
                    dateEpochMs    = dateMs,
                    billPhotoPath  = null
                )
            )
        }

        workbook.close()
        inputStream.close()

        return XlsParseResult(transactions, skippedFailed, skippedCredits)
    }

    private fun Row.getCellValue(index: Int): String {
        val cell = getCell(index) ?: return ""
        return when (cell.cellType) {
            CellType.STRING  -> cell.stringCellValue
            CellType.NUMERIC -> cell.numericCellValue.toString()
            CellType.BOOLEAN -> cell.booleanCellValue.toString()
            CellType.FORMULA -> cell.cellFormula
            else             -> cell.toString()
        }
    }

    private fun extractMerchant(description: String): String {
        // "Paid for K C VARIETY STORES_" → "K C VARIETY STORES"
        return description
            .removePrefix("Paid for ")
            .removePrefix("Payment to ")
            .removePrefix("Fund Transferred to ")
            .trimEnd('_')
            .trim()
    }

    private fun guessCategory(description: String): TransactionCategory {
        val text = description.lowercase()
        return when {
            text.containsAny("food", "restaurant", "cafe", "coffee", "pizza",
                "burger", "momo", "lunch", "dinner", "breakfast",
                "kitchen", "bakery", "hotel", "snack")          -> TransactionCategory.FOOD_DRINKS
            text.containsAny("bus", "taxi", "fuel", "petrol", "pathao",
                "indrive", "transport", "ride", "fare", "ticket",
                "metro", "vehicle")                              -> TransactionCategory.TRANSPORT
            text.containsAny("daraz", "shop", "store", "mart", "purchase",
                "order", "delivery", "variety", "retail", "mall",
                "market", "bazar")                               -> TransactionCategory.SHOPPING
            text.containsAny("electricity", "water", "internet", "wifi",
                "bill", "nea", "ntc", "ncell", "utility",
                "topup", "recharge", "mobile", "broadband")      -> TransactionCategory.BILLS_UTILITIES
            text.containsAny("school", "college", "tuition", "course",
                "book", "exam", "fee", "edu", "university",
                "institute", "library")                          -> TransactionCategory.EDUCATION
            text.containsAny("movie", "game", "netflix", "spotify",
                "concert", "entertainment", "fun", "play",
                "sport", "gym", "fitness")                       -> TransactionCategory.ENTERTAINMENT
            text.containsAny("repair", "service", "electronics", "laptop",
                "phone", "mobile repair", "computer", "technician",
                "hardware", "gadget")                            -> TransactionCategory.ELECTRONICS_REPAIR
            else                                                 -> TransactionCategory.OTHER
        }
    }

    private fun String.containsAny(vararg keywords: String) =
        keywords.any { this.contains(it) }
}
