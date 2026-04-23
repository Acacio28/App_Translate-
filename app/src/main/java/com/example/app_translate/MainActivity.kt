package com.example.app_translate

import TranslatorScreen
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.app_translate.ui.theme.App_TranslateTheme
import java.util.Locale


class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var ttsReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inisialisasi TTS sebelum setContent
        tts = TextToSpeech(this, this)

        enableEdgeToEdge()

        setContent {
            App_TranslateTheme {
                TranslatorScreen(
                    tts = tts,
                    ttsReady = { ttsReady }
                )
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Set bahasa default agar tidak error saat pertama kali digunakan
            tts?.language = Locale.US
            ttsReady = true
        }
    }

    override fun onDestroy() {
        // Penting untuk mencegah memory leak yang bisa bikin crash saat app ditutup
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}