package com.example.app_translate.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class TranslateRepository {

    suspend fun translate(text: String, from: String, to: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val encoded = URLEncoder.encode(text, "UTF-8")
                val url = "https://api.mymemory.translated.net/get?q=$encoded&langpair=$from|$to"
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
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
