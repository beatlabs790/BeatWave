package com.music.spine

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

object ModuleIndex {
    private val excludedCategories = setOf("category:artworks", "category:testing")
    private val listSerializer = ListSerializer(SpineModule.serializer())

    /**
     * Sources aren't limited to the handful of "category:modules"/"category:music"/
     * "category:debrid_modules" keys the format started with — a meta-index can
     * bundle other sources' modules under arbitrary "category:<key>" keys (e.g.
     * "category:ricky_modules"). A fixed-field data class silently dropped every
     * module under an unrecognized key. Scanning every "category:" key instead
     * picks those up too.
     */
    fun parseModules(json: Json, body: String): List<SpineModule> {
        val obj = json.decodeFromString(JsonObject.serializer(), body)
        return obj.entries
            .filter { it.key.startsWith("category:") && it.key !in excludedCategories }
            .flatMap { (_, value) ->
                runCatching { json.decodeFromJsonElement(listSerializer, value) }.getOrElse { emptyList() }
            }
            .distinctBy { it.id }
    }
}
