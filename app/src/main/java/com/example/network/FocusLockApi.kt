package com.example.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class OnlineRankEntry(
    val nickname: String,
    val totalHours: Float,
    val xp: Int,
    val avatarIndex: Int,
    val customAvatarUri: String?
)

object FocusLockApi {

    // Change this to your deployed Render/Railway URL later
    private const val BASE_URL = "https://focuslock-backend.onrender.com"

    private suspend fun post(endpoint: String, body: JSONObject): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL$endpoint")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 8000
            conn.readTimeout = 8000

            OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }

            if (conn.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = reader.readText()
                reader.close()
                JSONObject(response)
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private suspend fun get(endpoint: String): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL$endpoint")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 8000
            conn.readTimeout = 8000

            if (conn.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = reader.readText()
                reader.close()
                JSONObject(response)
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun syncProfile(
        nickname: String,
        customAvatarUri: String?,
        avatarIndex: Int,
        totalHours: Float,
        xp: Int
    ): Boolean {
        val body = JSONObject().apply {
            put("nickname", nickname)
            put("customAvatarUri", customAvatarUri ?: JSONObject.NULL)
            put("avatarIndex", avatarIndex)
            put("totalHours", totalHours.toDouble())
            put("xp", xp)
        }
        val result = post("/api/users/sync", body)
        return result?.optString("status") == "success"
    }

    suspend fun getOnlineRanking(): List<OnlineRankEntry> {
        val result = get("/api/users/ranking") ?: return emptyList()
        val rankingArray = result.optJSONArray("ranking") ?: return emptyList()

        return (0 until rankingArray.length()).map { i ->
            val obj = rankingArray.getJSONObject(i)
            OnlineRankEntry(
                nickname = obj.optString("nickname", "???"),
                totalHours = obj.optDouble("totalHours", 0.0).toFloat(),
                xp = obj.optInt("xp", 0),
                avatarIndex = obj.optInt("avatarIndex", 0),
                customAvatarUri = if (obj.isNull("customAvatarUri")) null else obj.optString("customAvatarUri")
            )
        }
    }
}
