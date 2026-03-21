package com.alpha.features.websearch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alpha.core.ai.GeminiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class WebSearchUiState(
    val query: String = "",
    val result: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val imageBytes: ByteArray? = null      // NEW — null = no image attached
) {
    // ByteArray doesn't implement equals/hashCode — needed for StateFlow diffing
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WebSearchUiState) return false
        if (query != other.query) return false
        if (result != other.result) return false
        if (isLoading != other.isLoading) return false
        if (error != other.error) return false
        return imageBytes.contentEquals(other.imageBytes)
    }

    override fun hashCode(): Int {
        var result1 = query.hashCode()
        result1 = 31 * result1 + result.hashCode()
        result1 = 31 * result1 + isLoading.hashCode()
        result1 = 31 * result1 + (error?.hashCode() ?: 0)
        result1 = 31 * result1 + (imageBytes?.contentHashCode() ?: 0)
        return result1
    }
}

class WebSearchViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(WebSearchUiState())
    val uiState: StateFlow<WebSearchUiState> = _uiState.asStateFlow()

    fun onQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(query = query, error = null)
    }

    fun onImageAttached(bytes: ByteArray?) {           // NEW
        _uiState.value = _uiState.value.copy(imageBytes = bytes, error = null)
    }

    fun search() {
        val query = _uiState.value.query.trim()
        if (query.isBlank()) return
        val imageBytes = _uiState.value.imageBytes
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, result = "", error = null)
            val systemPrompt = if (imageBytes != null)
                "You are a helpful visual assistant on a mobile device. " +
                        "Analyse the attached image and answer the user's question about it. " +
                        "Be concise and readable on a small screen."
            else
                "You are a concise research assistant on a mobile device. " +
                        "Search the web for the user's query and provide a clear, well-structured summary. " +
                        "Use bullet points where helpful. Keep the response readable on a small screen."

            GeminiClient.complete(
                userMessage    = query,
                systemPrompt   = systemPrompt,
                useWebSearch   = imageBytes == null,   // grounding disabled when image present
                maxTokens      = 1024,
                imageBytes     = imageBytes            // NEW
            ).fold(
                onSuccess = { _uiState.value = _uiState.value.copy(isLoading = false, result = it) },
                onFailure = { _uiState.value = _uiState.value.copy(isLoading = false, error = it.message ?: "Unknown error") }
            )
        }
    }

    fun clear() { _uiState.value = WebSearchUiState() }
}
