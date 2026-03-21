package com.alpha.core.ai

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.alpha.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

object GeminiClient {

    private const val MODEL = "gemini-2.0-flash"
    private val API_URL get() =
        "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent" +
                "?key=${BuildConfig.GEMINI_API_KEY}"

    private val gson = Gson()
    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * @param imageBytes  Optional JPEG/PNG bytes to send as an inline image part.
     *                    Pass null for text-only requests.
     */
    suspend fun complete(
        userMessage: String,
        systemPrompt: String = "You are a helpful personal assistant.",
        useWebSearch: Boolean = false,
        maxTokens: Int = 1024,
        imageBytes: ByteArray? = null          // NEW
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "your_gemini_api_key_here") {
            delay(1500)
            return@withContext Result.success(
                "🔧 Mock response — add your Gemini API key to local.properties!\n\nQuery: \"$userMessage\""
            )
        }
        runCatching {
            val body = buildRequestBody(userMessage, systemPrompt, useWebSearch, maxTokens, imageBytes)
            val request = Request.Builder()
                .url(API_URL)
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .header("Content-Type", "application/json")
                .build()
            val response = http.newCall(request).execute()
            val responseBody = response.body?.string()
                ?: throw IllegalStateException("Empty response")
            if (!response.isSuccessful)
                throw IllegalStateException("API error ${response.code}: $responseBody")
            extractText(responseBody)
        }
    }

    private fun buildRequestBody(
        userMessage: String,
        systemPrompt: String,
        useWebSearch: Boolean,
        maxTokens: Int,
        imageBytes: ByteArray?
    ): JsonObject = JsonObject().apply {
        add("system_instruction", JsonObject().apply {
            add("parts", JsonArray().apply {
                add(JsonObject().apply { addProperty("text", systemPrompt) })
            })
        })
        add("contents", JsonArray().apply {
            add(JsonObject().apply {
                addProperty("role", "user")
                add("parts", JsonArray().apply {
                    // Image part first (if present) — Gemini handles it better this way
                    if (imageBytes != null) {
                        val compressed = compressToJpeg(imageBytes)
                        val b64 = Base64.encodeToString(compressed, Base64.NO_WRAP)
                        add(JsonObject().apply {
                            add("inline_data", JsonObject().apply {
                                addProperty("mime_type", "image/jpeg")
                                addProperty("data", b64)
                            })
                        })
                    }
                    // Text part
                    add(JsonObject().apply { addProperty("text", userMessage) })
                })
            })
        })
        add("generationConfig", JsonObject().apply {
            addProperty("maxOutputTokens", maxTokens)
        })
        // Note: google_search tool is omitted when an image is attached —
        // Gemini doesn't support grounding + vision in the same request.
        if (useWebSearch && imageBytes == null) {
            add("tools", JsonArray().apply {
                add(JsonObject().apply { add("google_search", JsonObject()) })
            })
        }
    }

    /**
     * Compress raw bytes to JPEG at 85% quality and cap long edge at 1024px
     * to stay well within the inline_data size limit.
     */
    private fun compressToJpeg(bytes: ByteArray): ByteArray {
        val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        val maxEdge = 1024
        val scaled = if (original.width > maxEdge || original.height > maxEdge) {
            val scale = maxEdge.toFloat() / maxOf(original.width, original.height)
            Bitmap.createScaledBitmap(
                original,
                (original.width * scale).toInt(),
                (original.height * scale).toInt(),
                true
            )
        } else original
        return ByteArrayOutputStream().also {
            scaled.compress(Bitmap.CompressFormat.JPEG, 85, it)
        }.toByteArray()
    }

    private fun extractText(json: String): String = try {
        val root = gson.fromJson(json, JsonObject::class.java)
        root.getAsJsonArray("candidates")[0]
            .asJsonObject.getAsJsonObject("content")
            .getAsJsonArray("parts")
            .joinToString("\n") { it.asJsonObject.get("text")?.asString ?: "" }
            .trim().ifEmpty { "No response received." }
    } catch (e: Exception) {
        "Error parsing response: ${e.message}"
    }
}