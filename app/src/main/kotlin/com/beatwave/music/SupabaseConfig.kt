package com.beatwave.music

import kotlinx.serialization.Serializable

object SupabaseConfig {
    const val URL = "https://urltgenawxcpmxuyeuod.supabase.co"
    const val ANON_KEY = "YOUR_ANON_KEY"
    const val SECRET_KEY = "YOUR_SECRET_KEY"
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
    val status: String = "pending"
)
