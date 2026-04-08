package com.example.app_translate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_translate.data.model.Language
import com.example.app_translate.data.model.languages
import com.example.app_translate.data.repository.TranslateRepository
import com.google.mlkit.nl.languageid.LanguageIdentification // Import baru
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
    val isError: Boolean = false
)

class TranslatorViewModel(
    private val repository: TranslateRepository = TranslateRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(TranslatorUiState())
    val uiState: StateFlow<TranslatorUiState> = _uiState.asStateFlow()

    private var translateJob: Job? = null

    // Inisialisasi pendeteksi bahasa dari Google ML Kit
    private val languageIdentifier = LanguageIdentification.getClient()

    fun onInputChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
        triggerTranslate()
    }

    fun onSourceLangChanged(lang: Language) {
        _uiState.update { it.copy(sourceLang = lang) }
        triggerTranslate()
    }

    fun onTargetLangChanged(lang: Language) {
        _uiState.update { it.copy(targetLang = lang) }
        triggerTranslate()
    }

    fun onSwapLanguages() {
        _uiState.update { state ->
            state.copy(
                sourceLang = state.targetLang,
                targetLang = state.sourceLang
            )
        }
        triggerTranslate()
    }

    private fun triggerTranslate() {
        val state = _uiState.value

        // Jika input kosong, bersihkan output
        if (state.inputText.isBlank()) {
            _uiState.update { it.copy(outputText = "", isLoading = false, isError = false) }
            return
        }

        translateJob?.cancel()
        translateJob = viewModelScope.launch {
            delay(600) // Tunggu user selesai mengetik sebentar

            _uiState.update { it.copy(isLoading = true, isError = false) }

            // LOGIKA BARU: Deteksi bahasa sebelum kirim ke Repository
            languageIdentifier.identifyLanguage(state.inputText)
                .addOnSuccessListener { detectedLanguageCode ->
                    val selectedSourceCode = state.sourceLang.code

                    // Jika bahasa terdeteksi (bukan "und") dan TIDAK COCOK dengan pilihan user
                    if (detectedLanguageCode != "und" && detectedLanguageCode != selectedSourceCode) {
                        _uiState.update {
                            it.copy(
                                outputText = "Bahasa tidak sesuai! Anda memilih '${state.sourceLang.name}', tapi teks terdeteksi sebagai '$detectedLanguageCode'.",
                                isLoading = false,
                                isError = true
                            )
                        }
                    } else {
                        // Jika bahasa COCOK, lanjutkan terjemahan
                        performTranslation(state)
                    }
                }
                .addOnFailureListener {
                    // Jika deteksi gagal, tetap coba terjemahkan saja
                    performTranslation(state)
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
                            outputText = "Gagal menerjemahkan. Cek koneksi internet.",
                            isLoading = false,
                            isError = true
                        )
                    }
                }
            )
        }
    }
}