package com.example.app_translate

import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.app_translate.ui.screen.TranslatorScreen
import com.example.app_translate.ui.theme.App_TranslateTheme
import com.example.app_translate.viewmodel.TranslatorViewModel
import java.util.Locale

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private lateinit var translatorViewModel: TranslatorViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tts = TextToSpeech(this, this)
        translatorViewModel = TranslatorViewModel(application)
        enableEdgeToEdge()

        setContent {
            App_TranslateTheme {
                TranslatorScreen(
                    tts = tts,
                    ttsReady = { ttsReady },
                    viewModel = translatorViewModel
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