package com.example.app_translate.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.app_translate.ui.components.InputSection
import com.example.app_translate.ui.components.LanguagePickerDialog
import com.example.app_translate.ui.components.OutputSection
import com.example.app_translate.ui.theme.LightPurpleColor
import com.example.app_translate.ui.theme.PurpleColor
import com.example.app_translate.ui.theme.WhiteColor
import com.example.app_translate.viewmodel.TranslatorViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslatorScreen(
    tts: TextToSpeech?,
    ttsReady: () -> Boolean,
    viewModel: TranslatorViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showSourcePicker by remember { mutableStateOf(false) }
    var showTargetPicker by remember { mutableStateOf(false) }

    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val spoken = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
        if (spoken != null) {
            viewModel.onInputChanged(spoken)
        }
    }

    // --- Helper Functions ---
    fun speakText(text: String, langCode: String) {
        if (!ttsReady() || text.isBlank()) return
        val locale = when (langCode) {
            "en" -> Locale.US
            "id" -> Locale("in", "ID")
            "es" -> Locale("es", "ES")
            "fr" -> Locale.FRANCE
            "ja" -> Locale.JAPAN
            "de" -> Locale.GERMANY
            "ar" -> Locale("ar", "SA")
            "zh" -> Locale.CHINA
            else -> Locale.US
        }
        tts?.language = locale
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    fun copyText(text: String) {
        if (text.isBlank()) return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("text", text))
        Toast.makeText(context, "Teks disalin!", Toast.LENGTH_SHORT).show()
    }

    fun shareText(text: String) {
        if (text.isBlank()) return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Bagikan"))
    }

    fun startVoice() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, uiState.sourceLang.code)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Bicara sekarang...")
        }
        try {
            voiceLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Voice input tidak tersedia", Toast.LENGTH_SHORT).show()
        }
    }

    // --- Dialogs ---
    if (showSourcePicker) {
        LanguagePickerDialog(
            title = "Hili Dalen",
            currentLang = uiState.sourceLang,
            onLanguageSelected = {
                viewModel.onSourceLangChanged(it)
                showSourcePicker = false
            },
            onDismiss = { showSourcePicker = false }
        )
    }

    if (showTargetPicker) {
        LanguagePickerDialog(
            title = "Hili Dalen Tujuan",
            currentLang = uiState.targetLang,
            onLanguageSelected = {
                viewModel.onTargetLangChanged(it)
                showTargetPicker = false
            },
            onDismiss = { showTargetPicker = false }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Translator",
                        fontWeight = FontWeight.Bold,
                        color = PurpleColor
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = WhiteColor
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { startVoice() },
                containerColor = PurpleColor,
                contentColor = WhiteColor,
                shape = CircleShape,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    Icons.Default.Mic,
                    contentDescription = "Voice Input",
                    modifier = Modifier.size(30.dp)
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = WhiteColor,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { showSourcePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            uiState.sourceLang.name,
                            color = PurpleColor,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    IconButton(
                        onClick = { viewModel.onSwapLanguages() },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(LightPurpleColor)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                            contentDescription = "Swap",
                            tint = PurpleColor
                        )
                    }

                    TextButton(
                        onClick = { showTargetPicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            uiState.targetLang.name,
                            color = PurpleColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = WhiteColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    InputSection(
                        inputText = uiState.inputText,
                        onInputChanged = { viewModel.onInputChanged(it) },
                        onSpeak = { speakText(uiState.inputText, uiState.sourceLang.code) },
                        onCopy = { copyText(uiState.inputText) }
                    )
                }
            }
            if (uiState.detectedLanguage != null) {
                TextButton(
                    onClick = { viewModel.applyDetectedLanguage() },
                    modifier = Modifier.align(Alignment.Start).padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = "Terdeteksi: ${uiState.detectedLanguage?.name}. Klik untuk gunakan?",
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        color = PurpleColor
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = WhiteColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutputSection(
                        outputText = uiState.outputText,
                        isLoading = uiState.isLoading,
                        isError = uiState.isError,
                        onSpeak = { speakText(uiState.outputText, uiState.targetLang.code) },
                        onCopy = { copyText(uiState.outputText) },
                        onShare = { shareText(uiState.outputText) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
