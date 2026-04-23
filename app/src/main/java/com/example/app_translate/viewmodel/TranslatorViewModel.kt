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
        // Reset detectedLanguage saat user mengganti bahasa secara manual
        _uiState.update { it.copy(sourceLang = lang, detectedLanguage = null) }
        triggerTranslate()
    }

    fun onTargetLangChanged(lang: Language) {
        _uiState.update { it.copy(targetLang = lang) }
        triggerTranslate()
    }

    // 2. TAMBAHKAN INI: Fungsi untuk menerapkan saran bahasa
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

            // 3. UPDATE LOGIKA DETEKSI DI SINI
            languageIdentifier.identifyLanguage(state.inputText)
                .addOnSuccessListener { detectedLanguageCode ->
                    val currentSourceCode = _uiState.value.sourceLang.code

                    // Cari apakah kode bahasa yang dideteksi ada di daftar aplikasi kita
                    val detectedLang = languages.find { it.code == detectedLanguageCode }

                    if (detectedLang != null && detectedLanguageCode != "und" && detectedLanguageCode != currentSourceCode) {
                        // Jika terdeteksi bahasa lain, munculkan saran (suggestion)
                        _uiState.update { it.copy(detectedLanguage = detectedLang) }
                    } else {
                        // Jika bahasa cocok atau tidak dikenal, hapus saran
                        _uiState.update { it.copy(detectedLanguage = null) }
                    }

                    // Tetap lakukan translasi dengan bahasa yang sedang terpilih
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
}