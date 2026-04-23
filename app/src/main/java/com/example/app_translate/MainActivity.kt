package com.example.app_translate // <--- 1. WAJIB ADA PACKAGE

import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.app_translate.ui.theme.App_TranslateTheme
import com.example.app_translate.viewmodel.TranslatorViewModel
import com.example.app_translate.ui.screen.TranslatorScreen // <--- 2. IMPORT SCREEN YANG SUDAH DIBUAT
import java.util.Locale

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var ttsReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inisialisasi TTS (Text to Speech)
        tts = TextToSpeech(this, this)

        enableEdgeToEdge()

        setContent {
            App_TranslateTheme {
                // Menggunakan Factory agar AndroidViewModel (Database) bisa berjalan
                val viewModel: TranslatorViewModel = viewModel(
                    factory = ViewModelProvider.AndroidViewModelFactory.getInstance(application)
                )

                // Memanggil TranslatorScreen dari package ui.screen
                TranslatorScreen(
                    tts = tts,
                    ttsReady = { ttsReady },
                    viewModel = viewModel
                )
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            ttsReady = true
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}