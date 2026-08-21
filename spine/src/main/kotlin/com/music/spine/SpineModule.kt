package com.music.spine

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SpineModule(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("author") val author: String = "",
    @SerialName("version") val version: String = "",
    @SerialName("code") val code: Int = 0,
    @SerialName("type") val type: String = "MODULE",
    @SerialName("description") val description: String = "",
    @SerialName("tags") private val declaredTags: List<String> = emptyList(),
    /**
     * Some sources publish the same list under "labels" instead of "tags", with no "tags"
     * key at all -- the monochrome/ricky source is one, and it is the first link the
     * community hands out for setting up lossless. Reading only "tags" left those modules
     * with an empty list, so Claudo and Qobuz (whose labels literally say FLAC / LOSSLESS
     * / HI-RES) reported isLossless = false and showed no chips.
     */
    @SerialName("labels") private val declaredLabels: List<String> = emptyList(),
    @SerialName("size") val size: Long = 0,
    @SerialName("sizeLabel") val sizeLabel: String = "",
    @SerialName("download") val download: String = "",
    @SerialName("logo") val logo: String? = null,
    @SerialName("icon") val icon: String? = null,
    @SerialName("lastUpdated") val lastUpdated: String = "",
    @SerialName("trusted") val trusted: Boolean = false,
    @SerialName("featured") val featured: Boolean = false,
    @SerialName("nsfw") val nsfw: Boolean = false,
    @SerialName("sources") val sources: List<SpineSource> = emptyList(),
) {
    /** The module's labels, whichever key the source published them under. */
    val tags: List<String> get() = if (declaredTags.isNotEmpty()) declaredTags else declaredLabels

    val isLossless: Boolean get() = tags.any { it.uppercase().contains("LOSSLESS") || it.uppercase().contains("HI-RES") || it.uppercase().contains("FLAC") }
    val hasHiRes: Boolean get() = tags.any { it.uppercase().contains("HI-RES") }
    val isDolbyAtmos: Boolean get() = tags.any { it.uppercase().contains("ATMOS") || it.uppercase().contains("DOLBY") }
}

@Serializable
data class SpineSource(
    @SerialName("name") val name: String = "",
    @SerialName("lang") val lang: String = "all",
    @SerialName("id") val id: String = "",
    @SerialName("baseUrl") val baseUrl: String = ".",
)
