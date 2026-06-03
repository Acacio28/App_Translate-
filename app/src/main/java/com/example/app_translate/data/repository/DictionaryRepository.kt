// data/repository/DictionaryRepository.kt
package com.example.app_translate.data.repository

import com.example.app_translate.data.model.Definition
import com.example.app_translate.data.model.DictionaryEntry
import com.example.app_translate.data.model.Meaning
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

class DictionaryRepository {

    suspend fun lookup(word: String): Result<DictionaryEntry> {
        return withContext(Dispatchers.IO) {
            try {
                val url = "https://api.dictionaryapi.dev/api/v2/entries/en/${word.trim()}"
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.requestMethod = "GET"

                try {
                    val response = conn.inputStream.bufferedReader().readText()
                    val json = JSONArray(response)
                    val entry = json.getJSONObject(0)

                    val wordText = entry.getString("word")
                    val phonetic = entry.optString("phonetic", null)

                    val meaningsJson = entry.getJSONArray("meanings")
                    val meanings = mutableListOf<Meaning>()

                    for (i in 0 until meaningsJson.length()) {
                        val m = meaningsJson.getJSONObject(i)
                        val pos = m.getString("partOfSpeech")
                        val defsJson = m.getJSONArray("definitions")
                        val defs = mutableListOf<Definition>()

                        for (j in 0 until minOf(defsJson.length(), 3)) { // max 3 definisi
                            val d = defsJson.getJSONObject(j)
                            defs.add(
                                Definition(
                                    definition = d.getString("definition"),
                                    example = d.optString("example", null)
                                )
                            )
                        }
                        meanings.add(Meaning(pos, defs))
                    }

                    Result.success(DictionaryEntry(wordText, phonetic, meanings))
                } finally {
                    conn.disconnect()
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}