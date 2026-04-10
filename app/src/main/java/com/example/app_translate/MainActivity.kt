package com.example.app_translate

import TranslatorScreen
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier

// IMPORT PENTING: Pastikan baris ini ada agar TranslatorScreen dikenali
class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var ttsReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Mengaktifkan tampilan layar penuh
        enableEdgeToEdge()

        // 2. Inisialisasi Text To Speech
        tts = TextToSpeech(this, this)

        setContent {
            // 3. Menggunakan MaterialTheme standar agar tidak error Theme
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Memanggil layar utama aplikasi
                    TranslatorScreen(
                        tts = tts,
                        ttsReady = { ttsReady }
                    )
                }
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            ttsReady = true
        }
    }

    override fun onDestroy() {
        // Bersihkan resource agar tidak memory leak
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}