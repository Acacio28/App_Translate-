package com.example.app_translate.data.repository // <--- 1. WAJIB ADA PACKAGE

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
                // Encode teks agar aman dikirim melalui URL
                val encoded = URLEncoder.encode(text, "UTF-8")

                // Menggunakan API MyMemory (Gratis & Tanpa Key untuk penggunaan ringan)
                val url = "https://api.mymemory.translated.net/get?q=$encoded&langpair=$sourceLang|$targetLang"

                val conn = URL(url).openConnection() as HttpURLConnection
                conn.requestMethod = "GET"

                try {
                    val response = conn.inputStream.bufferedReader().readText()
                    val json = JSONObject(response)
                    val translated = json.getJSONObject("responseData").getString("translatedText")

                    Result.success(translated)
                } finally {
                    conn.disconnect()
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
