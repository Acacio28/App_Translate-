package com.example.app_translate.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_translate.data.local.AppDatabase
import com.example.app_translate.data.local.HistoryEntity
import com.example.app_translate.data.model.DictionaryEntry
import com.example.app_translate.data.model.Language
import com.example.app_translate.data.model.languages
import com.example.app_translate.data.repository.DictionaryRepository
import com.example.app_translate.data.repository.TranslateRepository
import com.google.mlkit.nl.languageid.LanguageIdentification
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DialogueMessage(
    val originalText: String,
    val translatedText: String,
    val sourceLangCode: String,
    val targetLangCode: String,
    val isFromMe: Boolean = true
)

data class TranslatorUiState(
    val sourceLang: Language = languages[0],
    val targetLang: Language = languages[9],
    val inputText: String = "",
    val outputText: String = "",
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val detectedLanguage: Language? = null,
    val isFavorited: Boolean = false,
    val historyList: List<HistoryEntity> = emptyList(),
    val dialogueMessages: List<DialogueMessage> = emptyList(),
    // --- Dictionary State ---
    val dictionaryResult: DictionaryEntry? = null,
    val isDictionaryLoading: Boolean = false,
    val isDictionaryError: Boolean = false
)

class TranslatorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TranslateRepository()
    private val dictionaryRepository = DictionaryRepository()
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

    // --- Translate Functions ---
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
                detectedLanguage = null,
                isFavorited = false
            )
        }
        triggerTranslate()
    }

    fun toggleFavorite() {
        val state = _uiState.value
        if (state.outputText.isBlank()) return
        viewModelScope.launch {
            val existing = historyDao.findHistory(
                state.inputText, state.outputText,
                state.sourceLang.name, state.targetLang.name
            )
            if (existing != null) {
                historyDao.setFavorite(existing.id, !state.isFavorited)
                _uiState.update { it.copy(isFavorited = !it.isFavorited) }
            } else {
                historyDao.insertHistory(
                    HistoryEntity(
                        sourceText = state.inputText,
                        targetText = state.outputText,
                        sourceLang = state.sourceLang.name,
                        targetLang = state.targetLang.name,
                        isFavorite = true
                    )
                )
                _uiState.update { it.copy(isFavorited = true) }
            }
        }
    }

    fun toggleFavoriteFromHistory(id: Int) {
        viewModelScope.launch {
            val entry = historyDao.findHistoryById(id)
            if (entry != null) {
                historyDao.setFavorite(id, !entry.isFavorite)
            }
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
                        _uiState.update { it.copy(sourceLang = detected, detectedLanguage = null) }
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
                state.sourceLang.apiCode,
                state.targetLang.apiCode
            )
            result.fold(
                onSuccess = { translated ->
                    _uiState.update { it.copy(outputText = translated, isLoading = false, isFavorited = false) }
                    addToHistory(
                        state.inputText,
                        translated,
                        state.sourceLang.name,
                        state.targetLang.name
                    )
                    val existing = historyDao.findHistory(
                        state.inputText, translated,
                        state.sourceLang.name, state.targetLang.name
                    )
                    if (existing?.isFavorite == true) {
                        _uiState.update { it.copy(isFavorited = true) }
                    }
                },
                onFailure = {
                    _uiState.update {
                        it.copy(
                            outputText = "Translation failed.",
                            isLoading = false,
                            isError = true
                        )
                    }
                }
            )
        }
    }

    // --- Dialogue Functions ---
    fun addDialogueMessage(text: String, isFromMe: Boolean) {
        val state = _uiState.value
        viewModelScope.launch {
            val result = repository.translate(
                text,
                state.sourceLang.apiCode,
                state.targetLang.apiCode
            )
            result.fold(
                onSuccess = { translated ->
                    val message = DialogueMessage(
                        originalText = text,
                        translatedText = translated,
                        sourceLangCode = state.sourceLang.apiCode,
                        targetLangCode = state.targetLang.apiCode,
                        isFromMe = isFromMe
                    )
                    _uiState.update { it.copy(dialogueMessages = it.dialogueMessages + message) }
                },
                onFailure = {}
            )
        }
    }

    fun clearDialogue() {
        _uiState.update { it.copy(dialogueMessages = emptyList()) }
    }

    // --- History Functions ---
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

    fun clearHistory() {
        viewModelScope.launch { historyDao.clearAll() }
    }

    // --- Dictionary Functions ---
    fun lookupWord(word: String) {
        if (word.isBlank()) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isDictionaryLoading = true,
                    isDictionaryError = false,
                    dictionaryResult = null
                )
            }
            val result = dictionaryRepository.lookup(word)
            result.fold(
                onSuccess = { entry ->
                    _uiState.update {
                        it.copy(
                            dictionaryResult = entry,
                            isDictionaryLoading = false
                        )
                    }
                },
                onFailure = {
                    _uiState.update {
                        it.copy(
                            isDictionaryError = true,
                            isDictionaryLoading = false
                        )
                    }
                }
            )
        }
    }

    fun clearDictionaryResult() {
        _uiState.update {
            it.copy(
                dictionaryResult = null,
                isDictionaryError = false
            )
        }
    }
}