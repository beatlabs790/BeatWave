package com.beatwave.music

import kotlinx.serialization.Serializable

object SupabaseConfig {
    val URL = BuildConfig.SUPABASE_URL
    val ANON_KEY = BuildConfig.SUPABASE_PUBLISHABLE_KEY
    val SECRET_KEY = BuildConfig.SUPABASE_SECRET_KEY
    val JWKS_URL = BuildConfig.SUPABASE_JWKS_URL
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
