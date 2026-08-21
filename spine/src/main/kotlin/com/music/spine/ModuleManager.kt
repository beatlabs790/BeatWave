package com.music.spine

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import timber.log.Timber

class ModuleManager {

    private val json = Json {
        isLenient = true
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val client by lazy {
        HttpClient(OkHttp) {
            install(ContentNegotiation) { json(json) }
            install(HttpTimeout) {
                requestTimeoutMillis = 15_000
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 15_000
            }
            expectSuccess = false
        }
    }

    private val loadedModules = mutableMapOf<String, LoadedModule>()

    data class LoadedModule(
        val module: SpineModule,
        val jsCode: String,
        val baseUrl: String,
    )

    suspend fun fetchIndex(sourceUrl: String): Result<List<SpineModule>> = withContext(Dispatchers.IO) {
        Log.d(TAG, "▶ fetchIndex($sourceUrl)")
        runCatching {
            val resp = client.get(sourceUrl)
            Log.d(TAG, "  HTTP ${resp.status.value}")
            if (resp.status != HttpStatusCode.OK) {
                val errBody = resp.bodyAsText().take(500)
                Log.e(TAG, "  ✗ HTTP ${resp.status.value}: $errBody")
                throw Exception("HTTP ${resp.status.value} from $sourceUrl")
            }
            val body = resp.bodyAsText()
            Log.d(TAG, "  Index body: ${body.length} chars")
            Log.d(TAG, "  First 500: ${body.take(500)}")
            val modules = ModuleIndex.parseModules(json, body)
            Log.d(TAG, "  Parsed ${modules.size} modules")
            for (m in modules) {
                Log.d(TAG, "    • [${m.id}] ${m.name} v${m.version} tags=${m.tags} download=${m.download}")
            }
            modules
        }.onFailure {
            Log.e(TAG, "  ✗ fetchIndex FAILED for $sourceUrl: ${it.message}", it)
        }
    }

    suspend fun loadModule(module: SpineModule, resolveBaseUrl: suspend (String) -> String = { it }): Result<LoadedModule> = withContext(Dispatchers.IO) {
        val cached = loadedModules[module.id]
        if (cached != null) {
            Log.d(TAG, "▶ loadModule(${module.id}) — CACHE HIT")
            return@withContext Result.success(cached)
        }

        Log.d(TAG, "▶ loadModule(${module.id}) download=${module.download}")
        runCatching {
            val downloadUrl = if (module.download.startsWith("http")) {
                module.download
            } else {
                val base = resolveBaseUrl(module.download)
                "$base/${module.download}"
            }

            Log.d(TAG, "  Resolved download URL: $downloadUrl")
            val resp = client.get(downloadUrl)
            Log.d(TAG, "  HTTP ${resp.status.value}")
            if (resp.status != HttpStatusCode.OK) {
                val errBody = resp.bodyAsText().take(500)
                Log.e(TAG, "  ✗ HTTP ${resp.status.value}: $errBody")
                throw Exception("HTTP ${resp.status.value} downloading module ${module.id}")
            }
            val jsCode = resp.bodyAsText()
            val baseUrl = downloadUrl.substringBeforeLast("/")

            QuickJsExecutor.loadModule(module.id, jsCode, baseUrl).getOrThrow()

            val loaded = LoadedModule(module = module, jsCode = jsCode, baseUrl = baseUrl)
            loadedModules[module.id] = loaded
            Log.d(TAG, "  ✓ Loaded module ${module.id}: ${jsCode.length} chars, baseUrl=$baseUrl")
            Log.d(TAG, "  First 300: ${jsCode.take(300)}")
            loaded
        }.onFailure {
            Log.e(TAG, "  ✗ loadModule FAILED for ${module.id}: ${it.message}", it)
        }
    }

    suspend fun searchTracks(
        loaded: LoadedModule,
        query: String,
        limit: Int = 50,
        settings: Map<String, String> = emptyMap(),
    ): Result<ModuleSearchResponse> = withContext(Dispatchers.IO) {
        val settingsJson = settings.entries.joinToString(",") { "\"${it.key}\":\"${it.value}\"" }
        val contextArg = "{settings:{value:{$settingsJson}}}"
        Log.d(TAG, "▶ searchTracks() module=${loaded.module.id} query=\"$query\" limit=$limit settings=$settings")
        Log.d(TAG, "  contextArg: $contextArg")

        runCatching {
            val result = QuickJsExecutor.callExport(
                moduleId = loaded.module.id,
                functionName = "searchTracks",
                args = listOf("\"$query\"", limit.toString(), contextArg),
            ).getOrThrow()

            Log.d(TAG, "  searchTracks result: ${result.length} chars")
            Log.d(TAG, "  Full result: $result")

            try {
                val parsed = json.decodeFromString<ModuleSearchResponse>(result)
                Log.d(TAG, "  ✓ Parsed ${parsed.tracks.size} tracks (total=${parsed.total})")
                for ((i, t) in parsed.tracks.withIndex()) {
                    Log.d(TAG, "    [$i] id=${t.id} title=\"${t.title}\" artist=\"${t.artist}\" quality=${t.audioQuality} duration=${t.duration}s")
                }
                parsed
            } catch (e: Exception) {
                Log.e(TAG, "  ✗ JSON parse FAILED for searchTracks: ${result.take(500)}", e)
                throw e
            }
        }.onFailure {
            Log.e(TAG, "  ✗ searchTracks FAILED for module ${loaded.module.id} query='$query': ${it.message}", it)
        }
    }

    suspend fun getStreamUrl(
        loaded: LoadedModule,
        trackId: String,
        settings: Map<String, String> = emptyMap(),
    ): Result<ModuleStreamResponse> = withContext(Dispatchers.IO) {
        val settingsJson = settings.entries.joinToString(",") { "\"${it.key}\":\"${it.value}\"" }
        val contextArg = "{settings:{value:{$settingsJson}}}"
        Log.d(TAG, "▶ getStreamUrl() module=${loaded.module.id} trackId=$trackId settings=$settings")
        Log.d(TAG, "  contextArg: $contextArg")

        runCatching {
            val result = QuickJsExecutor.callExport(
                moduleId = loaded.module.id,
                functionName = "getTrackStreamUrl",
                args = listOf("\"$trackId\"", "\"\"", contextArg),
            ).getOrThrow()

            Log.d(TAG, "  getStreamUrl result: ${result.length} chars")
            Log.d(TAG, "  Full result: $result")

            try {
                val parsed = json.decodeFromString<ModuleStreamResponse>(result)
                Log.d(TAG, "  ✓ Parsed stream response:")
                Log.d(TAG, "    streamUrl: ${parsed.streamUrl?.take(200)}")
                Log.d(TAG, "    track?.id: ${parsed.track?.id}")
                Log.d(TAG, "    track?.audioQuality: ${parsed.track?.audioQuality}")
                Log.d(TAG, "    track?.mimeType: ${parsed.track?.mimeType}")
                Log.d(TAG, "    track?.bitDepth: ${parsed.track?.bitDepth}")
                Log.d(TAG, "    track?.sampleRate: ${parsed.track?.sampleRate}")
                Log.d(TAG, "    track?.audioModes: ${parsed.track?.audioModes}")
                parsed
            } catch (e: Exception) {
                Log.e(TAG, "  ✗ JSON parse FAILED for getStreamUrl: ${result.take(500)}", e)
                throw e
            }
        }.onFailure {
            Log.e(TAG, "  ✗ getStreamUrl FAILED for module ${loaded.module.id} trackId=$trackId: ${it.message}", it)
        }
    }

    fun unloadModule(moduleId: String) {
        loadedModules.remove(moduleId)
        QuickJsExecutor.unload(moduleId)
        Log.d(TAG, "Unloaded module $moduleId")
    }

    fun unloadAll() {
        loadedModules.clear()
        QuickJsExecutor.unloadAll()
        Log.d(TAG, "Unloaded all modules")
    }

    companion object {
        private const val TAG = "SpineDebug"
    }
}
