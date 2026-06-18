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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// IMPORT WARNA: Baris ini sangat penting agar warna tidak merah
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
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "TRANSLATION RESULT",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = GrayColor,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 160.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(DarkPurpleColor)
                .padding(20.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = WhiteColor,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Text(
                    text = if (isError) "An error occurred..." else outputText,
                    fontSize = 20.sp,
                    color = if (isError) RedColor else WhiteColor,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Aksi di bawah output
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onShare) {
                Text("📤 Share", color = PurpleColor)
            }
            TextButton(onClick = onSpeak) {
                Text("🔊 Read aloud", color = PurpleColor)
            }
            TextButton(onClick = onCopy) {
                Text("📋 Copy", color = PurpleColor)
            }
        }
    }
}