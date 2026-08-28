package com.beatwave.music.api

import com.beatwave.music.AppUpdateRow
import com.beatwave.music.NewsUpdateRow
import com.beatwave.music.SuggestionRow
import com.beatwave.music.BugReportRow
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

    suspend fun submitSuggestion(userName: String, instaId: String?, content: String): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val suggestion = SuggestionRow(
                user_name = userName,
                insta_id = instaId,
                content = content,
                status = "pending"
            )
            val bodyStr = json.encodeToString(suggestion)
            val request = Request.Builder()
                .url("${SupabaseConfig.URL}/rest/v1/suggestions")
                .post(bodyStr.toRequestBody(mediaType))
                .header("apikey", SupabaseConfig.SECRET_KEY)
                .header("Authorization", "Bearer ${SupabaseConfig.SECRET_KEY}")
                .header("Content-Type", "application/json")
                .header("Prefer", "return=representation")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val list = json.decodeFromString<List<SuggestionRow>>(body)
                    val inserted = list.firstOrNull()
                    if (inserted?.id != null) {
                        Result.success(inserted.id)
                    } else {
                        Result.failure(Exception("Failed to retrieve suggestion ID"))
                    }
                } else {
                    val errorBody = response.body?.string() ?: ""
                    Result.failure(Exception("HTTP error: ${response.code} ${response.message} - $errorBody"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchSuggestionsByIds(ids: List<Long>): Result<List<SuggestionRow>> = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext Result.success(emptyList())
        try {
            val idsParam = ids.joinToString(",")
            val request = Request.Builder()
                .url("${SupabaseConfig.URL}/rest/v1/suggestions?id=in.($idsParam)&order=created_at.desc")
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

    suspend fun submitBugReport(userName: String, instaId: String?, description: String, deviceInfo: String): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val bugReport = BugReportRow(
                user_name = userName,
                insta_id = instaId,
                description = description,
                device_info = deviceInfo,
                status = "pending"
            )
            val bodyStr = json.encodeToString(bugReport)
            val request = Request.Builder()
                .url("${SupabaseConfig.URL}/rest/v1/bug_reports")
                .post(bodyStr.toRequestBody(mediaType))
                .header("apikey", SupabaseConfig.SECRET_KEY)
                .header("Authorization", "Bearer ${SupabaseConfig.SECRET_KEY}")
                .header("Content-Type", "application/json")
                .header("Prefer", "return=representation")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val list = json.decodeFromString<List<BugReportRow>>(body)
                    val inserted = list.firstOrNull()
                    if (inserted?.id != null) {
                        Result.success(inserted.id)
                    } else {
                        Result.failure(Exception("Failed to retrieve bug report ID"))
                    }
                } else {
                    val errorBody = response.body?.string() ?: ""
                    Result.failure(Exception("HTTP error: ${response.code} ${response.message} - $errorBody"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchBugReports(): Result<List<BugReportRow>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("${SupabaseConfig.URL}/rest/v1/bug_reports?order=created_at.desc")
                .get()
                .header("apikey", SupabaseConfig.SECRET_KEY)
                .header("Authorization", "Bearer ${SupabaseConfig.SECRET_KEY}")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@use Result.success(emptyList())
                    val list = json.decodeFromString<List<BugReportRow>>(body)
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

    suspend fun fetchBugReportsByIds(ids: List<Long>): Result<List<BugReportRow>> = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext Result.success(emptyList())
        try {
            val idsParam = ids.joinToString(",")
            val request = Request.Builder()
                .url("${SupabaseConfig.URL}/rest/v1/bug_reports?id=in.($idsParam)&order=created_at.desc")
                .get()
                .header("apikey", SupabaseConfig.SECRET_KEY)
                .header("Authorization", "Bearer ${SupabaseConfig.SECRET_KEY}")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@use Result.success(emptyList())
                    val list = json.decodeFromString<List<BugReportRow>>(body)
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

    suspend fun updateBugReportStatus(id: Long, status: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val body = buildJsonObject {
                put("status", status)
            }
            val request = Request.Builder()
                .url("${SupabaseConfig.URL}/rest/v1/bug_reports?id=eq.$id")
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

    suspend fun publishNews(news: NewsUpdateRow): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val bodyStr = json.encodeToString(news)
            val request = Request.Builder()
                .url("${SupabaseConfig.URL}/rest/v1/news_updates")
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

    suspend fun checkLatestNews(): Result<NewsUpdateRow?> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("${SupabaseConfig.URL}/rest/v1/news_updates?order=created_at.desc&limit=1")
                .get()
                .header("apikey", SupabaseConfig.SECRET_KEY)
                .header("Authorization", "Bearer ${SupabaseConfig.SECRET_KEY}")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@use Result.success(null)
                    val list = json.decodeFromString<List<NewsUpdateRow>>(body)
                    Result.success(list.firstOrNull())
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
