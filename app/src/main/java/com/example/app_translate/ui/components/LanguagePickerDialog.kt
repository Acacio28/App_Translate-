package com.example.app_translate.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.app_translate.data.model.Language
import com.example.app_translate.data.model.languages
import com.example.app_translate.ui.theme.PurpleColor

@Composable
fun LanguagePickerDialog(
    title: String,
    currentLang: Language,
    onLanguageSelected: (Language) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                languages.forEach { lang ->
                    TextButton(
                        onClick = {
                            onLanguageSelected(lang)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = lang.name,
                            color = if (lang.code == currentLang.code) PurpleColor else Color.Black
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = PurpleColor)
            }
        }
    )
}