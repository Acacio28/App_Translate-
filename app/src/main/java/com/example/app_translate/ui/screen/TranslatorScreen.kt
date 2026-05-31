package com.example.app_translate.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.app_translate.ui.components.LanguagePickerDialog
import com.example.app_translate.viewmodel.TranslatorViewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.Locale

// ── Warna DeepL ──────────────────────────────────────────────────────────────
private val DeepLBlue   = Color(0xFF1A56DB)
private val DeepLBlueBg = Color(0xFFDEEAFF)
private val DeepLGrayBg = Color(0xFFF0F0F0)
private val DeepLDarkBar   = Color(0xFF4A4A4A)
private val DeepLTextBlack = Color(0xFF1A1A1A)
private val DeepLTextGray  = Color(0xFFAAAAAA)
private val DeepLDivider   = Color(0xFFE5E5E5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslatorScreen(
    tts: TextToSpeech?,
    ttsReady: () -> Boolean,
    viewModel: TranslatorViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var currentTab     by remember { mutableStateOf("translate") }
    var showSourcePicker by remember { mutableStateOf(false) }
    var showTargetPicker by remember { mutableStateOf(false) }
    var showAlternatives by remember { mutableStateOf(false) }
    var alternativeTab   by remember { mutableStateOf("kata") }

    var undoStack by remember { mutableStateOf(listOf<String>()) }
    var redoStack by remember { mutableStateOf(listOf<String>()) }

    // ML Kit text recognizer untuk gallery image
    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    // ── Launchers ─────────────────────────────────────────────────────────────

    // Voice
    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val spoken = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
        if (spoken != null) {
            undoStack = undoStack + uiState.inputText
            redoStack = emptyList()
            viewModel.onInputChanged(spoken)
        }
    }

    // Gallery — ambil gambar lalu OCR
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            val image = InputImage.fromFilePath(context, uri)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val extracted = visionText.text
                    if (extracted.isNotBlank()) {
                        undoStack = undoStack + uiState.inputText
                        redoStack = emptyList()
                        viewModel.onInputChanged(extracted)
                        Toast.makeText(context, "Teks berhasil diekstrak!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Tidak ada teks ditemukan", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Gagal membaca teks dari gambar", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membuka gambar", Toast.LENGTH_SHORT).show()
        }
    }

    // ── Helper functions ──────────────────────────────────────────────────────

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
        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cb.setPrimaryClip(ClipData.newPlainText("text", text))
        Toast.makeText(context, "Disalin!", Toast.LENGTH_SHORT).show()
    }

    fun pasteText() {
        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val pasted = cb.primaryClip?.getItemAt(0)?.text?.toString() ?: return
        undoStack = undoStack + uiState.inputText
        redoStack = emptyList()
        viewModel.onInputChanged(pasted)
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
        catch (e: Exception) {
            Toast.makeText(context, "Voice tidak tersedia", Toast.LENGTH_SHORT).show()
        }
    }

    fun openGallery() {
        galleryLauncher.launch("image/*")
    }

    fun openCamera() {
        currentTab = "camera"
    }

    fun onInputWithHistory(newText: String) {
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

    // ── Dialogs ───────────────────────────────────────────────────────────────
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

    // ── ROOT ──────────────────────────────────────────────────────────────────
    when (currentTab) {
        "camera" -> {
            CameraScreen(
                viewModel = viewModel,
                tts = tts,
                ttsReady = ttsReady,
                onBack = { currentTab = "translate" }
            )
        }
        "history" -> {
            HistoryScreen(viewModel = viewModel)
        }
        else -> {
            // ── TRANSLATE / WRITE TAB ─────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .statusBarsPadding()
            ) {
                // ── TOP BAR ───────────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profil icon
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(DeepLGrayBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, null, tint = Color(0xFF888888), modifier = Modifier.size(20.dp))
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Pill Translator (aktif)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .border(
                                width = 1.5.dp,
                                color = if (currentTab == "translate") DeepLBlue else Color.Transparent,
                                shape = RoundedCornerShape(50)
                            )
                            .background(Color.White)
                            .clickable { currentTab = "translate" }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Translate, null,
                                tint = if (currentTab == "translate") DeepLBlue else Color(0xFF888888),
                                modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Translator",
                                color = if (currentTab == "translate") DeepLBlue else Color(0xFF888888),
                                fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Pill Write
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(DeepLGrayBg)
                            .clickable { currentTab = "write" }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, null,
                                tint = Color(0xFF888888), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Write", color = Color(0xFF888888), fontSize = 14.sp)
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // History icon
                    IconButton(onClick = { currentTab = "history" }) {
                        Icon(Icons.Default.History, null, tint = Color(0xFF888888), modifier = Modifier.size(24.dp))
                    }

                    // Bookmark icon biru
                    Icon(Icons.Default.Bookmark, null, tint = DeepLBlue,
                        modifier = Modifier.size(26.dp))
                }

                // ── KONTEN ────────────────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // ── INPUT AREA ─────────────────────────────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 180.dp)
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        if (uiState.inputText.isEmpty()) {
                            Text(
                                "Ketik, tempel, atau terjemahkan dari sumber di bawah ini",
                                color = DeepLTextGray, fontSize = 16.sp, lineHeight = 24.sp
                            )
                        }
                        BasicTextField(
                            value = uiState.inputText,
                            onValueChange = { onInputWithHistory(it) },
                            textStyle = TextStyle(fontSize = 20.sp, color = DeepLTextBlack, lineHeight = 28.sp),
                            cursorBrush = SolidColor(DeepLBlue),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Tombol Tempelkan — hanya saat input kosong
                    if (uiState.inputText.isEmpty()) {
                        Row(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = DeepLBlueBg,
                                modifier = Modifier.clickable { pasteText() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.ContentPaste, null, tint = DeepLBlue, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Tempelkan", color = DeepLBlue, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Deteksi bahasa hint
                    if (uiState.detectedLanguage != null) {
                        TextButton(
                            onClick = { viewModel.applyDetectedLanguage() },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Text("Terdeteksi: ${uiState.detectedLanguage?.name}. Gunakan?",
                                color = DeepLBlue, fontSize = 13.sp)
                        }
                    }

                    // ── INPUT TOOLBAR ──────────────────────────────────────────
                    if (uiState.inputText.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { speakText(uiState.inputText, uiState.sourceLang.code) }) {
                                Icon(Icons.Default.VolumeUp, null, tint = DeepLTextBlack, modifier = Modifier.size(22.dp))
                            }
                            IconButton(onClick = { undo() }) {
                                Icon(Icons.AutoMirrored.Filled.Undo, null,
                                    tint = if (undoStack.isNotEmpty()) DeepLTextBlack else DeepLTextGray,
                                    modifier = Modifier.size(22.dp))
                            }
                            IconButton(onClick = { redo() }) {
                                Icon(Icons.AutoMirrored.Filled.Redo, null,
                                    tint = if (redoStack.isNotEmpty()) DeepLTextBlack else DeepLTextGray,
                                    modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            // FOLDER → buka gallery + OCR
                            IconButton(onClick = { openGallery() }) {
                                Icon(Icons.Default.FolderOpen, null, tint = DeepLTextBlack, modifier = Modifier.size(22.dp))
                            }
                            // KAMERA → pindah ke tab kamera
                            IconButton(onClick = { openCamera() }) {
                                Icon(Icons.Default.PhotoCamera, null, tint = DeepLTextBlack, modifier = Modifier.size(22.dp))
                            }
                            // MIC
                            IconButton(onClick = { startVoice() }) {
                                Icon(Icons.Default.Mic, null, tint = DeepLTextBlack, modifier = Modifier.size(22.dp))
                            }
                        }
                    } else {
                        // Saat input kosong: undo/redo kecil
                        Row(modifier = Modifier.padding(horizontal = 8.dp)) {
                            IconButton(onClick = { undo() }) {
                                Icon(Icons.AutoMirrored.Filled.Undo, null, tint = DeepLTextGray, modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = { redo() }) {
                                Icon(Icons.AutoMirrored.Filled.Redo, null, tint = DeepLTextGray, modifier = Modifier.size(20.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(60.dp))

                        // 3 icon bulat besar tengah bawah
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 24.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // FOLDER → gallery + OCR
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(DeepLGrayBg)
                                    .clickable { openGallery() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.FolderOpen, null, tint = DeepLTextBlack, modifier = Modifier.size(26.dp))
                            }
                            Spacer(modifier = Modifier.width(20.dp))
                            // KAMERA → tab kamera
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(DeepLGrayBg)
                                    .clickable { openCamera() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.PhotoCamera, null, tint = DeepLTextBlack, modifier = Modifier.size(28.dp))
                            }
                            Spacer(modifier = Modifier.width(20.dp))
                            // MIC
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(DeepLGrayBg)
                                    .clickable { startVoice() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Mic, null, tint = DeepLTextBlack, modifier = Modifier.size(26.dp))
                            }
                        }
                    }

                    // ── DIVIDER + OUTPUT ───────────────────────────────────────
                    if (uiState.inputText.isNotEmpty()) {
                        HorizontalDivider(color = DeepLDivider, thickness = 1.dp)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 160.dp)
                                .padding(horizontal = 20.dp, vertical = 16.dp)
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    color = DeepLBlue, strokeWidth = 2.dp,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Text(
                                    text = if (uiState.isError) "Terjemahan gagal" else uiState.outputText,
                                    fontSize = 20.sp, lineHeight = 28.sp,
                                    color = if (uiState.isError) Color.Red else DeepLBlue
                                )
                            }
                        }

                        // ── OUTPUT TOOLBAR ─────────────────────────────────────
                        if (!uiState.isLoading && uiState.outputText.isNotBlank()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { speakText(uiState.outputText, uiState.targetLang.code) }) {
                                    Icon(Icons.Default.VolumeUp, null, tint = DeepLTextBlack, modifier = Modifier.size(22.dp))
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                // Chip Alternatif
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = DeepLBlueBg,
                                    modifier = Modifier.clickable {
                                        showAlternatives = !showAlternatives
                                        if (showAlternatives) alternativeTab = "kata"
                                    }
                                ) {
                                    Text("Alternatif", color = DeepLBlue, fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                IconButton(onClick = { }) {
                                    Icon(Icons.Default.BookmarkBorder, null, tint = DeepLTextBlack, modifier = Modifier.size(22.dp))
                                }
                                IconButton(onClick = { shareText(uiState.outputText) }) {
                                    Icon(Icons.Default.Share, null, tint = DeepLTextBlack, modifier = Modifier.size(22.dp))
                                }
                                IconButton(onClick = { copyText(uiState.outputText) }) {
                                    Icon(Icons.Default.ContentCopy, null, tint = DeepLTextBlack, modifier = Modifier.size(22.dp))
                                }
                            }

                            // ── PANEL ALTERNATIF ───────────────────────────────
                            if (showAlternatives) {
                                HorizontalDivider(color = DeepLDivider)
                                // Tab Kata | Kalimat | X
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    listOf("kata" to "Kata", "kalimat" to "Kalimat").forEach { (key, label) ->
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (alternativeTab == key) DeepLBlueBg else Color.Transparent,
                                            modifier = Modifier.clickable { alternativeTab = key }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.List, null,
                                                    tint = if (alternativeTab == key) DeepLBlue else Color(0xFF888888),
                                                    modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(label,
                                                    color = if (alternativeTab == key) DeepLBlue else Color(0xFF888888),
                                                    fontSize = 14.sp,
                                                    fontWeight = if (alternativeTab == key) FontWeight.Medium else FontWeight.Normal)
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Spacer(modifier = Modifier.weight(1f))
                                    IconButton(onClick = { showAlternatives = false }) {
                                        Icon(Icons.Default.Close, null, tint = Color(0xFF888888), modifier = Modifier.size(20.dp))
                                    }
                                }

                                // List alternatif
                                val alts = if (alternativeTab == "kata")
                                    listOf("Hello,...", "Hi,...", "Hey,...")
                                else
                                    listOf(
                                        "${uiState.outputText}...",
                                        "Well, ${uiState.outputText.lowercase()}...",
                                        "Actually, ${uiState.outputText.lowercase()}..."
                                    )
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    alts.forEach { alt ->
                                        Text(
                                            text = alt,
                                            fontSize = 16.sp,
                                            color = DeepLTextBlack,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { viewModel.onInputChanged(alt.removeSuffix("...")) }
                                                .padding(horizontal = 20.dp, vertical = 14.dp)
                                        )
                                        HorizontalDivider(color = Color(0xFFF0F0F0),
                                            modifier = Modifier.padding(horizontal = 16.dp))
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }

                // ── LANGUAGE BAR (bawah) ──────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DeepLDarkBar,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showSourcePicker = true }
                    ) {
                        Text(
                            text = uiState.sourceLang.name,
                            color = Color.White,
                            fontSize = 15.sp,
                            modifier = Modifier
                                .padding(vertical = 13.dp)
                                .wrapContentWidth(Alignment.CenterHorizontally)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.onSwapLanguages() },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.CompareArrows, null,
                            tint = Color(0xFF444444), modifier = Modifier.size(24.dp))
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DeepLDarkBar,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showTargetPicker = true }
                    ) {
                        Text(
                            text = uiState.targetLang.name,
                            color = Color.White,
                            fontSize = 15.sp,
                            modifier = Modifier
                                .padding(vertical = 13.dp)
                                .wrapContentWidth(Alignment.CenterHorizontally)
                        )
                    }
                }
            }
        }
    }
}