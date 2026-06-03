// data/model/DictionaryModel.kt
package com.example.app_translate.data.model

data class DictionaryEntry(
    val word: String,
    val phonetic: String?,
    val meanings: List<Meaning>
)

data class Meaning(
    val partOfSpeech: String,
    val definitions: List<Definition>
)

data class Definition(
    val definition: String,
    val example: String?
)