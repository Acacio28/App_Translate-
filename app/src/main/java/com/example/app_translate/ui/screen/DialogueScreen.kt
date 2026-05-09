package com.example.app_translate.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app_translate.ui.theme.*
import com.example.app_translate.viewmodel.DialogueMessage
import com.example.app_translate.viewmodel.TranslatorViewModel
import kotlinx.coroutines.launch

@Composable
fun DialogueScreen(viewModel: TranslatorViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll ke bawah saat pesan baru masuk
    LaunchedEffect(uiState.dialogueMessages.size) {
        if (uiState.dialogueMessages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(uiState.dialogueMessages.size - 1)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        // Info bar: bahasa
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = WhiteColor,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Kamu: ${uiState.sourceLang.name}",
                    color = PurpleColor,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )
                Text("  ↔  ", color = GrayColor)
                Text(
                    "AI: ${uiState.targetLang.name}",
                    color = DarkPurpleColor,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )
            }
        }

        // Chat messages
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (uiState.dialogueMessages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillParentMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Mulai percakapan!\nKetik pesan di bawah.",
                            color = GrayColor,
                            fontSize = 14.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
            items(uiState.dialogueMessages) { message ->
                ChatBubble(message = message)
            }
            // Bubble loading saat AI sedang "membalas"
            if (uiState.isDialogueLoading) {
                item {
                    AiTypingBubble()
                }
            }
        }

        // Input bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = WhiteColor,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Ketik pesan...", color = GrayColor) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PurpleColor,
                        unfocusedBorderColor = LightGrayColor
                    ),
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (inputText.isNotBlank()) {
                            viewModel.sendDialogueMessage(inputText)
                            inputText = ""
                        }
                    })
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            viewModel.sendDialogueMessage(inputText)
                            inputText = ""
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (inputText.isNotBlank()) PurpleColor else LightGrayColor,
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Kirim",
                        tint = WhiteColor
                    )
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: DialogueMessage) {
    val isUser = message.isUser
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            // Avatar AI
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(DarkPurpleColor, shape = androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("AI", color = WhiteColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(6.dp))
        }

        Column(horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = if (isUser) 16.dp else 4.dp,
                    topEnd = if (isUser) 4.dp else 16.dp,
                    bottomStart = 16.dp,
                    bottomEnd = 16.dp
                ),
                color = if (isUser) PurpleColor else WhiteColor,
                shadowElevation = 2.dp,
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Teks asli
                    Text(
                        text = message.originalText,
                        color = if (isUser) WhiteColor else Color.Black,
                        fontSize = 15.sp
                    )
                    // Divider tipis
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 6.dp),
                        thickness = 0.5.dp,
                        color = if (isUser) WhiteColor.copy(alpha = 0.4f) else LightGrayColor
                    )
                    // Terjemahan di bawah
                    Text(
                        text = message.translatedText,
                        color = if (isUser) WhiteColor.copy(alpha = 0.85f) else PurpleColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(6.dp))
            // Avatar User
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(LightPurpleColor, shape = androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("U", color = PurpleColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AiTypingBubble() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(DarkPurpleColor, shape = androidx.compose.foundation.shape.CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("AI", color = WhiteColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(6.dp))
        Surface(
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
            color = WhiteColor,
            shadowElevation = 2.dp
        ) {
            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = PurpleColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("menerjemahkan...", color = GrayColor, fontSize = 13.sp)
            }
        }
    }
}