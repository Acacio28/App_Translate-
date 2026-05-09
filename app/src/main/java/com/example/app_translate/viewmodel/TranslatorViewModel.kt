package com.example.app_translate.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_translate.data.local.AppDatabase
import com.example.app_translate.data.local.HistoryEntity
import com.example.app_translate.data.model.Language
import com.example.app_translate.data.model.languages
import com.example.app_translate.data.repository.TranslateRepository
import com.google.mlkit.nl.languageid.LanguageIdentification
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ✅ BARU: Data class untuk pesan dialogue
data class DialogueMessage(
    val originalText: String,
    val translatedText: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class TranslatorUiState(
    val sourceLang: Language = languages[0],
    val targetLang: Language = languages[1],
    val inputText: String = "",
    val outputText: String = "",
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val detectedLanguage: Language? = null,
    val historyList: List<HistoryEntity> = emptyList(),
    // ✅ BARU: Tambahan untuk fitur Dialogue
    val dialogueMessages: List<DialogueMessage> = emptyList(),
    val isDialogueLoading: Boolean = false
)

class TranslatorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TranslateRepository = TranslateRepository()
    private val db = AppDatabase.getDatabase(application)
    private val historyDao = db.historyDao()

    private val _uiState = MutableStateFlow(TranslatorUiState())
    val uiState: StateFlow<TranslatorUiState> = _uiState.asStateFlow()

    private var translateJob: Job? = null
    private val languageIdentifier = LanguageIdentification.getClient()

    init {
        viewModelScope.launch {
            historyDao.getAllHistory().collect { list ->
                _uiState.update { it.copy(historyList = list) }
            }
        }
    }

    // ─── Fungsi Translate Tab ───────────────────────────────────────

    fun onInputChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
        triggerTranslate()
    }

    fun onSourceLangChanged(lang: Language) {
        _uiState.update { it.copy(sourceLang = lang, detectedLanguage = null) }
        triggerTranslate()
    }

    fun onTargetLangChanged(lang: Language) {
        _uiState.update { it.copy(targetLang = lang) }
        triggerTranslate()
    }

    fun applyDetectedLanguage() {
        val detected = _uiState.value.detectedLanguage
        if (detected != null) {
            _uiState.update { it.copy(sourceLang = detected, detectedLanguage = null) }
            triggerTranslate()
        }
    }

    fun onSwapLanguages() {
        _uiState.update { state ->
            state.copy(
                sourceLang = state.targetLang,
                targetLang = state.sourceLang,
                detectedLanguage = null
            )
        }
        triggerTranslate()
    }

    fun clearHistory() {
        viewModelScope.launch {
            historyDao.clearAll()
        }
    }

    private fun addToHistory(source: String, target: String, sLang: String, tLang: String) {
        if (source.isBlank() || target.isBlank()) return
        viewModelScope.launch {
            historyDao.insertHistory(
                HistoryEntity(
                    sourceText = source,
                    targetText = target,
                    sourceLang = sLang,
                    targetLang = tLang
                )
            )
        }
    }

    private fun triggerTranslate() {
        val state = _uiState.value
        if (state.inputText.isBlank()) {
            _uiState.update { it.copy(outputText = "", isLoading = false, detectedLanguage = null) }
            return
        }

        translateJob?.cancel()
        translateJob = viewModelScope.launch {
            delay(600)
            _uiState.update { it.copy(isLoading = true, isError = false) }

            languageIdentifier.identifyLanguage(state.inputText)
                .addOnSuccessListener { code ->
                    val detected = languages.find { it.code == code }
                    if (detected != null && code != _uiState.value.sourceLang.code) {
                        _uiState.update { it.copy(detectedLanguage = detected) }
                    } else {
                        _uiState.update { it.copy(detectedLanguage = null) }
                    }
                    performTranslation(_uiState.value)
                }
                .addOnFailureListener { performTranslation(_uiState.value) }
        }
    }

    private fun performTranslation(state: TranslatorUiState) {
        viewModelScope.launch {
            val result = repository.translate(
                state.inputText,
                state.sourceLang.code,
                state.targetLang.code
            )
            result.fold(
                onSuccess = { translated ->
                    _uiState.update { it.copy(outputText = translated, isLoading = false) }
                    addToHistory(state.inputText, translated, state.sourceLang.name, state.targetLang.name)
                },
                onFailure = {
                    _uiState.update {
                        it.copy(outputText = "Gagal menerjemahkan.", isLoading = false, isError = true)
                    }
                }
            )
        }
    }

    // ─── ✅ BARU: Fungsi Dialogue Tab ───────────────────────────────

    fun sendDialogueMessage(text: String) {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isDialogueLoading = true) }

            // Terjemahkan pesan user: source → target
            val userTranslation = repository.translate(
                text,
                state.sourceLang.code,
                state.targetLang.code
            )
            val userTranslatedText = userTranslation.getOrElse { "..." }

            // Tambahkan bubble user
            val userMessage = DialogueMessage(
                originalText = text,
                translatedText = userTranslatedText,
                isUser = true
            )
            _uiState.update { it.copy(dialogueMessages = it.dialogueMessages + userMessage) }

            // Delay sedikit agar terasa natural
            delay(800)

            // AI generate balasan lalu terjemahkan balik: target → source
            val aiReplyOriginal = generateAiReply()
            val aiReplyTranslation = repository.translate(
                aiReplyOriginal,
                state.targetLang.code,
                state.sourceLang.code
            )
            val aiTranslatedText = aiReplyTranslation.getOrElse { "..." }

            // Tambahkan bubble AI
            val aiMessage = DialogueMessage(
                originalText = aiReplyOriginal,
                translatedText = aiTranslatedText,
                isUser = false
            )
            _uiState.update {
                it.copy(
                    dialogueMessages = it.dialogueMessages + aiMessage,
                    isDialogueLoading = false
                )
            }
        }
    }

    fun clearDialogue() {
        _uiState.update { it.copy(dialogueMessages = emptyList()) }
    }

    private fun generateAiReply(): String {
        val replies = listOf(
            "That's interesting! Tell me more.",
            "I understand. How can I help you?",
            "Great point! What do you think about that?",
            "I see! Can you explain further?",
            "Sure, I'd be happy to help with that.",
            "Noted! Is there anything else you'd like to discuss?",
            "Really? That's quite fascinating!",
            "Thanks for sharing that with me."
        )
        return replies.random()
    }
}