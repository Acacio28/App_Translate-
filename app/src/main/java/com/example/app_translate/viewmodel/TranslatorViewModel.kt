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

    // ─── Fungsi Dialogue Tab ────────────────────────────────────────

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
                .decodeHtmlEntities()

            val userMessage = DialogueMessage(
                originalText = text,
                translatedText = userTranslatedText,
                isUser = true
            )
            _uiState.update { it.copy(dialogueMessages = it.dialogueMessages + userMessage) }

            delay(800)

            // ✅ AI balas sesuai bahasa SOURCE user
            val aiReplyOriginal = generateAiReply(state.sourceLang.code)

            // Terjemahkan balasan AI ke bahasa target (untuk subtitle)
            val aiReplyTranslation = repository.translate(
                aiReplyOriginal,
                state.sourceLang.code,
                state.targetLang.code
            )
            val aiTranslatedText = aiReplyTranslation.getOrElse { "..." }
                .decodeHtmlEntities()

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

    // ✅ AI balas sesuai bahasa user
    private fun generateAiReply(langCode: String): String {
        val replies = when (langCode) {
            "id" -> listOf(
                "Menarik sekali! Ceritakan lebih lanjut.",
                "Saya mengerti. Ada yang bisa saya bantu?",
                "Bagus! Apa pendapat kamu tentang itu?",
                "Oh begitu! Bisa dijelaskan lebih lanjut?",
                "Tentu, saya senang membantu.",
                "Noted! Ada hal lain yang ingin didiskusikan?",
                "Wah, itu cukup menarik!",
                "Terima kasih sudah berbagi!"
            )
            "pt" -> listOf(
                "Muito interessante! Conte-me mais.",
                "Entendo. Como posso ajudar?",
                "Ótimo ponto! O que você acha disso?",
                "Já vejo! Pode explicar melhor?",
                "Claro, fico feliz em ajudar.",
                "Anotado! Há mais alguma coisa?",
                "Realmente? Que fascinante!",
                "Obrigado por compartilhar!"
            )
            "es" -> listOf(
                "¡Muy interesante! Cuéntame más.",
                "Entiendo. ¿En qué puedo ayudarte?",
                "¡Buen punto! ¿Qué piensas al respecto?",
                "¡Ya veo! ¿Puedes explicar más?",
                "Claro, con gusto te ayudo.",
                "¡Anotado! ¿Algo más?",
                "¿De verdad? ¡Fascinante!",
                "¡Gracias por compartir!"
            )
            "fr" -> listOf(
                "Très intéressant ! Dites-m'en plus.",
                "Je comprends. Comment puis-je vous aider ?",
                "Bon point ! Qu'en pensez-vous ?",
                "Je vois ! Pouvez-vous expliquer davantage ?",
                "Bien sûr, je suis heureux de vous aider.",
                "Noté ! Autre chose ?",
                "Vraiment ? C'est fascinant !",
                "Merci de partager !"
            )
            "ja" -> listOf(
                "面白いですね！もっと教えてください。",
                "わかりました。何かお手伝いできますか？",
                "いい点ですね！どう思いますか？",
                "なるほど！もう少し説明できますか？",
                "もちろん、喜んでお手伝いします。",
                "了解です！他に何かありますか？",
                "本当に？それは魅力的ですね！",
                "共有してくれてありがとう！"
            )
            "zh" -> listOf(
                "很有趣！告诉我更多。",
                "我明白了。我能帮你什么？",
                "好观点！你怎么看？",
                "原来如此！能进一步解释吗？",
                "当然，我很乐意帮助。",
                "明白了！还有其他事情吗？",
                "真的吗？真是令人着迷！",
                "谢谢分享！"
            )
            "de" -> listOf(
                "Sehr interessant! Erzähl mir mehr.",
                "Ich verstehe. Wie kann ich helfen?",
                "Guter Punkt! Was denkst du darüber?",
                "Ich sehe! Kannst du es erklären?",
                "Natürlich, ich helfe gerne.",
                "Verstanden! Gibt es noch etwas?",
                "Wirklich? Das ist faszinierend!",
                "Danke fürs Teilen!"
            )
            "ar" -> listOf(
                "مثير للاهتمام! أخبرني المزيد.",
                "أفهم. كيف يمكنني مساعدتك؟",
                "نقطة جيدة! ما رأيك؟",
                "أرى! هل يمكنك الشرح أكثر؟",
                "بالطبع، يسعدني المساعدة.",
                "تم! هل هناك شيء آخر؟",
                "حقاً؟ هذا رائع!",
                "شكراً على المشاركة!"
            )
            "tet" -> listOf(
                "Interesante tebes! Konta tan.",
                "Hau komprende. Hau bele ajuda saida?",
                "Pont di'ak! Ita hanoin saida kona-ba ne'e?",
                "Haree ona! Bele esplika liután?",
                "Loos, hau kontente atu ajuda.",
                "Entendidu! Iha buat seluk tan?",
                "Verdade? Ne'e fascinante tebes!",
                "Obrigadu partilha ho hau!"
            )
            else -> listOf(
                "That's interesting! Tell me more.",
                "I understand. How can I help you?",
                "Great point! What do you think about that?",
                "I see! Can you explain further?",
                "Sure, I'd be happy to help with that.",
                "Noted! Is there anything else?",
                "Really? That's quite fascinating!",
                "Thanks for sharing that with me."
            )
        }
        return replies.random()
    }

    // ✅ Fix HTML entity seperti &#39; → '
    private fun String.decodeHtmlEntities(): String {
        return this
            .replace("&#39;", "'")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&nbsp;", " ")
    }
}