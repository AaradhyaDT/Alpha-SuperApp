package com.alpha.features.budget

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.alpha.features.budget.models.BudgetState
import com.alpha.features.budget.models.CategoryBudget
import com.alpha.features.budget.models.Transaction
import com.alpha.features.budget.models.TransactionCategory
import com.alpha.features.budget.models.TransactionSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.budgetDataStore by preferencesDataStore("budget_prefs")

class BudgetRepository(private val context: Context) {

    companion object {
        private val KEY_TRANSACTIONS    = stringPreferencesKey("transactions")
        private val KEY_CATEGORY_LIMITS = stringPreferencesKey("category_limits")
        private val KEY_NET_WORTH       = stringPreferencesKey("net_worth")
    }

    val budgetStateFlow: Flow<BudgetState> = context.budgetDataStore.data.map { prefs ->
        val transactions    = prefs[KEY_TRANSACTIONS]?.let    { parseTransactions(it) }    ?: emptyList()
        val categoryBudgets = prefs[KEY_CATEGORY_LIMITS]?.let { parseCategoryBudgets(it) } ?: TransactionCategory.entries.map { CategoryBudget(it) }
        val netWorth        = prefs[KEY_NET_WORTH]?.toDoubleOrNull() ?: 0.0
        BudgetState(transactions = transactions, categoryBudgets = categoryBudgets, netWorthRs = netWorth)
    }

    suspend fun saveTransactions(transactions: List<Transaction>) {
        context.budgetDataStore.edit { prefs ->
            prefs[KEY_TRANSACTIONS] = serializeTransactions(transactions)
        }
    }

    suspend fun saveCategoryBudgets(budgets: List<CategoryBudget>) {
        context.budgetDataStore.edit { prefs ->
            prefs[KEY_CATEGORY_LIMITS] = serializeCategoryBudgets(budgets)
        }
    }

    suspend fun saveNetWorth(amount: Double) {
        context.budgetDataStore.edit { prefs ->
            prefs[KEY_NET_WORTH] = amount.toString()
        }
    }

    // ── Public helpers for DriveSync ──────────────────────────────────────

    fun serializeTransactionsPublic(list: List<Transaction>): String =
        serializeTransactions(list)

    fun parseTransactionsPublic(json: String): List<Transaction> =
        parseTransactions(json)

    // ── Serialization ─────────────────────────────────────────────────────

    private fun serializeTransactions(list: List<Transaction>): String {
        val arr = JSONArray()
        list.forEach { t ->
            arr.put(JSONObject().apply {
                put("id",           t.id)
                put("gmailId",      t.gmailMessageId ?: "")
                put("amount",       t.amount)
                put("category",     t.category.name)
                put("source",       t.source.name)
                put("merchant",     t.merchantName)
                put("note",         t.note)
                put("date",         t.dateEpochMs)
                put("billPhotoPath",t.billPhotoPath ?: "")
            })
        }
        return arr.toString()
    }

    private fun parseTransactions(json: String): List<Transaction> = runCatching {
        val arr = JSONArray(json)
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val categoryStr = o.getString("category")
            val category = runCatching { TransactionCategory.valueOf(categoryStr) }
                .getOrElse { TransactionCategory.OTHER }

            Transaction(
                id             = o.getString("id"),
                gmailMessageId = o.getString("gmailId").ifEmpty { null },
                amount         = o.getDouble("amount"),
                category       = category,
                source         = TransactionSource.valueOf(o.getString("source")),
                merchantName   = o.getString("merchant"),
                note           = o.getString("note"),
                dateEpochMs    = o.getLong("date"),
                billPhotoPath  = o.optString("billPhotoPath").ifEmpty { null }
            )
        }
    }.getOrElse { emptyList() }

    private fun serializeCategoryBudgets(list: List<CategoryBudget>): String {
        val arr = JSONArray()
        list.forEach { b ->
            arr.put(JSONObject().apply {
                put("category", b.category.name)
                put("limit",    b.limitRs)
            })
        }
        return arr.toString()
    }

    private fun parseCategoryBudgets(json: String): List<CategoryBudget> = runCatching {
        val arr = JSONArray(json)
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val categoryStr = o.getString("category")
            val category = runCatching { TransactionCategory.valueOf(categoryStr) }
                .getOrElse { TransactionCategory.OTHER }

            CategoryBudget(
                category = category,
                limitRs  = o.getDouble("limit")
            )
        }
    }.getOrElse { TransactionCategory.entries.map { CategoryBudget(it) } }
}
