package com.example.app_translate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app_translate.ui.theme.*

@Composable
fun InputSection(
    inputText: String,
    onInputChanged: (String) -> Unit,
    onSpeak: () -> Unit,
    onCopy: () -> Unit
) {
    Text(
        text = "Input",
        fontSize = 12.sp,
        color = GrayColor,
        modifier = Modifier.padding(bottom = 4.dp)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LightPurpleColor)
            .padding(16.dp)
    ) {
        BasicTextField(
            value = inputText,
            onValueChange = onInputChanged,
            textStyle = TextStyle(fontSize = 18.sp, color = Color.Black),
            cursorBrush = SolidColor(PurpleColor),
            decorationBox = { inner ->
                if (inputText.isEmpty()) {
                    Text(
                        text = "Ketik teks untuk diterjemahkan...",
                        color = LightGrayColor
                    )
                }
                inner()
            },
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (inputText.isNotEmpty()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onSpeak) {
                Text(text = "🔊 Baca", color = PurpleColor)
            }
            TextButton(onClick = onCopy) {
                Text(text = "📋 Salin", color = PurpleColor)
            }
        }
    }
}