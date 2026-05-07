package com.example.app_translate.ui.screen

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.app_translate.ui.theme.*
import com.example.app_translate.viewmodel.DialogueMessage
import com.example.app_translate.viewmodel.TranslatorViewModel
import java.util.Locale

@Composable
fun DialogueScreen(
    tts: TextToSpeech?,
    ttsReady: () -> Boolean,
    viewModel: TranslatorViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    // Funsaun hodi AI ko'alia sai (TTS)
    val speakAI = { text: String, langCode: String ->
        if (ttsReady() && text.isNotBlank()) {
            tts?.language = Locale(langCode)
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    // Launcher hodi rona user ko'alia
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0)
            spokenText?.let { viewModel.addDialogueMessage(it, isFromMe = true) }
        }
    }

    // AI Auto-play lian tradusaun nian bainhira iha mensajen foun
    LaunchedEffect(uiState.dialogueMessages.size) {
        if (uiState.dialogueMessages.isNotEmpty()) {
            val lastMsg = uiState.dialogueMessages.last()
            speakAI(lastMsg.translatedText, lastMsg.targetLangCode)
            listState.animateScrollToItem(uiState.dialogueMessages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFBFBFE))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "AI Conversation",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = PurpleColor,
            modifier = Modifier.padding(top = 16.dp)
        )

        // Hatudu lian ne'ebé hili husi TranslatorScreen
        Text(
            text = "${uiState.sourceLang.name} ➔ ${uiState.targetLang.name}",
            style = MaterialTheme.typography.bodySmall,
            color = GrayColor
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Chat List
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(uiState.dialogueMessages) { message ->
                DialogueItemAI(message, speakAI)
            }
        }

        // Microphone Button
        FloatingActionButton(
            onClick = {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, uiState.sourceLang.code)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Ko'alia agora...")
                }
                speechLauncher.launch(intent)
            },
            containerColor = PurpleColor,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier.size(72.dp)
        ) {
            Icon(Icons.Default.Mic, contentDescription = "User Speak", modifier = Modifier.size(36.dp))
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        Text("Speak in ${uiState.sourceLang.name}", fontSize = 12.sp, color = GrayColor)
    }
}

@Composable
fun DialogueItemAI(message: DialogueMessage, onSpeak: (String, String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End
    ) {
        // User (Lian Orijinál)
        Surface(
            shape = RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp),
            color = LightPurpleColor,
            modifier = Modifier.padding(start = 40.dp)
        ) {
            Text(
                text = message.originalText,
                modifier = Modifier.padding(12.dp),
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // AI (Tradusaun)
        Surface(
            shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp),
            color = DarkPurpleColor,
            modifier = Modifier.align(Alignment.Start).padding(end = 40.dp),
            shadowElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "\" ${message.translatedText} \"",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                
                IconButton(
                    onClick = { onSpeak(message.translatedText, message.targetLangCode) },
                    modifier = Modifier.size(24.dp).align(Alignment.End)
                ) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
