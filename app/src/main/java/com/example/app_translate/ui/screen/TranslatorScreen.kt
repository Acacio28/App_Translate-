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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.app_translate.ui.components.InputSection
import com.example.app_translate.ui.components.LanguagePickerDialog
import com.example.app_translate.ui.components.OutputSection
import com.example.app_translate.ui.theme.*
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

    // Fungsi bantuan (Speak, Copy, Share, Voice)
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
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, uiState.sourceLang.code)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Bicara sekarang...")
        }
        try {
            voiceLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Voice input tidak tersedia", Toast.LENGTH_SHORT).show()
        }
    }

    // Dialogs
    if (showSourcePicker) {
        LanguagePickerDialog(
            title = "Pilih Bahasa Sumber",
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
            title = "Pilih Bahasa Tujuan",
            currentLang = uiState.targetLang,
            onLanguageSelected = {
                viewModel.onTargetLangChanged(it)
                showTargetPicker = false
            },
            onDismiss = { showTargetPicker = false }
        )
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = WhiteColor,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = true,
                    onClick = { /* Navigasi */ },
                    icon = { Icon(Icons.Default.Translate, contentDescription = null) },
                    label = { Text("Translate") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PurpleColor,
                        selectedTextColor = PurpleColor,
                        indicatorColor = LightPurpleColor
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { /* Navigasi */ },
                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                    label = { Text("History") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { /* Navigasi */ },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Settings") }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(WhiteColor)
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Translator App",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = PurpleColor,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { showSourcePicker = true },
                    colors = ButtonDefaults.buttonColors(containerColor = LightPurpleColor),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(text = uiState.sourceLang.name, color = PurpleColor)
                }

                // Tombol Swap menggunakan Icon yang benar
                IconButton(
                    onClick = { viewModel.onSwapLanguages() },
                    modifier = Modifier
                        .size(48.dp)
                        .background(DarkPurpleColor, RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                        contentDescription = "Swap",
                        tint = WhiteColor
                    )
                }

                Button(
                    onClick = { showTargetPicker = true },
                    colors = ButtonDefaults.buttonColors(containerColor = LightPurpleColor),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(text = uiState.targetLang.name, color = PurpleColor)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            InputSection(
                inputText = uiState.inputText,
                onInputChanged = { viewModel.onInputChanged(it) },
                onSpeak = { speakText(uiState.inputText, uiState.sourceLang.code) },
                onCopy = { copyText(uiState.inputText) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutputSection(
                outputText = uiState.outputText,
                isLoading = uiState.isLoading,
                isError = uiState.isError,
                onSpeak = { speakText(uiState.outputText, uiState.targetLang.code) },
                onCopy = { copyText(uiState.outputText) },
                onShare = { shareText(uiState.outputText) }
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { startVoice() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PurpleColor),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(text = "🎤 Voice Input", fontSize = 16.sp, color = WhiteColor)
            }
        }
    }
}