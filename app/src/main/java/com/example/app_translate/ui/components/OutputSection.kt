package com.example.app_translate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app_translate.ui.theme.*

@Composable
fun OutputSection(
    outputText: String,
    isLoading: Boolean,
    isError: Boolean,
    onSpeak: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "Output", fontSize = 12.sp, color = GrayColor)
        if (outputText.isNotEmpty() && !isLoading && !isError) {
            TextButton(onClick = onShare) {
                Text(text = "📤 Bagikan", color = PurpleColor)
            }
        }
    }

    Spacer(modifier = Modifier.height(4.dp))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkPurpleColor)
            .padding(16.dp)
    ) {
        when {
            isLoading -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = PurpleColor,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Menerjemahkan...", color = GrayColor)
                }
            }
            isError -> Text(text = outputText, color = RedColor)
            outputText.isNotEmpty() -> Text(text = outputText, fontSize = 18.sp, color = PurpleColor)
            else -> Text(text = "Hasil terjemahan akan muncul di sini...", color = LightGrayColor)
        }
    }

    if (outputText.isNotEmpty() && !isLoading && !isError) {
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