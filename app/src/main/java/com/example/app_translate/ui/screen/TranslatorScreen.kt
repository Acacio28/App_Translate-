import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.app_translate.ui.components.InputSection
import com.example.app_translate.ui.components.LanguagePickerDialog
import com.example.app_translate.ui.components.OutputSection
import com.example.app_translate.ui.theme.DarkPurpleColor
import com.example.app_translate.ui.theme.LightPurpleColor
import com.example.app_translate.ui.theme.PurpleColor
import com.example.app_translate.viewmodel.TranslatorUiState
import com.example.app_translate.viewmodel.TranslatorViewModel

enum class ScreenTab { Translate, Dialogue, Camera, History }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslatorScreen(
    tts: TextToSpeech?,
    ttsReady: () -> Boolean,
    viewModel: TranslatorViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var currentTab by remember { mutableStateOf(ScreenTab.Translate) }

    var showSourcePicker by remember { mutableStateOf(false) }
    var showTargetPicker by remember { mutableStateOf(false) }

    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val spoken =
            result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (spoken != null) viewModel.onInputChanged(spoken)
    }

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

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
                NavigationBarItem(
                    selected = currentTab == ScreenTab.Translate,
                    onClick = { currentTab = ScreenTab.Translate },
                    icon = { Icon(Icons.Default.Translate, null) },
                    label = { Text("Translate", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PurpleColor,
                        indicatorColor = LightPurpleColor
                    )
                )
                NavigationBarItem(
                    selected = currentTab == ScreenTab.Dialogue,
                    onClick = {
                        currentTab = ScreenTab.Dialogue
                        viewModel.onInputChanged("")
                    },
                    icon = { Icon(Icons.Default.Chat, null) },
                    label = { Text("Dialogue", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PurpleColor,
                        indicatorColor = LightPurpleColor
                    )
                )
                NavigationBarItem(
                    selected = currentTab == ScreenTab.Camera,
                    onClick = { currentTab = ScreenTab.Camera },
                    icon = { Icon(Icons.Default.PhotoCamera, null) },
                    label = { Text("Camera", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PurpleColor,
                        indicatorColor = LightPurpleColor
                    )
                )
                NavigationBarItem(
                    selected = currentTab == ScreenTab.History,
                    onClick = { currentTab = ScreenTab.History },
                    icon = { Icon(Icons.Default.History, null) },
                    label = { Text("History", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PurpleColor,
                        indicatorColor = LightPurpleColor
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentTab) {
                ScreenTab.Translate -> TranslateDisplay(
                    viewModel, uiState,
                    { showSourcePicker = true },
                    { showTargetPicker = true }
                )

                ScreenTab.Dialogue -> DialogueDisplay(
                    uiState = uiState,
                    onMicClick = { langCode, _ ->
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(
                                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                            )
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, langCode)
                        }
                        try {
                            voiceLauncher.launch(intent)
                        } catch (e: Exception) {
                        }
                    },
                    tts = tts,
                    ttsReady = ttsReady,
                    onClearInput = { viewModel.onInputChanged("") }
                )

                ScreenTab.Camera -> CameraDisplay()
                ScreenTab.History -> HistoryDisplay()
            }
        }
    }
}

@Composable
fun TranslateDisplay(
    viewModel: TranslatorViewModel,
    uiState: TranslatorUiState,
    onSourceClick: () -> Unit,
    onTargetClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Translate", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = PurpleColor)
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onSourceClick,
                colors = ButtonDefaults.buttonColors(containerColor = LightPurpleColor),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(uiState.sourceLang.name, color = PurpleColor)
            }
            IconButton(
                onClick = { viewModel.onSwapLanguages() },
                modifier = Modifier.background(DarkPurpleColor, RoundedCornerShape(12.dp))
            ) {
                Icon(Icons.AutoMirrored.Filled.CompareArrows, null, tint = Color.White)
            }
            Button(
                onClick = onTargetClick,
                colors = ButtonDefaults.buttonColors(containerColor = LightPurpleColor),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(uiState.targetLang.name, color = PurpleColor)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        InputSection(uiState.inputText, { viewModel.onInputChanged(it) }, {}, {})
        Spacer(modifier = Modifier.height(24.dp))
        OutputSection(uiState.outputText, uiState.isLoading, uiState.isError, {}, {}, {})
    }
}

// --- PERBAIKAN: Menambahkan fungsi DialogueDisplay yang tadinya hilang ---
@Composable
fun DialogueDisplay(
    uiState: TranslatorUiState,
    onMicClick: (String, Boolean) -> Unit,
    tts: TextToSpeech?,
    ttsReady: () -> Boolean,
    onClearInput: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Dialogue", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = PurpleColor)
        Spacer(modifier = Modifier.height(40.dp))

        // Tombol Mic untuk Bicara
        FloatingActionButton(
            onClick = { onMicClick(uiState.sourceLang.code, true) },
            containerColor = PurpleColor,
            contentColor = Color.White,
            shape = RoundedCornerShape(50.dp),
            modifier = Modifier.size(80.dp)
        ) {
            Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(40.dp))
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text("Tap to Speak (${uiState.sourceLang.name})", color = Color.Gray)

        if (uiState.inputText.isNotEmpty()) {
            Spacer(modifier = Modifier.height(30.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = LightPurpleColor),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = uiState.inputText, fontSize = 18.sp, color = Color.Black)
                    if (uiState.outputText.isNotEmpty()) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = PurpleColor.copy(alpha = 0.2f)
                        )
                        Text(
                            text = uiState.outputText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = PurpleColor
                        )
                    }
                }
            }

            TextButton(onClick = onClearInput) {
                Text("Clear Conversation", color = Color.Red)
            }
        }
    }
}

@Composable
fun CameraDisplay() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Camera Screen") }
}

@Composable
fun HistoryDisplay() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("History Screen") }
}