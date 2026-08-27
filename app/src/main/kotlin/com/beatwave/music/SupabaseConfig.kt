package com.beatwave.music

import kotlinx.serialization.Serializable

object SupabaseConfig {
    const val URL = "https://urltgenawxcpmxuyeuod.supabase.co"
    const val ANON_KEY = "sb_publishable_FUbuyV6Pw9vlNqcPV_qTOA_9uzt4YNi"
    const val SECRET_KEY = "sb_secret_X9bYfzuBpXRLZ3KiFgbmWg_4XGmYwQ7"
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
