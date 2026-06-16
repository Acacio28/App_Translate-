package com.example.app_translate.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.app_translate.ui.theme.PurpleColor
import com.example.app_translate.ui.theme.WhiteColor
import com.example.app_translate.viewmodel.TranslatorViewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.Locale

@Composable
fun CameraScreen(
    viewModel: TranslatorViewModel,
    tts: TextToSpeech? = null,
    ttsReady: () -> Boolean = { false },
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
        )
    }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            capturedBitmap = bitmap
            isProcessing = true

            val inputImage = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    isProcessing = false
                    if (visionText.text.isNotBlank()) {
                        viewModel.onInputChanged(visionText.text)
                    } else {
                        Toast.makeText(context, "Tidak ada teks terdeteksi", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    isProcessing = false
                    Toast.makeText(context, "Gagal membaca teks dari foto", Toast.LENGTH_SHORT).show()
                }
        }
    }

    fun launchCamera() {
        if (hasCameraPermission) {
            cameraLauncher.launch(null)
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun speakOutput() {
        val text = uiState.outputText
        if (!ttsReady() || text.isBlank()) return
        val locale = when (uiState.targetLang.code) {
            "en" -> Locale.US
            "id" -> Locale("in", "ID")
            else -> Locale.US
        }
        tts?.language = locale
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Top bar: tombol back + indikator bahasa
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = Color(0xFF1A1A1A)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Row(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.06f), RoundedCornerShape(50.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(uiState.sourceLang.name, color = PurpleColor, fontSize = 14.sp)
                    Icon(
                        Icons.AutoMirrored.Filled.CompareArrows,
                        null,
                        tint = PurpleColor,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Text(uiState.targetLang.name, color = PurpleColor, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(48.dp)) // biar judul tetap di tengah
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                capturedBitmap?.let { bmp ->
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (isProcessing) {
                    CircularProgressIndicator(color = PurpleColor)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Membaca teks dari foto...", color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))
                } else if (uiState.outputText.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = WhiteColor,
                        shadowElevation = 2.dp
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = uiState.outputText,
                                modifier = Modifier
                                    .padding(12.dp)
                                    .weight(1f, fill = false),
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { speakOutput() }) {
                                Icon(Icons.Default.VolumeUp, contentDescription = "Dengar", tint = PurpleColor)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                FloatingActionButton(
                    onClick = { launchCamera() },
                    containerColor = PurpleColor,
                    contentColor = WhiteColor,
                    shape = CircleShape
                ) {
                    Icon(
                        Icons.Default.PhotoCamera,
                        contentDescription = "Ambil Foto",
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Ketuk untuk buka kamera & terjemahkan",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}