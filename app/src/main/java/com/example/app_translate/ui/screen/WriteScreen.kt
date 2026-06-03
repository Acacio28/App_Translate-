package com.example.app_translate.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import com.example.app_translate.BuildConfig
import com.example.app_translate.data.model.Language
import com.example.app_translate.data.model.languages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// ── Warna ─────────────────────────────────────────────────────────────────────
private val WriteBlue      = Color(0xFF1A56DB)
private val WriteBlueBg    = Color(0xFFDEEAFF)
private val WriteGrayBg    = Color(0xFFF0F0F0)
private val WriteDarkBar   = Color(0xFF4A4A4A)
private val WriteTextBlack = Color(0xFF1A1A1A)
private val WriteTextGray  = Color(0xFFAAAAAA)
private val WriteDivider   = Color(0xFFE5E5E5)
private val WritePurple    = Color(0xFF7B61FF)
private val WritePurpleBg  = Color(0xFFF0EEFF)

// ── Data ──────────────────────────────────────────────────────────────────────
data class CorrectionResult(
    val corrected: String,
    val changes: List<Pair<String, String>>
)

val gayaOptions = listOf("Tidak ada", "Sederhana", "Bisnis", "Akademis", "Informal", "Kreatif")
val nadaOptions = listOf("Tidak ada", "Formal", "Ramah", "Tegas", "Persuasif", "Netral")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WriteScreen(
    selectedLang: Language = languages.first { it.code == "en" },
    onLangClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    var inputText  by remember { mutableStateOf("") }
    var resultText by remember { mutableStateOf("") }
    var isLoading  by remember { mutableStateOf(false) }
    var isError    by remember { mutableStateOf(false) }
    var showResult by remember { mutableStateOf(false) }

    // Gaya & Nada
    var selectedGaya       by remember { mutableStateOf("Tidak ada") }
    var selectedNada       by remember { mutableStateOf("Tidak ada") }
    var showGayaNadaPanel  by remember { mutableStateOf(false) }
    var activeTab          by remember { mutableStateOf("gaya") } // "gaya" atau "nada"

    // Undo/Redo
    var undoStack by remember { mutableStateOf(listOf<String>()) }
    var redoStack by remember { mutableStateOf(listOf<String>()) }

    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val spoken = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
        if (spoken != null) {
            undoStack = undoStack + inputText
            redoStack = emptyList()
            inputText = spoken
        }
    }

    fun startVoice() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, selectedLang.code)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Bicara sekarang...")
        }
        try { voiceLauncher.launch(intent) }
        catch (e: Exception) {
            Toast.makeText(context, "Voice tidak tersedia", Toast.LENGTH_SHORT).show()
        }
    }

    fun copyText(text: String) {
        if (text.isBlank()) return
        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cb.setPrimaryClip(ClipData.newPlainText("text", text))
        Toast.makeText(context, "Disalin!", Toast.LENGTH_SHORT).show()
    }

    fun shareText(text: String) {
        if (text.isBlank()) return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Bagikan"))
    }

    fun correctText() {
        if (inputText.isBlank()) return
        isLoading  = true
        isError    = false
        showResult = false

        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val url  = URL("https://api.anthropic.com/v1/messages")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.setRequestProperty("x-api-key", BuildConfig.ANTHROPIC_API_KEY)
                    conn.setRequestProperty("anthropic-version", "2023-06-01")
                    conn.doOutput = true

                    val langName  = selectedLang.name
                    val gayaInfo  = if (selectedGaya != "Tidak ada") "Writing style: $selectedGaya." else ""
                    val nadaInfo  = if (selectedNada != "Tidak ada") "Tone: $selectedNada." else ""

                    val prompt = """You are a writing assistant. The user wrote text in $langName.
$gayaInfo $nadaInfo
Fix grammar, spelling, punctuation, and style errors.
If a writing style or tone is specified, rewrite accordingly.
Return ONLY a JSON object with two fields:
- "corrected": the fully corrected/rewritten text
- "changes": array of objects with "original" and "fixed" fields (max 5 changes)

Example:
{"corrected":"Hello, how are you?","changes":[{"original":"helo","fixed":"Hello"}]}

User text: "${inputText.replace("\"", "\\\"")}""""

                    val body = JSONObject().apply {
                        put("model", "claude-sonnet-4-20250514")
                        put("max_tokens", 1000)
                        put("messages", JSONArray().apply {
                            put(JSONObject().apply {
                                put("role", "user")
                                put("content", prompt)
                            })
                        })
                    }.toString()

                    conn.outputStream.bufferedWriter().use { it.write(body) }

                    val response = conn.inputStream.bufferedReader().readText()
                    conn.disconnect()

                    val json    = JSONObject(response)
                    val content = json.getJSONArray("content")
                        .getJSONObject(0).getString("text").trim()

                    val resultJson = JSONObject(content)
                    val corrected  = resultJson.getString("corrected")
                    val changesArr = resultJson.getJSONArray("changes")
                    val changes    = mutableListOf<Pair<String, String>>()
                    for (i in 0 until changesArr.length()) {
                        val ch = changesArr.getJSONObject(i)
                        changes.add(ch.getString("original") to ch.getString("fixed"))
                    }
                    CorrectionResult(corrected, changes)
                }

                resultText = result.corrected
                isLoading  = false
                showResult = true

            } catch (e: Exception) {
                isError    = true
                isLoading  = false
                resultText = "Gagal memperbaiki teks. Coba lagi."
                showResult = true
            }
        }
    }

    // ── UI ────────────────────────────────────────────────────────────────────
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            // ── INPUT AREA ─────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = if (showResult) 140.dp else 240.dp)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                if (inputText.isEmpty()) {
                    Text(
                        "Masukkan teks yang ingin Anda perbaiki atau tulis ulang di sini",
                        color = WriteTextGray, fontSize = 16.sp, lineHeight = 24.sp
                    )
                }
                BasicTextField(
                    value = inputText,
                    onValueChange = {
                        undoStack = undoStack + inputText
                        redoStack = emptyList()
                        inputText = it
                        if (showResult) { showResult = false; resultText = "" }
                    },
                    textStyle = TextStyle(
                        fontSize = 18.sp, color = WriteTextBlack, lineHeight = 27.sp
                    ),
                    cursorBrush = SolidColor(WritePurple),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ── INPUT TOOLBAR ──────────────────────────────────────────────────
            if (inputText.isNotEmpty() && !showResult) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        if (undoStack.isNotEmpty()) {
                            redoStack = redoStack + inputText
                            inputText = undoStack.last()
                            undoStack = undoStack.dropLast(1)
                        }
                    }) {
                        Icon(Icons.Default.Undo, null,
                            tint = if (undoStack.isNotEmpty()) WriteTextBlack else WriteTextGray,
                            modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = {
                        if (redoStack.isNotEmpty()) {
                            undoStack = undoStack + inputText
                            inputText = redoStack.last()
                            redoStack = redoStack.dropLast(1)
                        }
                    }) {
                        Icon(Icons.Default.Redo, null,
                            tint = if (redoStack.isNotEmpty()) WriteTextBlack else WriteTextGray,
                            modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = { correctText() },
                        colors = ButtonDefaults.buttonColors(containerColor = WritePurple),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Default.AutoFixHigh, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Perbaiki", fontSize = 13.sp)
                    }
                }
            }

            // ── LOADING ────────────────────────────────────────────────────────
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = WritePurple, strokeWidth = 2.dp,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // ── HASIL KOREKSI ──────────────────────────────────────────────────
            if (showResult && resultText.isNotBlank()) {
                HorizontalDivider(color = WriteDivider)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AutoFixHigh, null, tint = WritePurple, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Hasil Perbaikan", color = WritePurple, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    if (selectedGaya != "Tidak ada" || selectedNada != "Tidak ada") {
                        Spacer(modifier = Modifier.width(8.dp))
                        if (selectedGaya != "Tidak ada") {
                            Surface(shape = RoundedCornerShape(6.dp), color = WritePurpleBg) {
                                Text(selectedGaya, color = WritePurple, fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        if (selectedNada != "Tidak ada") {
                            Surface(shape = RoundedCornerShape(6.dp), color = WriteBlueBg) {
                                Text(selectedNada, color = WriteBlue, fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                            }
                        }
                    }
                }

                Text(
                    text = resultText,
                    fontSize = 18.sp,
                    color = if (isError) Color.Red else WriteTextBlack,
                    lineHeight = 27.sp,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)
                )

                if (!isError) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = WritePurpleBg,
                            modifier = Modifier.clickable {
                                undoStack = undoStack + inputText
                                inputText = resultText
                                showResult = false
                                resultText = ""
                            }
                        ) {
                            Text("Gunakan teks ini", color = WritePurple, fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = { shareText(resultText) }) {
                            Icon(Icons.Default.Share, null, tint = WriteTextBlack, modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = { copyText(resultText) }) {
                            Icon(Icons.Default.ContentCopy, null, tint = WriteTextBlack, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            // ── MIC BULAT (saat kosong) ────────────────────────────────────────
            if (inputText.isEmpty() && !isLoading) {
                Spacer(modifier = Modifier.height(80.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(WriteGrayBg)
                            .clickable { startVoice() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Mic, null, tint = WriteTextBlack, modifier = Modifier.size(28.dp))
                    }
                }
            }

            // ── PANEL GAYA & NADA ──────────────────────────────────────────────
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = WriteDivider)

            // Header panel — klik untuk expand/collapse
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showGayaNadaPanel = !showGayaNadaPanel }
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (showGayaNadaPanel) Icons.Default.KeyboardArrowUp
                    else Icons.Default.KeyboardArrowDown,
                    null, tint = WriteTextBlack, modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Gaya dan nada penulisan",
                    fontSize = 15.sp, color = WriteTextBlack, fontWeight = FontWeight.Medium)

                // Badge gaya/nada yang dipilih
                Spacer(modifier = Modifier.weight(1f))
                if (selectedGaya != "Tidak ada") {
                    Surface(shape = RoundedCornerShape(6.dp), color = WritePurpleBg) {
                        Text(selectedGaya, color = WritePurple, fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                if (selectedNada != "Tidak ada") {
                    Surface(shape = RoundedCornerShape(6.dp), color = WriteBlueBg) {
                        Text(selectedNada, color = WriteBlue, fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                    }
                }
            }

            // Konten panel
            if (showGayaNadaPanel) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // ── TAB GAYA / NADA ────────────────────────────────────────
                    Row(modifier = Modifier.fillMaxWidth()) {
                        // Tab Gaya
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (activeTab == "gaya") WritePurpleBg else WriteGrayBg,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { activeTab = "gaya" }
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = activeTab == "gaya",
                                        onClick = { activeTab = "gaya" },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = WritePurple
                                        )
                                    )
                                    Text("Gaya", fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (activeTab == "gaya") WritePurple else WriteTextBlack)
                                }
                                Text(selectedGaya, fontSize = 12.sp,
                                    color = if (activeTab == "gaya") WritePurple else WriteTextGray,
                                    modifier = Modifier.padding(start = 4.dp))
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Tab Nada
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (activeTab == "nada") WriteBlueBg else WriteGrayBg,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { activeTab = "nada" }
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = activeTab == "nada",
                                        onClick = { activeTab = "nada" },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = WriteBlue
                                        )
                                    )
                                    Text("Nada", fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (activeTab == "nada") WriteBlue else WriteTextBlack)
                                }
                                Text(selectedNada, fontSize = 12.sp,
                                    color = if (activeTab == "nada") WriteBlue else WriteTextGray,
                                    modifier = Modifier.padding(start = 4.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Deskripsi
                    Text(
                        text = if (activeTab == "gaya")
                            "Pilih gaya untuk menulis ulang teks Anda dengan cara yang cocok untuk audiens dan tujuan Anda."
                        else
                            "Pilih nada untuk menyesuaikan nuansa emosional teks Anda.",
                        fontSize = 13.sp, color = WriteTextGray,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // List pilihan Gaya atau Nada
                    val options = if (activeTab == "gaya") gayaOptions else nadaOptions
                    val selected = if (activeTab == "gaya") selectedGaya else selectedNada

                    options.forEach { option ->
                        val isSelected = option == selected
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (activeTab == "gaya") selectedGaya = option
                                    else selectedNada = option
                                }
                                .background(
                                    if (isSelected) if (activeTab == "gaya") WritePurpleBg else WriteBlueBg
                                    else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = option,
                                fontSize = 15.sp,
                                color = if (isSelected)
                                    if (activeTab == "gaya") WritePurple else WriteBlue
                                else WriteTextBlack,
                                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check, null,
                                    tint = if (activeTab == "gaya") WritePurple else WriteBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        HorizontalDivider(color = Color(0xFFF5F5F5))
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        // ── LANGUAGE BAR BAWAH ─────────────────────────────────────────────────
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
                color = WriteDarkBar,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLangClick() }
            ) {
                Text(
                    text = selectedLang.name,
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