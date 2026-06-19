package com.example.app_translate.data.model

data class Language(val name: String, val code: String, val apiCode: String = code)

val languages = listOf(
    Language("English", "en", "en-US"),
    Language("Portuguese", "pt", "pt-PT"),
    Language("Indonesian", "id"),
    Language("Spanish", "es"),
    Language("French", "fr"),
    Language("Japanese", "ja"),
    Language("German", "de"),
    Language("Arabic", "ar"),
    Language("Chinese", "zh"),
    Language("Tetum", "tet")
)
