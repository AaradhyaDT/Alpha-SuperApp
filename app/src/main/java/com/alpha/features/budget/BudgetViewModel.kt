package com.alpha.features.budget

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alpha.features.budget.models.BudgetState
import com.alpha.features.budget.models.CategoryBudget
import com.alpha.features.budget.models.Transaction
import com.alpha.features.budget.models.TransactionCategory
import com.alpha.features.budget.models.TransactionSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

class BudgetViewModel(app: Application) : AndroidViewModel(app) {

    private val repo             = BudgetRepository(app)
    private val driveSync        = DriveSync(app)
    private val billPhotoManager = BillPhotoManager(app)
    private val http             = OkHttpClient()

    private val _uiState = MutableStateFlow(BudgetState())
    val uiState: StateFlow<BudgetState> = _uiState.asStateFlow()

    var driveAccessToken: String? = null

    init {
        viewModelScope.launch {
            repo.budgetStateFlow.collect { saved ->
                _uiState.update { saved }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            pullFromDriveIfOnline()
        }
    }

    // ── Drive sync ────────────────────────────────────────────────────────

    private suspend fun pullFromDriveIfOnline() {
        val token = driveAccessToken ?: return
        if (!driveSync.isOnline()) return
        runCatching {
            val json = driveSync.pullBackup(token) ?: return
            val driveTransactions = repo.parseTransactionsPublic(json)
            val localTransactions = _uiState.value.transactions
            val merged = (driveTransactions + localTransactions).distinctBy { it.id }
            repo.saveTransactions(merged)
            _uiState.update { it.copy(transactions = merged) }
        }
    }

    private fun pushToDriveIfOnline(transactions: List<Transaction>) {
        val token = driveAccessToken ?: return
        if (!driveSync.isOnline()) return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val json = repo.serializeTransactionsPublic(transactions)
                driveSync.pushBackup(token, json)
            }
        }
    }

    fun setDriveAccessToken(token: String) {
        driveAccessToken = token
        viewModelScope.launch(Dispatchers.IO) { pullFromDriveIfOnline() }
    }

    fun forceDriveSync() {
        val token = driveAccessToken ?: run {
            _uiState.update { it.copy(syncError = "Not signed in to Google") }
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isSyncing = true, syncError = null) }
            runCatching {
                val json    = repo.serializeTransactionsPublic(_uiState.value.transactions)
                val success = driveSync.pushBackup(token, json)
                if (!success) _uiState.update { it.copy(syncError = "Drive backup failed") }
            }.onFailure { e ->
                _uiState.update { it.copy(syncError = e.message) }
            }
            _uiState.update { it.copy(isSyncing = false) }
        }
    }

    // ── Gmail sync ────────────────────────────────────────────────────────

    fun syncEsewaEmails(gmailAccessToken: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isSyncing = true, syncError = null) }
            runCatching {
                val existingGmailIds = _uiState.value.transactions
                    .mapNotNull { it.gmailMessageId }.toSet()

                val messageIds = fetchGmailMessageIds(gmailAccessToken)
                val newTxns    = mutableListOf<Transaction>()

                messageIds.forEach { msgId ->
                    if (msgId in existingGmailIds) return@forEach
                    val (subject, body, dateMs) = fetchGmailMessage(gmailAccessToken, msgId)
                    GmailParser.parse(msgId, subject, body, dateMs)?.let { newTxns.add(it) }
                }

                if (newTxns.isNotEmpty()) {
                    val merged = _uiState.value.transactions + newTxns
                    repo.saveTransactions(merged)
                    _uiState.update { it.copy(transactions = merged) }
                    pushToDriveIfOnline(merged)
                }
            }.onFailure { e ->
                _uiState.update { it.copy(syncError = e.message) }
            }
            _uiState.update { it.copy(isSyncing = false) }
        }
    }

    private fun fetchGmailMessageIds(token: String): List<String> {
        val url = "https://gmail.googleapis.com/gmail/v1/users/me/messages" +
                  "?q=from:esewa&maxResults=100"
        val req = Request.Builder().url(url)
            .addHeader("Authorization", "Bearer $token").build()
        val body = http.newCall(req).execute().body?.string() ?: return emptyList()
        val arr  = JSONObject(body).optJSONArray("messages") ?: return emptyList()
        return (0 until arr.length()).map { arr.getJSONObject(it).getString("id") }
    }

    private data class GmailMessage(val subject: String, val body: String, val dateMs: Long)

    private fun fetchGmailMessage(token: String, msgId: String): GmailMessage {
        val url = "https://gmail.googleapis.com/gmail/v1/users/me/messages/$msgId?format=full"
        val req = Request.Builder().url(url)
            .addHeader("Authorization", "Bearer $token").build()
        val json    = JSONObject(http.newCall(req).execute().body?.string() ?: "{}")
        val payload = json.optJSONObject("payload") ?: JSONObject()
        val headers = payload.optJSONArray("headers") ?: JSONArray()

        var subject = ""
        var dateMs  = System.currentTimeMillis()
        for (i in 0 until headers.length()) {
            val h = headers.getJSONObject(i)
            when (h.getString("name").lowercase()) {
                "subject" -> subject = h.getString("value")
                "date"    -> dateMs  = parseDateHeader(h.getString("value"))
            }
        }
        return GmailMessage(subject, extractBody(payload), dateMs)
    }

    private fun extractBody(payload: JSONObject): String {
        val parts = payload.optJSONArray("parts")
        if (parts != null) {
            for (i in 0 until parts.length()) {
                val part = parts.getJSONObject(i)
                if (part.getString("mimeType") == "text/plain") {
                    val data = part.optJSONObject("body")?.optString("data") ?: continue
                    return String(android.util.Base64.decode(data, android.util.Base64.URL_SAFE))
                }
            }
        }
        return payload.optString("snippet", "")
    }

    private fun parseDateHeader(dateStr: String): Long = runCatching {
        java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", java.util.Locale.ENGLISH)
            .parse(dateStr)?.time ?: System.currentTimeMillis()
    }.getOrElse { System.currentTimeMillis() }

    // ── Manual entry ──────────────────────────────────────────────────────

    fun addManualTransaction(
        amount: Double,
        category: TransactionCategory,
        merchant: String,
        note: String,
        dateMs: Long,
        billPhotoBytes: ByteArray? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val txnId = java.util.UUID.randomUUID().toString()

            // Save photo locally first, get path
            val photoPath = billPhotoBytes?.let {
                billPhotoManager.saveLocally(txnId, it)
            }

            val txn = Transaction(
                id           = txnId,
                amount       = amount,
                category     = category,
                source       = TransactionSource.MANUAL,
                merchantName = merchant,
                note         = note,
                dateEpochMs  = dateMs,
                billPhotoPath = photoPath
            )

            val updated = _uiState.value.transactions + txn
            repo.saveTransactions(updated)
            _uiState.update { it.copy(transactions = updated) }
            pushToDriveIfOnline(updated)

            // Upload photo to Drive in background
            if (photoPath != null) {
                driveAccessToken?.let { token ->
                    if (driveSync.isOnline()) {
                        billPhotoManager.uploadToDrive(token, txnId, photoPath)
                    }
                }
            }
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // Clean up photo
            billPhotoManager.deleteLocally(id)
            driveAccessToken?.let { token ->
                if (driveSync.isOnline()) billPhotoManager.deleteFromDrive(token, id)
            }

            val updated = _uiState.value.transactions.filterNot { it.id == id }
            repo.saveTransactions(updated)
            _uiState.update { it.copy(transactions = updated) }
            pushToDriveIfOnline(updated)
        }
    }

    // ── Budget limits ─────────────────────────────────────────────────────

    fun setCategoryLimit(category: TransactionCategory, limitRs: Double) {
        viewModelScope.launch {
            val updated = _uiState.value.categoryBudgets.map {
                if (it.category == category) it.copy(limitRs = limitRs) else it
            }
            repo.saveCategoryBudgets(updated)
            _uiState.update { it.copy(categoryBudgets = updated) }
        }
    }

    // ── Net worth ─────────────────────────────────────────────────────────

    fun setNetWorth(amount: Double) {
        viewModelScope.launch {
            repo.saveNetWorth(amount)
            _uiState.update { it.copy(netWorthRs = amount) }
        }
    }

    // ── Month navigation ──────────────────────────────────────────────────

    fun previousMonth() = shiftMonth(-1)
    fun nextMonth()     = shiftMonth(1)

    private fun shiftMonth(delta: Int) {
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = _uiState.value.selectedMonthEpochMs
            add(java.util.Calendar.MONTH, delta)
        }
        _uiState.update { it.copy(selectedMonthEpochMs = cal.timeInMillis) }
    }
}
