package com.alpha.features.budget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

private const val DRIVE_UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files"
private const val DRIVE_FILES_URL  = "https://www.googleapis.com/drive/v3/files"
private const val BILLS_DIR        = "budget_bills"

class BillPhotoManager(private val context: Context) {

    private val http = OkHttpClient()

    // ── Local storage ─────────────────────────────────────────────────────

    /** Saves [bytes] as a compressed JPEG, returns the absolute file path */
    fun saveLocally(transactionId: String, bytes: ByteArray): String {
        val dir = File(context.filesDir, BILLS_DIR).also { it.mkdirs() }
        val file = File(dir, "$transactionId.jpg")
        // Compress to ~80% quality to save space
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
        }
        return file.absolutePath
    }

    /** Deletes the local bill photo for [transactionId] if it exists */
    fun deleteLocally(transactionId: String) {
        File(context.filesDir, "$BILLS_DIR/$transactionId.jpg").delete()
    }

    /** Reads a local bill photo as a Bitmap, returns null if not found */
    fun loadBitmap(path: String): Bitmap? = runCatching {
        BitmapFactory.decodeFile(path)
    }.getOrNull()

    // ── Drive storage ─────────────────────────────────────────────────────

    /** Uploads the bill photo to Drive. No-op if file doesn't exist locally. */
    fun uploadToDrive(token: String, transactionId: String, localPath: String) {
        val file = File(localPath)
        if (!file.exists()) return
        runCatching {
            val bytes    = file.readBytes()
            val filename = "alpha_bill_$transactionId.jpg"
            val existing = findDriveFileId(token, filename)
            if (existing != null) {
                updateDriveFile(token, existing, bytes)
            } else {
                createDriveFile(token, filename, bytes)
            }
        }
    }

    /** Deletes the bill photo from Drive */
    fun deleteFromDrive(token: String, transactionId: String) {
        runCatching {
            val filename = "alpha_bill_$transactionId.jpg"
            val fileId   = findDriveFileId(token, filename) ?: return
            val req = Request.Builder()
                .url("$DRIVE_FILES_URL/$fileId")
                .addHeader("Authorization", "Bearer $token")
                .delete()
                .build()
            http.newCall(req).execute()
        }
    }

    // ── Internal Drive helpers ────────────────────────────────────────────

    private fun findDriveFileId(token: String, filename: String): String? {
        val query = "name='$filename' and trashed=false"
        val url   = "$DRIVE_FILES_URL?q=${java.net.URLEncoder.encode(query, "UTF-8")}&fields=files(id)"
        val req   = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .build()
        val body  = http.newCall(req).execute().body?.string() ?: return null
        val files = JSONObject(body).optJSONArray("files") ?: return null
        return if (files.length() > 0) files.getJSONObject(0).getString("id") else null
    }

    private fun createDriveFile(token: String, filename: String, bytes: ByteArray) {
        val boundary = "bill_boundary"
        val metadata = JSONObject().apply {
            put("name",     filename)
            put("mimeType", "image/jpeg")
        }.toString()

        val body = "--$boundary\r\n" +
                   "Content-Type: application/json\r\n\r\n" +
                   "$metadata\r\n" +
                   "--$boundary\r\n" +
                   "Content-Type: image/jpeg\r\n\r\n"

        val fullBody = (body.toByteArray() + bytes + "\r\n--$boundary--".toByteArray())

        val req = Request.Builder()
            .url("$DRIVE_UPLOAD_URL?uploadType=multipart")
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Content-Type", "multipart/related; boundary=$boundary")
            .post(fullBody.toRequestBody("multipart/related".toMediaType()))
            .build()
        http.newCall(req).execute()
    }

    private fun updateDriveFile(token: String, fileId: String, bytes: ByteArray) {
        val req = Request.Builder()
            .url("$DRIVE_UPLOAD_URL/$fileId?uploadType=media")
            .addHeader("Authorization", "Bearer $token")
            .patch(bytes.toRequestBody("image/jpeg".toMediaType()))
            .build()
        http.newCall(req).execute()
    }
}
