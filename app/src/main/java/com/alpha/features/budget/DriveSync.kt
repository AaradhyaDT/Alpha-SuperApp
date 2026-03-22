package com.alpha.features.budget

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLEncoder

private const val BACKUP_FILENAME = "alpha_budget_backup.json"
private const val DRIVE_FILES_URL = "https://www.googleapis.com/drive/v3/files"
private const val DRIVE_UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files"

class DriveSync(private val context: Context) {

    private val http = OkHttpClient()

    fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    // ── Push: serialize transactions → upload to Drive ────────────────────

    suspend fun pushBackup(token: String, json: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val existingFileId = findBackupFileId(token)
            if (existingFileId != null) {
                updateFile(token, existingFileId, json)
            } else {
                createFile(token, json)
            }
            true
        }.getOrElse { false }
    }

    // ── Pull: download JSON from Drive ────────────────────────────────────

    suspend fun pullBackup(token: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val fileId = findBackupFileId(token) ?: return@runCatching null
            val req = Request.Builder()
                .url("$DRIVE_FILES_URL/$fileId?alt=media")
                .addHeader("Authorization", "Bearer $token")
                .build()
            http.newCall(req).execute().use { response ->
                response.body?.string()
            }
        }.getOrNull()
    }

    // ── Internal helpers ──────────────────────────────────────────────────

    private fun findBackupFileId(token: String): String? {
        val query = "name='$BACKUP_FILENAME' and trashed=false"
        val url   = "$DRIVE_FILES_URL?q=${URLEncoder.encode(query, "UTF-8")}&fields=files(id)"
        val req   = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .build()
        
        http.newCall(req).execute().use { response ->
            val body = response.body?.string() ?: return null
            val files = JSONObject(body).optJSONArray("files") ?: return null
            return if (files.length() > 0) files.getJSONObject(0).getString("id") else null
        }
    }

    private fun createFile(token: String, json: String) {
        val metadata = JSONObject().apply {
            put("name",     BACKUP_FILENAME)
            put("mimeType", "application/json")
        }.toString()

        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addPart(metadata.toRequestBody("application/json".toMediaType()))
            .addPart(json.toRequestBody("application/json".toMediaType()))
            .build()

        val req = Request.Builder()
            .url("$DRIVE_UPLOAD_URL?uploadType=multipart")
            .addHeader("Authorization", "Bearer $token")
            .post(body)
            .build()
        
        http.newCall(req).execute().use { 
            if (!it.isSuccessful) throw Exception("Drive create failed: ${it.code}")
        }
    }

    private fun updateFile(token: String, fileId: String, json: String) {
        val req = Request.Builder()
            .url("$DRIVE_UPLOAD_URL/$fileId?uploadType=media")
            .addHeader("Authorization", "Bearer $token")
            .patch(json.toRequestBody("application/json".toMediaType()))
            .build()
        
        http.newCall(req).execute().use {
            if (!it.isSuccessful) throw Exception("Drive update failed: ${it.code}")
        }
    }
}
