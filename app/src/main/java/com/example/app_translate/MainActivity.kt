package com.example.app_translate

import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModelProvider
import com.example.app_translate.ui.screen.SplashScreen
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
            var showSplash by remember { mutableStateOf(true) }

            App_TranslateTheme {
                if (showSplash) {
                    SplashScreen(onFinished = { showSplash = false })
                } else {
                    AnimatedContent(
                        targetState = Unit,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500))
                        },
                        label = "splashTransition"
                    ) {
                        TranslatorScreen(
                            tts = tts,
                            ttsReady = { ttsReady },
                            viewModel = translatorViewModel
                        )
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