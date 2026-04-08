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
    val isError: Boolean = false
)

class TranslatorViewModel(
    private val repository: TranslateRepository = TranslateRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(TranslatorUiState())
    val uiState: StateFlow<TranslatorUiState> = _uiState.asStateFlow()

    private var translateJob: Job? = null

    // Inisialisasi Google ML Kit Language ID
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

        if (state.inputText.isBlank()) {
            _uiState.update { it.copy(outputText = "", isLoading = false, isError = false) }
            return
        }

        translateJob?.cancel()
        translateJob = viewModelScope.launch {
            delay(600) // Tunggu user selesai mengetik

            _uiState.update { it.copy(isLoading = true, isError = false) }

            // Deteksi bahasa teks yang diinput
            languageIdentifier.identifyLanguage(state.inputText)
                .addOnSuccessListener { detectedLanguageCode ->
                    val selectedSourceCode = state.sourceLang.code

                    // LOGIKA UTAMA:
                    // Jika bahasa yang diketik (detected) BUKAN bahasa asal yang dipilih (source)
                    // Dan bahasa tersebut terdeteksi (bukan "und")
                    if (detectedLanguageCode != "und" && detectedLanguageCode != selectedSourceCode) {
                        _uiState.update {
                            it.copy(
                                outputText = state.inputText, // Langsung copy teks asli ke output
                                isLoading = false,
                                isError = false
                            )
                        }
                    } else {
                        // Jika bahasa sesuai, lakukan proses terjemahan normal
                        performTranslation(state)
                    }
                }
                .addOnFailureListener {
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