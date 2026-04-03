package com.example.app_translate

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
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
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

// Data class untuk bahasa
data class Language(val name: String, val code: String)

// Daftar bahasa
val languages = listOf(
    Language("English", "en"),
    Language("Indonesian", "id"),
    Language("Spanish", "es"),
    Language("French", "fr"),
    Language("Japanese", "ja"),
    Language("German", "de"),
    Language("Arabic", "ar"),
    Language("Chinese", "zh")
)

// Warna
val PurpleColor = Color(0xFF5D3FD3)
val LightPurpleColor = Color(0xFFF7EDFF)
val DarkPurpleColor = Color(0xFFE7D6FF)
val GrayColor = Color(0xFF64547E)
val LightGrayColor = Color(0xFFB7A5D4)
val WhiteColor = Color(0xFFFFFFFF)
val RedColor = Color(0xFFB41340)

// Fungsi terjemahan
suspend fun translateText(text: String, from: String, to: String): String {
    return withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(text, "UTF-8")
        val url = "https://api.mymemory.translated.net/get?q=$encoded&langpair=$from|$to"
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 10000
        conn.readTimeout = 10000
        try {
            val response = conn.inputStream.bufferedReader().readText()
            val json = JSONObject(response)
            json.getJSONObject("responseData").getString("translatedText")
        } finally {
            conn.disconnect()
        }
    }
}

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var ttsReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        tts = TextToSpeech(this, this)

        setContent {
            TranslatorApp(
                tts = tts,
                ttsReady = { ttsReady }
            )
        }
    }

    override fun onInit(status: Int) {
        ttsReady = (status == TextToSpeech.SUCCESS)
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

@Composable
fun TranslatorApp(tts: TextToSpeech?, ttsReady: () -> Boolean) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var sourceLang by remember { mutableStateOf(languages[0]) }
    var targetLang by remember { mutableStateOf(languages[1]) }
    var inputText by remember { mutableStateOf("") }
    var outputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }
    var showSourcePicker by remember { mutableStateOf(false) }
    var showTargetPicker by remember { mutableStateOf(false) }
    var translateJob by remember { mutableStateOf<Job?>(null) }

    fun doTranslate() {
        if (inputText.isBlank()) {
            outputText = ""
            return
        }
        translateJob?.cancel()
        translateJob = scope.launch {
            delay(500)
            isLoading = true
            isError = false
            try {
                outputText = translateText(inputText, sourceLang.code, targetLang.code)
            } catch (e: Exception) {
                isError = true
                outputText = "Gagal menerjemahkan. Cek koneksi internet."
            } finally {
                isLoading = false
            }
        }
    }

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

    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (spoken != null) {
            inputText = spoken
            doTranslate()
        }
    }

    fun startVoice() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, sourceLang.code)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Bicara sekarang...")
        }
        try {
            voiceLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Voice input tidak tersedia", Toast.LENGTH_SHORT).show()
        }
    }

    // Dialog source language
    if (showSourcePicker) {
        AlertDialog(
            onDismissRequest = { showSourcePicker = false },
            title = { Text("Pilih Bahasa Sumber") },
            text = {
                Column {
                    languages.forEach { lang ->
                        TextButton(
                            onClick = {
                                sourceLang = lang
                                showSourcePicker = false
                                if (inputText.isNotEmpty()) doTranslate()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                lang.name,
                                color = if (lang.code == sourceLang.code) PurpleColor else Color.Black
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSourcePicker = false }) {
                    Text("Batal", color = PurpleColor)
                }
            }
        )
    }

    // Dialog target language
    if (showTargetPicker) {
        AlertDialog(
            onDismissRequest = { showTargetPicker = false },
            title = { Text("Pilih Bahasa Tujuan") },
            text = {
                Column {
                    languages.forEach { lang ->
                        TextButton(
                            onClick = {
                                targetLang = lang
                                showTargetPicker = false
                                if (inputText.isNotEmpty()) doTranslate()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                lang.name,
                                color = if (lang.code == targetLang.code) PurpleColor else Color.Black
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showTargetPicker = false }) {
                    Text("Batal", color = PurpleColor)
                }
            }
        )
    }

    // UI Utama
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WhiteColor)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Title
        Text(
            text = "Translator App",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = PurpleColor,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Language selection row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Source language button
            Button(
                onClick = { showSourcePicker = true },
                colors = ButtonDefaults.buttonColors(containerColor = LightPurpleColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = sourceLang.name, color = PurpleColor)
            }

            // Swap button
            Button(
                onClick = {
                    val temp = sourceLang
                    sourceLang = targetLang
                    targetLang = temp
                    if (inputText.isNotEmpty()) doTranslate()
                },
                colors = ButtonDefaults.buttonColors(containerColor = DarkPurpleColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "⇄", fontSize = 20.sp, color = PurpleColor)
            }

            // Target language button
            Button(
                onClick = { showTargetPicker = true },
                colors = ButtonDefaults.buttonColors(containerColor = LightPurpleColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = targetLang.name, color = PurpleColor)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Input section
        Text(
            text = "Input",
            fontSize = 12.sp,
            color = GrayColor,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(LightPurpleColor)
                .padding(16.dp)
        ) {
            BasicTextField(
                value = inputText,
                onValueChange = {
                    inputText = it
                    doTranslate()
                },
                textStyle = TextStyle(fontSize = 18.sp, color = Color.Black),
                cursorBrush = SolidColor(PurpleColor),
                decorationBox = { inner ->
                    if (inputText.isEmpty()) {
                        Text(
                            text = "Ketik teks untuk diterjemahkan...",
                            color = LightGrayColor
                        )
                    }
                    inner()
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Input action buttons
        if (inputText.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { speakText(inputText, sourceLang.code) }) {
                    Text(text = "🔊 Baca", color = PurpleColor)
                }
                TextButton(onClick = { copyText(inputText) }) {
                    Text(text = "📋 Salin", color = PurpleColor)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Output section header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Output",
                fontSize = 12.sp,
                color = GrayColor
            )
            if (outputText.isNotEmpty() && !isLoading && !isError) {
                TextButton(onClick = { shareText(outputText) }) {
                    Text(text = "📤 Bagikan", color = PurpleColor)
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Output box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DarkPurpleColor)
                .padding(16.dp)
        ) {
            when {
                isLoading -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = PurpleColor,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Menerjemahkan...", color = GrayColor)
                    }
                }
                isError -> Text(text = outputText, color = RedColor)
                outputText.isNotEmpty() -> Text(
                    text = outputText,
                    fontSize = 18.sp,
                    color = PurpleColor
                )
                else -> Text(
                    text = "Hasil terjemahan akan muncul di sini...",
                    color = LightGrayColor
                )
            }
        }

        // Output action buttons
        if (outputText.isNotEmpty() && !isLoading && !isError) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { speakText(outputText, targetLang.code) }) {
                    Text(text = "🔊 Baca", color = PurpleColor)
                }
                TextButton(onClick = { copyText(outputText) }) {
                    Text(text = "📋 Salin", color = PurpleColor)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Voice input button
        Button(
            onClick = { startVoice() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = PurpleColor),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(text = "🎤 Voice Input", fontSize = 16.sp, color = WhiteColor)
        }
    }
}