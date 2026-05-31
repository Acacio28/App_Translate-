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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.app_translate.ui.components.LanguagePickerDialog
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

    var currentTab by remember { mutableStateOf("translate") }
    var showSourcePicker by remember { mutableStateOf(false) }
    var showTargetPicker by remember { mutableStateOf(false) }
    var showAlternatives by remember { mutableStateOf(false) }

    // Undo/Redo stack sederhana
    var undoStack by remember { mutableStateOf(listOf<String>()) }
    var redoStack by remember { mutableStateOf(listOf<String>()) }

    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val spoken =
            result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (spoken != null) {
            undoStack = undoStack + uiState.inputText
            redoStack = emptyList()
            viewModel.onInputChanged(spoken)
        }
    }

    fun speakText(text: String, langCode: String) {
        if (!ttsReady() || text.isBlank()) return
        val locale = when (langCode) {
            "en" -> Locale.US
            "id" -> Locale("in", "ID")
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
        try { voiceLauncher.launch(intent) }
        catch (e: Exception) { Toast.makeText(context, "Voice input tidak tersedia", Toast.LENGTH_SHORT).show() }
    }

    fun onInputChangedWithHistory(newText: String) {
        undoStack = undoStack + uiState.inputText
        redoStack = emptyList()
        viewModel.onInputChanged(newText)
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            redoStack = redoStack + uiState.inputText
            val prev = undoStack.last()
            undoStack = undoStack.dropLast(1)
            viewModel.onInputChanged(prev)
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            undoStack = undoStack + uiState.inputText
            val next = redoStack.last()
            redoStack = redoStack.dropLast(1)
            viewModel.onInputChanged(next)
        }
    }

    // --- Dialogs ---
    if (showSourcePicker) {
        LanguagePickerDialog(
            title = "Pilih Bahasa Sumber",
            currentLang = uiState.sourceLang,
            onLanguageSelected = { viewModel.onSourceLangChanged(it); showSourcePicker = false },
            onDismiss = { showSourcePicker = false }
        )
    }
    if (showTargetPicker) {
        LanguagePickerDialog(
            title = "Pilih Bahasa Tujuan",
            currentLang = uiState.targetLang,
            onLanguageSelected = { viewModel.onTargetLangChanged(it); showTargetPicker = false },
            onDismiss = { showTargetPicker = false }
        )
    }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = WhiteColor, tonalElevation = 0.dp) {
                NavigationBarItem(
                    selected = currentTab == "translate",
                    onClick = { currentTab = "translate" },
                    icon = { Icon(Icons.Default.Translate, null) },
                    label = { Text("Translate") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PurpleColor,
                        indicatorColor = LightPurpleColor
                    )
                )
                NavigationBarItem(
                    selected = currentTab == "camera",
                    onClick = { currentTab = "camera" },
                    icon = { Icon(Icons.Default.PhotoCamera, null) },
                    label = { Text("Camera") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PurpleColor,
                        indicatorColor = LightPurpleColor
                    )
                )
                NavigationBarItem(
                    selected = currentTab == "history",
                    onClick = { currentTab = "history" },
                    icon = { Icon(Icons.Default.History, null) },
                    label = { Text("History") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PurpleColor,
                        indicatorColor = LightPurpleColor
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentTab) {
                "translate" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // ── INPUT AREA ──────────────────────────────────────
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            // Deteksi bahasa
                            if (uiState.detectedLanguage != null) {
                                TextButton(
                                    onClick = { viewModel.applyDetectedLanguage() },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(
                                        "Terdeteksi: ${uiState.detectedLanguage?.name}. Gunakan?",
                                        color = PurpleColor,
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            // Text field input (minimal, tidak pakai box)
                            TextField(
                                value = uiState.inputText,
                                onValueChange = { onInputChangedWithHistory(it) },
                                placeholder = {
                                    Text(
                                        "Teks",
                                        fontSize = 24.sp,
                                        color = Color(0xFFAAAAAA)
                                    )
                                },
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 24.sp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 150.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                )
                            )
                        }

                        // ── INPUT TOOLBAR ────────────────────────────────────
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Speaker
                            IconButton(onClick = { speakText(uiState.inputText, uiState.sourceLang.code) }) {
                                Icon(Icons.Default.VolumeUp, null, tint = Color(0xFF444444))
                            }
                            // Undo
                            IconButton(
                                onClick = { undo() },
                                enabled = undoStack.isNotEmpty()
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Undo,
                                    null,
                                    tint = if (undoStack.isNotEmpty()) Color(0xFF444444) else Color(0xFFCCCCCC)
                                )
                            }
                            // Redo
                            IconButton(
                                onClick = { redo() },
                                enabled = redoStack.isNotEmpty()
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Redo,
                                    null,
                                    tint = if (redoStack.isNotEmpty()) Color(0xFF444444) else Color(0xFFCCCCCC)
                                )
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            // Folder/File (placeholder)
                            IconButton(onClick = { }) {
                                Icon(Icons.Default.FolderOpen, null, tint = Color(0xFF444444))
                            }
                            // Camera — pindah ke tab kamera
                            IconButton(onClick = { currentTab = "camera" }) {
                                Icon(Icons.Default.PhotoCamera, null, tint = Color(0xFF444444))
                            }
                            // Mic
                            IconButton(onClick = { startVoice() }) {
                                Icon(Icons.Default.Mic, null, tint = Color(0xFF444444))
                            }
                        }

                        HorizontalDivider(color = Color(0xFFE0E0E0))

                        // ── OUTPUT AREA ──────────────────────────────────────
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    color = PurpleColor,
                                    modifier = Modifier
                                        .padding(vertical = 24.dp)
                                        .size(28.dp)
                                )
                            } else {
                                Text(
                                    text = if (uiState.isError) "Terjadi kesalahan..." else uiState.outputText,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = if (uiState.isError) Color.Red else PurpleColor,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = 150.dp)
                                        .padding(vertical = 8.dp)
                                )
                            }
                        }

                        // ── OUTPUT TOOLBAR ────────────────────────────────────
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Speaker output
                            IconButton(onClick = { speakText(uiState.outputText, uiState.targetLang.code) }) {
                                Icon(Icons.Default.VolumeUp, null, tint = Color(0xFF444444))
                            }

                            // Tombol Alternatif (highlight)
                            if (uiState.outputText.isNotBlank() && !uiState.isLoading) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = LightPurpleColor,
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .clickable { showAlternatives = !showAlternatives }
                                ) {
                                    Text(
                                        "Alternatif",
                                        color = PurpleColor,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            // Bookmark
                            IconButton(onClick = { }) {
                                Icon(Icons.Default.BookmarkBorder, null, tint = Color(0xFF444444))
                            }
                            // Share
                            IconButton(onClick = { shareText(uiState.outputText) }) {
                                Icon(Icons.Default.Share, null, tint = Color(0xFF444444))
                            }
                            // Copy
                            IconButton(onClick = { copyText(uiState.outputText) }) {
                                Icon(Icons.Default.ContentCopy, null, tint = Color(0xFF444444))
                            }
                        }

                        // ── PANEL ALTERNATIF ──────────────────────────────────
                        if (showAlternatives && uiState.outputText.isNotBlank()) {
                            HorizontalDivider(color = Color(0xFFE0E0E0))
                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                // Tab Kata / Kalimat
                                Row(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = LightPurpleColor
                                    ) {
                                        Text(
                                            "Kata",
                                            color = PurpleColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = Color.Transparent
                                    ) {
                                        Text(
                                            "Kalimat",
                                            color = Color(0xFF666666),
                                            fontSize = 13.sp,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                // Contoh alternatif statis
                                listOf("Hi...", "Good...", "${uiState.outputText}...").forEach { alt ->
                                    Text(
                                        text = alt,
                                        fontSize = 16.sp,
                                        color = Color(0xFF333333),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.onInputChanged(alt.removeSuffix("...")) }
                                            .padding(horizontal = 16.dp, vertical = 12.dp)
                                    )
                                    HorizontalDivider(color = Color(0xFFF0F0F0))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        HorizontalDivider(color = Color(0xFFE0E0E0))

                        // ── LANGUAGE BAR (BAWAH) ──────────────────────────────
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF333333))
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Bahasa sumber
                            TextButton(
                                onClick = { showSourcePicker = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    uiState.sourceLang.name,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 15.sp
                                )
                            }
                            // Swap
                            IconButton(onClick = { viewModel.onSwapLanguages() }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.CompareArrows,
                                    null,
                                    tint = Color.White
                                )
                            }
                            // Bahasa tujuan
                            TextButton(
                                onClick = { showTargetPicker = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    uiState.targetLang.name,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }

                "camera" -> {
                    CameraScreen(
                        viewModel = viewModel,
                        tts = tts,
                        ttsReady = ttsReady
                    )
                }

                "history" -> {
                    HistoryScreen(viewModel = viewModel)
                }
            }
        }
    }
}