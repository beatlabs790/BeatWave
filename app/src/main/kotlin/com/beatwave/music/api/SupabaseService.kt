package com.beatwave.music.api

import com.beatwave.music.AppUpdateRow
import com.beatwave.music.SuggestionRow
import com.beatwave.music.SupabaseConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

object SupabaseService {
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun publishUpdate(update: AppUpdateRow): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val bodyStr = json.encodeToString(update)
            val request = Request.Builder()
                .url("${SupabaseConfig.URL}/rest/v1/app_updates")
                .post(bodyStr.toRequestBody(mediaType))
                .header("apikey", SupabaseConfig.SECRET_KEY)
                .header("Authorization", "Bearer ${SupabaseConfig.SECRET_KEY}")
                .header("Content-Type", "application/json")
                .header("Prefer", "return=minimal")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    val errorBody = response.body?.string() ?: ""
                    Result.failure(Exception("HTTP error: ${response.code} ${response.message} - $errorBody"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkLatestUpdate(): Result<AppUpdateRow?> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("${SupabaseConfig.URL}/rest/v1/app_updates?order=created_at.desc&limit=1")
                .get()
                .header("apikey", SupabaseConfig.ANON_KEY)
                .header("Authorization", "Bearer ${SupabaseConfig.ANON_KEY}")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@use Result.success(null)
                    val updates = json.decodeFromString<List<AppUpdateRow>>(body)
                    Result.success(updates.firstOrNull())
                } else {
                    val errorBody = response.body?.string() ?: ""
                    Result.failure(Exception("HTTP error: ${response.code} ${response.message} - $errorBody"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitSuggestion(content: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val suggestion = SuggestionRow(content = content, status = "pending")
            val bodyStr = json.encodeToString(suggestion)
            val request = Request.Builder()
                .url("${SupabaseConfig.URL}/rest/v1/suggestions")
                .post(bodyStr.toRequestBody(mediaType))
                .header("apikey", SupabaseConfig.SECRET_KEY)
                .header("Authorization", "Bearer ${SupabaseConfig.SECRET_KEY}")
                .header("Content-Type", "application/json")
                .header("Prefer", "return=minimal")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    val errorBody = response.body?.string() ?: ""
                    Result.failure(Exception("HTTP error: ${response.code} ${response.message} - $errorBody"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchSuggestions(): Result<List<SuggestionRow>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("${SupabaseConfig.URL}/rest/v1/suggestions?order=created_at.desc")
                .get()
                .header("apikey", SupabaseConfig.SECRET_KEY)
                .header("Authorization", "Bearer ${SupabaseConfig.SECRET_KEY}")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@use Result.success(emptyList())
                    val list = json.decodeFromString<List<SuggestionRow>>(body)
                    Result.success(list)
                } else {
                    val errorBody = response.body?.string() ?: ""
                    Result.failure(Exception("HTTP error: ${response.code} ${response.message} - $errorBody"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateSuggestionStatus(id: Long, status: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val body = buildJsonObject {
                put("status", status)
            }
            val request = Request.Builder()
                .url("${SupabaseConfig.URL}/rest/v1/suggestions?id=eq.$id")
                .patch(body.toString().toRequestBody(mediaType))
                .header("apikey", SupabaseConfig.SECRET_KEY)
                .header("Authorization", "Bearer ${SupabaseConfig.SECRET_KEY}")
                .header("Content-Type", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    val errorBody = response.body?.string() ?: ""
                    Result.failure(Exception("HTTP error: ${response.code} ${response.message} - $errorBody"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteSuggestion(id: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("${SupabaseConfig.URL}/rest/v1/suggestions?id=eq.$id")
                .delete()
                .header("apikey", SupabaseConfig.SECRET_KEY)
                .header("Authorization", "Bearer ${SupabaseConfig.SECRET_KEY}")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    val errorBody = response.body?.string() ?: ""
                    Result.failure(Exception("HTTP error: ${response.code} ${response.message} - $errorBody"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
