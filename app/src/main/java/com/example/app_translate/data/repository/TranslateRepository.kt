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
                val encoded = URLEncoder.encode(text, "UTF-8")
                val url = "https://api.mymemory.translated.net/get?q=$encoded&langpair=$sourceLang|$targetLang&de=enzi23dev@gmail.com"

                val conn = URL(url).openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("User-Agent", "Mozilla/5.0")
                conn.connectTimeout = 10000
                conn.readTimeout = 10000

                val response = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                val translated = json.getJSONObject("responseData").getString("translatedText")
                val decoded = android.text.Html.fromHtml(translated, android.text.Html.FROM_HTML_MODE_LEGACY).toString()

                Result.success(decoded)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
