package com.example.app_translate.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app_translate.data.model.Language
import com.example.app_translate.ui.theme.PurpleColor
import com.example.app_translate.ui.theme.WhiteColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WriteScreen(
    selectedLang: Language,
    onLangClick: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    var resultText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var selectedMode by remember { mutableStateOf("Fix Grammar") }
    val scope = rememberCoroutineScope()

    val apiKey = "YOUR_API_KEY_HERE"

    val modes = listOf("Fix Grammar", "Formal", "Casual", "Concise", "Expand")

    suspend fun processText(text: String, mode: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = when (mode) {
                    "Fix Grammar" -> "Fix the grammar and spelling of the following text. Only show the corrected result without explanation:\n\n$text"
                    "Formal"           -> "Rewrite the following text in a professional formal style. Only show the result:\n\n$text"
                    "Casual"           -> "Rewrite the following text in a casual and natural style. Only show the result:\n\n$text"
                    "Concise"          -> "Summarize the following text to be more concise without losing key points. Only show the result:\n\n$text"
                    "Expand"          -> "Expand the following text with more complete details. Only show the result:\n\n$text"
                    else               -> text
                }

                val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + apiKey)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                val body = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply { put("text", prompt) })
                            })
                        })
                    })
                }

                conn.outputStream.write(body.toString().toByteArray())

                val response = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                json.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
            } catch (e: Exception) {
                "Error: ${e.message}"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Language selector
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFEEEEEE),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Language: ", fontSize = 13.sp, color = Color.Gray)
                TextButton(onClick = onLangClick, contentPadding = PaddingValues(0.dp)) {
                    Text(
                        selectedLang.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PurpleColor
                    )
                    Icon(
                        Icons.Default.ArrowDropDown,
                        null,
                        tint = PurpleColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Mode selector
        Text(
            "Mode",
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A1A),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(androidx.compose.foundation.rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            modes.forEach { mode ->
                FilterChip(
                    selected = selectedMode == mode,
                    onClick = { selectedMode = mode },
                    label = { Text(mode, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PurpleColor,
                        selectedLabelColor = WhiteColor
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Input
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = WhiteColor),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "INPUT TEXT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { if (it.length <= 1000) inputText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 140.dp),
                    placeholder = { Text("Enter the text you want to improve...") },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PurpleColor,
                        unfocusedBorderColor = Color.LightGray
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${inputText.length}/1000",
                        fontSize = 12.sp,
                        color = if (inputText.length > 900) Color.Red else Color.Gray
                    )
                    TextButton(onClick = { inputText = ""; resultText = "" }) {
                        Text("Clear", color = Color.Gray)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Process button
        Button(
            onClick = {
                if (inputText.isNotBlank()) {
                    isLoading = true
                    resultText = ""
                    scope.launch {
                        resultText = processText(inputText, selectedMode)
                        isLoading = false
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PurpleColor),
            enabled = inputText.isNotBlank() && !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = WhiteColor,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                if (isLoading) "Processing..." else "✨ Process with AI",
                fontWeight = FontWeight.Bold
            )
        }

        // Result
        AnimatedVisibility(
            visible = resultText.isNotEmpty(),
            enter = fadeIn() + slideInVertically()
        ) {
            Column {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = WhiteColor),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "RESULT",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                            TextButton(onClick = {
                                inputText = resultText
                                resultText = ""
                            }) {
                                Text("Use this text", color = PurpleColor, fontSize = 12.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = resultText,
                            fontSize = 16.sp,
                            color = Color.Black,
                            lineHeight = 24.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}