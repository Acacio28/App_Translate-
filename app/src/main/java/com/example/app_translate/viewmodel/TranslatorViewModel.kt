package com.example.app_translate.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_translate.data.helper.DatabaseHelper
import com.example.app_translate.data.model.Language
import com.example.app_translate.data.model.languages
import com.example.app_translate.data.repository.TranslateRepository
import com.google.mlkit.nl.languageid.LanguageIdentification
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TranslatorUiState(
    val sourceLang: Language = languages[0],
    val targetLang: Language = languages[1],
    val inputText: String = "",
    val outputText: String = "",
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    // 1. TAMBAHKAN INI: Untuk menyimpan saran bahasa yang terdeteksi
    val detectedLanguage: Language? = null
)

class TranslatorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TranslateRepository = TranslateRepository()
    private val dbHelper = DatabaseHelper(application)

    private val _uiState = MutableStateFlow(TranslatorUiState())
    val uiState: StateFlow<TranslatorUiState> = _uiState.asStateFlow()

    private var translateJob: Job? = null
    private val languageIdentifier = LanguageIdentification.getClient()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            val history = dbHelper.getAllMessages()
            _uiState.update { it.copy(dialogueMessages = history) }
        }
    }

    fun onInputChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
        triggerTranslate()
    }

    fun onSourceLangChanged(lang: Language) {
        // Reset detectedLanguage saat user mengganti bahasa secara manual
        _uiState.update { it.copy(sourceLang = lang, detectedLanguage = null) }
        if (_uiState.value.inputText.isNotBlank()) triggerTranslate()
    }

    fun onTargetLangChanged(lang: Language) {
        _uiState.update { it.copy(targetLang = lang) }
        if (_uiState.value.inputText.isNotBlank()) triggerTranslate()
    }

    // 2. TAMBAHKAN INI: Fungsi untuk menerapkan saran bahasa
    fun applyDetectedLanguage() {
        val detected = _uiState.value.detectedLanguage
        if (detected != null) {
            _uiState.update { 
                it.copy(
                    sourceLang = detected, 
                    detectedLanguage = null 
                ) 
            }
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
        if (_uiState.value.inputText.isNotBlank()) triggerTranslate()
    }

    private fun triggerTranslate() {
        val state = _uiState.value
        if (state.inputText.isBlank()) {
            _uiState.update { it.copy(outputText = "", isLoading = false, isError = false, detectedLanguage = null) }
            return
        }

        translateJob?.cancel()
        translateJob = viewModelScope.launch {
            delay(600)
            _uiState.update { it.copy(isLoading = true, isError = false) }

            // 3. UPDATE LOGIKA DETEKSI DI SINI
            languageIdentifier.identifyLanguage(state.inputText)
                .addOnSuccessListener { detectedLanguageCode ->
<<<<<<< Updated upstream
                    val currentSourceCode = _uiState.value.sourceLang.code

                    // Cari apakah kode bahasa yang dideteksi ada di daftar aplikasi kita
                    val detectedLang = languages.find { it.code == detectedLanguageCode }

                    if (detectedLang != null && detectedLanguageCode != "und" && detectedLanguageCode != currentSourceCode) {
                        // Jika terdeteksi bahasa lain, munculkan saran (suggestion)
=======
                    val detectedLang = languages.find { it.code == detectedLanguageCode }
                    if (detectedLang != null && detectedLanguageCode != "und" && detectedLanguageCode != _uiState.value.sourceLang.code) {
>>>>>>> Stashed changes
                        _uiState.update { it.copy(detectedLanguage = detectedLang) }
                    } else {
                        // Jika bahasa cocok atau tidak dikenal, hapus saran
                        _uiState.update { it.copy(detectedLanguage = null) }
                    }
<<<<<<< Updated upstream

                    // Tetap lakukan translasi dengan bahasa yang sedang terpilih
                    performTranslation(_uiState.value)
                }
                .addOnFailureListener {
=======
>>>>>>> Stashed changes
                    performTranslation(_uiState.value)
                }
                .addOnFailureListener { performTranslation(_uiState.value) }
        }
    }

    private fun performTranslation(state: TranslatorUiState) {
        viewModelScope.launch {
            val result = repository.translate(state.inputText, state.sourceLang.code, state.targetLang.code)
            result.fold(
                onSuccess = { translated -> _uiState.update { it.copy(outputText = translated, isLoading = false) } },
                onFailure = { _uiState.update { it.copy(outputText = "Error", isLoading = false, isError = true) } }
            )
        }
    }
<<<<<<< Updated upstream
}
=======

    // UPDATED: Handle Practice Mode (Source or Target)
    fun addDialogueMessage(text: String, isSourceLanguage: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAiThinking = true) }
            val state = _uiState.value
            
            // Determine translation direction
            val fromLangCode = if (isSourceLanguage) state.sourceLang.code else state.targetLang.code
            val toLangCode = if (isSourceLanguage) state.targetLang.code else state.sourceLang.code

            // 1. Add User message
            val userMsg = DialogueMessage(
                originalText = text,
                translatedText = "",
                isFromMe = true,
                sourceLangCode = fromLangCode,
                targetLangCode = toLangCode
            )
            _uiState.update { it.copy(dialogueMessages = it.dialogueMessages + userMsg) }
            dbHelper.saveMessage(userMsg)

            // 2. AI Responds with translation (Chatbot Interaction)
            delay(800)
            val result = repository.translate(text, fromLangCode, toLangCode)
            
            result.onSuccess { translated ->
                val aiMsg = DialogueMessage(
                    originalText = text,
                    translatedText = translated,
                    isFromMe = false,
                    sourceLangCode = fromLangCode,
                    targetLangCode = toLangCode
                )
                
                _uiState.update { 
                    it.copy(
                        dialogueMessages = it.dialogueMessages + aiMsg,
                        isAiThinking = false
                    ) 
                }
                dbHelper.saveMessage(aiMsg)
            }
            
            result.onFailure {
                _uiState.update { it.copy(isAiThinking = false) }
            }
        }
    }
    
    fun clearDialogue() {
        dbHelper.clearHistory()
        _uiState.update { it.copy(dialogueMessages = emptyList()) }
    }

    override fun onCleared() {
        super.onCleared()
        languageIdentifier.close()
    }
}
>>>>>>> Stashed changes
