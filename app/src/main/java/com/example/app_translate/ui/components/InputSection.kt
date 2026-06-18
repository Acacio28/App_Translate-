package com.example.app_translate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// IMPORT WARNA: Pastikan baris di bawah ini sesuai dengan nama package Anda
import com.example.app_translate.ui.theme.*

@Composable
fun InputSection(
    inputText: String,
    onInputChanged: (String) -> Unit,
    onSpeak: () -> Unit,
    onCopy: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "INPUT TEXT",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = GrayColor, // Diambil dari Color.kt
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 160.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(LightPurpleColor.copy(alpha = 0.5f)) // .copy() otomatis dari Color
                .padding(20.dp)
        ) {
            BasicTextField(
                value = inputText,
                onValueChange = onInputChanged,
                textStyle = TextStyle(fontSize = 20.sp, color = Color.Black),
                cursorBrush = SolidColor(PurpleColor),
                decorationBox = { inner ->
                    if (inputText.isEmpty()) {
                        Text(
                            text = "Start typing...",
                            color = GrayColor,
                            fontSize = 20.sp
                        )
                    }
                    inner()
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onSpeak) {
                Text("🔊 Listen", color = PurpleColor)
            }
            TextButton(onClick = onCopy) {
                Text("📋 Copy", color = PurpleColor)
            }
        }
    }
}