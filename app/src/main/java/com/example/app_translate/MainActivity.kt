package com.example.app_translate

import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.app_translate.ui.components.BottomNavigationBar
import com.example.app_translate.ui.screen.DialogueScreen
import com.example.app_translate.ui.screen.TranslatorScreen
import com.example.app_translate.ui.theme.App_TranslateTheme
import java.util.Locale

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var ttsReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inisialisasi TTS
        tts = TextToSpeech(this, this)

        enableEdgeToEdge()

        setContent {
            App_TranslateTheme {
                // Estado hodi jere navegasaun entre screens
                var currentRoute by remember { mutableStateOf("translate") }

                Scaffold(
                    bottomBar = {
                        BottomNavigationBar(
                            currentRoute = currentRoute,
                            onItemSelected = { route ->
                                currentRoute = route
                            }
                        )
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (currentRoute) {
                            "translate" -> {
                                TranslatorScreen(
                                    tts = tts,
                                    ttsReady = { ttsReady }
                                )
                            }
                            "dialogue" -> {
                                // HADIA IHA NE'E: Pasa parameter tts ba DialogueScreen
                                DialogueScreen(
                                    tts = tts,
                                    ttsReady = { ttsReady }
                                )
                            }
                            // Ita bele aumenta "history" ka "settings" iha futuru
                            else -> {
                                TranslatorScreen(
                                    tts = tts,
                                    ttsReady = { ttsReady }
                                )
                            }
                        }
                    }
                }
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
