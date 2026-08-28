package com.beatwave.music

import kotlinx.serialization.Serializable

object SupabaseConfig {
    var URL: String = BuildConfig.SUPABASE_URL
    var ANON_KEY: String = BuildConfig.SUPABASE_PUBLISHABLE_KEY
    var SECRET_KEY: String = BuildConfig.SUPABASE_SECRET_KEY
    var JWKS_URL: String = BuildConfig.SUPABASE_JWKS_URL

    fun init(url: String, anon: String, secret: String, jwks: String) {
        URL = url
        ANON_KEY = anon
        SECRET_KEY = secret
        JWKS_URL = jwks
    }
}

@Serializable
data class AppUpdateRow(
    val id: Long? = null,
    val created_at: String? = null,
    val version: String,
    val title: String,
    val description: String,
    val update_type: String, // "optional" or "force"
    val apk_url: String? = null
)

@Serializable
data class SuggestionRow(
    val id: Long? = null,
    val created_at: String? = null,
    val content: String,
    val status: String = "pending",
    val user_name: String,
    val insta_id: String? = null
)

@Serializable
data class NewsUpdateRow(
    val id: Long? = null,
    val created_at: String? = null,
    val title: String,
    val content: String,
    val image_url: String? = null
)

@Serializable
data class BugReportRow(
    val id: Long? = null,
    val created_at: String? = null,
    val user_name: String,
    val insta_id: String? = null,
    val description: String,
    val device_info: String? = null,
    val status: String = "pending"
)
