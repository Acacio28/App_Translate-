package com.example.app_translate.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class TranslateRepository {

    suspend fun translate(text: String, sourceLang: String, targetLang: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                // Kalau teks lebih dari 450 karakter, potong dan terjemahkan per bagian
                if (text.length > 450) {
                    val chunks = splitText(text, 450)
                    val translatedChunks = mutableListOf<String>()

                    for (chunk in chunks) {
                        val result = translateChunk(chunk, sourceLang, targetLang)
                        result.fold(
                            onSuccess = { translatedChunks.add(it) },
                            onFailure = { return@withContext Result.failure(it) }
                        )
                    }

                    Result.success(translatedChunks.joinToString(" "))
                } else {
                    translateChunk(text, sourceLang, targetLang)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private suspend fun translateChunk(text: String, sourceLang: String, targetLang: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val encoded = URLEncoder.encode(text, "UTF-8")
                val url = "https://api.mymemory.translated.net/get?q=$encoded&langpair=$sourceLang|$targetLang&de=enzi23dev@gmail.com"

                val conn = URL(url).openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 10000
                conn.readTimeout = 10000

                try {
                    val response = conn.inputStream.bufferedReader().readText()
                    val json = JSONObject(response)

                    // Cek response status
                    val responseStatus = json.optInt("responseStatus", 200)
                    if (responseStatus != 200) {
                        return@withContext Result.failure(Exception("API Error: $responseStatus"))
                    }

                    val translated = json.getJSONObject("responseData").getString("translatedText")

                    // Decode HTML entities
                    val decoded = android.text.Html.fromHtml(translated, android.text.Html.FROM_HTML_MODE_LEGACY).toString()

                    Result.success(decoded)
                } finally {
                    conn.disconnect()
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // Potong teks per kalimat agar tidak putus di tengah kata
    private fun splitText(text: String, maxLength: Int): List<String> {
        val chunks = mutableListOf<String>()
        val sentences = text.split(Regex("(?<=[.!?\\n])\\s*"))

        var current = StringBuilder()
        for (sentence in sentences) {
            if (current.length + sentence.length > maxLength) {
                if (current.isNotEmpty()) {
                    chunks.add(current.toString().trim())
                    current = StringBuilder()
                }
                // Kalau 1 kalimat saja sudah > maxLength, potong paksa per kata
                if (sentence.length > maxLength) {
                    val words = sentence.split(" ")
                    for (word in words) {
                        if (current.length + word.length + 1 > maxLength) {
                            chunks.add(current.toString().trim())
                            current = StringBuilder()
                        }
                        current.append("$word ")
                    }
                } else {
                    current.append(sentence)
                }
            } else {
                current.append(sentence)
            }
        }

        if (current.isNotEmpty()) {
            chunks.add(current.toString().trim())
        }

        return chunks
    }
}