package com.example.app_translate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

data class DialogueMessage(
    val originalText: String,
    val translatedText: String,
    val isFromMe: Boolean,
    val sourceLangCode: String,
    val targetLangCode: String
)

data class TranslatorUiState(
    val sourceLang: Language = languages[0],
    val targetLang: Language = languages[1],
    val inputText: String = "",
    val outputText: String = "",
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val detectedLanguage: Language? = null,
    val dialogueMessages: List<DialogueMessage> = emptyList(),
    val isAiThinking: Boolean = false
)

class TranslatorViewModel(
    private val repository: TranslateRepository = TranslateRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(TranslatorUiState())
    val uiState: StateFlow<TranslatorUiState> = _uiState.asStateFlow()

    private var translateJob: Job? = null
    private val languageIdentifier = LanguageIdentification.getClient()

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

    private fun triggerTranslate() {
        val state = _uiState.value

        if (state.inputText.isBlank()) {
            _uiState.update {
                it.copy(outputText = "", isLoading = false, isError = false, detectedLanguage = null)
            }
            return
        }

        translateJob?.cancel()
        translateJob = viewModelScope.launch {
            delay(600)

            _uiState.update { it.copy(isLoading = true, isError = false) }

            languageIdentifier.identifyLanguage(state.inputText)
                .addOnSuccessListener { detectedLanguageCode ->
                    val currentSourceCode = _uiState.value.sourceLang.code
                    val detectedLang = languages.find { it.code == detectedLanguageCode }

                    if (detectedLang != null && detectedLanguageCode != "und" && detectedLanguageCode != currentSourceCode) {
                        _uiState.update { it.copy(detectedLanguage = detectedLang) }
                    } else {
                        _uiState.update { it.copy(detectedLanguage = null) }
                    }

                    performTranslation(_uiState.value)
                }
                .addOnFailureListener {
                    performTranslation(_uiState.value)
                }
        }
    }

    private fun performTranslation(state: TranslatorUiState) {
        viewModelScope.launch {
            val result = repository.translate(state.inputText, state.sourceLang.code, state.targetLang.code)
            result.fold(
                onSuccess = { translated ->
                    _uiState.update { it.copy(outputText = translated, isLoading = false) }
                },
                onFailure = {
                    _uiState.update {
                        it.copy(
                            outputText = "Gagal menerjemahkan. Cek koneksi.",
                            isLoading = false,
                            isError = true
                        )
                    }
                }
            )
        }
    }

    fun addDialogueMessage(text: String, isFromMe: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAiThinking = true) }
            val state = _uiState.value
            
            // Logic: User speaks, AI translates.
            // If isFromMe = true, user speaks in sourceLang, AI translates to targetLang.
            // If isFromMe = false, user speaks in targetLang, AI translates back to sourceLang.
            val source = if (isFromMe) state.sourceLang else state.targetLang
            val target = if (isFromMe) state.targetLang else state.sourceLang

            val result = repository.translate(text, source.code, target.code)
            result.onSuccess { translated ->
                val newMessage = DialogueMessage(
                    originalText = text,
                    translatedText = translated,
                    isFromMe = isFromMe,
                    sourceLangCode = source.code,
                    targetLangCode = target.code
                )
                _uiState.update { 
                    it.copy(
                        dialogueMessages = it.dialogueMessages + newMessage,
                        isAiThinking = false
                    ) 
                }
            }
            result.onFailure {
                _uiState.update { it.copy(isAiThinking = false) }
            }
        }
    }
    
    fun clearDialogue() {
        _uiState.update { it.copy(dialogueMessages = emptyList()) }
    }
}
