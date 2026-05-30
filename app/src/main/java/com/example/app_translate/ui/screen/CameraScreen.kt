package com.example.app_translate.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import android.speech.tts.TextToSpeech
import androidx.compose.ui.geometry.Offset
import androidx.core.content.ContextCompat
import com.example.app_translate.ui.theme.PurpleColor
import com.example.app_translate.viewmodel.TranslatorViewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import android.view.MotionEvent
import androidx.camera.core.FocusMeteringAction
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle // New import

enum class FlashMode {
    OFF, ON, AUTO
}

@Composable
fun CameraScreen(
    tts: TextToSpeech?,
    ttsReady: () -> Boolean,
    viewModel: TranslatorViewModel
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var hasCameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasCameraPermission = it }

    LaunchedEffect(Unit) { if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA) }

    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var currentFlashMode by remember { mutableStateOf(FlashMode.OFF) }
    val cameraProvider = remember { cameraProviderFuture.get() }
    var previewUseCase by remember { mutableStateOf<Preview?>(null) }
    var camera: Camera? by remember { mutableStateOf(null) } // New state for Camera

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val inputImage = InputImage.fromFilePath(context, it)
            recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    viewModel.onInputChanged(visionText.text)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Failed to process image: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    val zoomState = rememberTransformableState { zoomChange, panChange, rotationChange ->
        camera?.cameraControl?.setZoomRatio(
            (camera?.cameraInfo?.zoomState?.value?.zoomRatio ?: 1f) * zoomChange
        )
    }

    LaunchedEffect(currentFlashMode) {
        if (hasCameraPermission) {
            cameraProvider.unbindAll()

            val newImageCapture = ImageCapture.Builder()
                .setFlashMode(
                    when (currentFlashMode) {
                        FlashMode.ON -> ImageCapture.FLASH_MODE_ON
                        FlashMode.OFF -> ImageCapture.FLASH_MODE_OFF
                        FlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
                    }
                )
                .build()
            imageCapture = newImageCapture

            val newPreview = Preview.Builder().build()
            previewUseCase = newPreview

            camera = cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, newPreview, newImageCapture)
        }
    }

    if (hasCameraPermission) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .transformable(state = zoomState)
        ) {

            // 1. Full Screen Camera Preview
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    previewUseCase?.setSurfaceProvider(previewView.surfaceProvider)

                    previewView.setOnTouchListener { v, event ->
                        if (event.action == MotionEvent.ACTION_UP) {
                            val factory = previewView.meteringPointFactory
                            val point = factory.createPoint(event.x, event.y)
                            val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
                                .disableAutoCancel()
                                .build()
                            camera?.cameraControl?.startFocusAndMetering(action)
                        }
                        true
                    }
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // 2. Target Frame (Kotak Bidik di Tengah)
            Box(
                modifier = Modifier
                    .size(width = 280.dp, height = 180.dp)
                    .align(Alignment.Center)
                    .border(BorderStroke(2.dp, Color.White), RoundedCornerShape(12.dp))
            )

            // 3. Top Bar (Language Selector - Floating)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp, start = 20.dp, end = 20.dp)
                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(50.dp))
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(uiState.sourceLang.name, color = Color.White, fontSize = 14.sp)
                Icon(Icons.AutoMirrored.Filled.CompareArrows, null, tint = Color.White, modifier = Modifier.padding(horizontal = 16.dp))
                Text(uiState.targetLang.name, color = Color.White, fontSize = 14.sp)
            }

            // 4. Bottom Section (Result & Shutter Button)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Hasil Terjemahan (Floating Text)
                if (uiState.outputText.isNotEmpty()) {
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = uiState.outputText,
                            modifier = Modifier.padding(12.dp),
                            color = Color.Black,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Flash Icon
                    IconButton(onClick = {
                        currentFlashMode = when (currentFlashMode) {
                            FlashMode.OFF -> FlashMode.ON
                            FlashMode.ON -> FlashMode.AUTO
                            FlashMode.AUTO -> FlashMode.OFF
                        }
                    }) {
                        val flashIcon = when (currentFlashMode) {
                            FlashMode.OFF -> Icons.Default.FlashOff
                            FlashMode.ON -> Icons.Default.FlashOn
                            FlashMode.AUTO -> Icons.Default.FlashAuto
                        }
                        Icon(flashIcon, null, tint = Color.White)
                    }

                    // Shutter Button (Tombol Bulat Besar)
                    Surface(
                        modifier = Modifier.size(70.dp),
                        shape = CircleShape,
                        color = Color.Transparent,
                        border = BorderStroke(4.dp, Color.White),
                        onClick = {
                            val capture = imageCapture ?: return@Surface
                            capture.takePicture(ContextCompat.getMainExecutor(context), object : ImageCapture.OnImageCapturedCallback() {
                                override fun onCaptureSuccess(image: ImageProxy) {
                                    val mediaImage = image.image
                                    if (mediaImage != null) {
                                        val inputImage = InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees)
                                        recognizer.process(inputImage)
                                            .addOnSuccessListener { visionText ->
                                                viewModel.onInputChanged(visionText.text)
                                                image.close()
                                            }
                                            .addOnFailureListener { image.close() }
                                    }
                                }
                            })
                        }
                    ) {
                        Box(modifier = Modifier.padding(4.dp).background(Color.White, CircleShape))
                    }

                    // Gallery Icon
                    IconButton(onClick = {
                        galleryLauncher.launch("image/*")
                    }) {
                        Icon(Icons.Default.PhotoLibrary, null, tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("Ketuk tombol untuk menerjemahkan", color = Color.White, fontSize = 12.sp)
            }
        }
    }
}