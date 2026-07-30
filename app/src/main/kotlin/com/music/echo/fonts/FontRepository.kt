package iad1tya.echo.music.fonts

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.annotation.StringRes
import dagger.hilt.android.qualifiers.ApplicationContext
import iad1tya.echo.music.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A failure with something to show the user.
 *
 * The message is carried as a resource id rather than text so it is resolved against the app's
 * locale at the point it is displayed, not here.
 */
class FontException(
    @StringRes val messageRes: Int,
    val formatArg: String? = null,
) : Exception()

/**
 * Single entry point for everything font related: browsing the remote catalog, installing from
 * Google Fonts or from a user-supplied file, and listing/removing what is installed.
 */
@Singleton
class FontRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    @Volatile
    private var memoryCatalog: List<AppFont>? = null

    /**
     * The browsable catalog, newest-first by popularity.
     *
     * Served from memory, then from the on-disk cache (valid for [CATALOG_TTL_MS]), then from the
     * network. [forceRefresh] skips both caches.
     */
    suspend fun catalog(forceRefresh: Boolean = false): List<AppFont> {
        if (!forceRefresh) {
            memoryCatalog?.let { return it }
            readCachedCatalog()?.let {
                memoryCatalog = it
                return it
            }
        }

        // The catalog is optional data, so nothing it throws should reach the screen. Throwable
        // rather than Exception: a failure inside GoogleFontsApi's initialiser surfaces as an
        // ExceptionInInitializerError, which a plain catch would let through.
        val fonts = try {
            GoogleFontsApi.fetchCatalog(client)
        } catch (e: Throwable) {
            Timber.e(e, "Font catalog unavailable")
            emptyList()
        }

        if (fonts.isEmpty()) return fonts

        memoryCatalog = fonts
        if (fonts !== GoogleFontsApi.FallbackFamilies) {
            writeCachedCatalog(fonts)
        }
        return fonts
    }

    /** [catalog] with installation state merged in, filtered by [query]. */
    suspend fun search(query: String, forceRefresh: Boolean = false): List<AppFont> {
        val installedIds = FontStorage.installedFontIds(context)
        return catalog(forceRefresh)
            .asSequence()
            .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
            .map { font ->
                if (font.id in installedIds) {
                    font.copy(installed = true, sizeBytes = FontStorage.sizeOf(context, font.id))
                } else {
                    font
                }
            }
            .toList()
    }

    fun installedFonts(): List<AppFont> {
        val manifest = readManifest()
        return FontStorage.installedFontIds(context)
            .map { id ->
                val variants = FontStorage.variantFiles(context, id).keys.sorted()
                val entry = manifest[id]
                AppFont(
                    id = id,
                    name = entry?.name ?: id.split('-').joinToString(" ") { part ->
                        part.replaceFirstChar { it.uppercase() }
                    },
                    provider = entry?.provider ?: FontProvider.USER_UPLOAD,
                    category = entry?.category,
                    variants = variants.ifEmpty { listOf(FontVariant.Regular) },
                    installed = true,
                    sizeBytes = FontStorage.sizeOf(context, id),
                    installedAt = entry?.installedAt ?: 0L,
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    fun totalInstalledSize(): Long = FontStorage.totalSize(context)

    /**
     * Downloads every available face of [font].
     *
     * [onProgress] receives 0f..1f. Fails only if not a single face could be fetched; a family
     * whose bold is missing still installs with the faces that did arrive.
     */
    suspend fun download(
        font: AppFont,
        onProgress: (Float) -> Unit = {},
    ): Result<AppFont> = withContext(Dispatchers.IO) {
        try {
            onProgress(0f)

            val urls = font.urls
                .mapNotNull { (key, url) -> FontVariant.fromKey(key)?.let { it to url } }
                .toMap()
                .ifEmpty { GoogleFontsApi.resolveDownloadUrls(client, font.name) }

            if (urls.isEmpty()) {
                return@withContext Result.failure(FontException(R.string.fonts_error_no_files, font.name))
            }

            val wanted = urls.filterKeys { it in FontVariant.Desired }
                .ifEmpty { urls.entries.sortedBy { it.key }.take(1).associate { it.key to it.value } }

            val installedVariants = mutableListOf<FontVariant>()
            wanted.entries.sortedBy { it.key }.forEachIndexed { index, (variant, url) ->
                try {
                    GoogleFontsApi.openStream(client, url).use { response ->
                        FontStorage.save(context, font.id, variant, response.body.byteStream())
                        installedVariants += variant
                    }
                } catch (e: Exception) {
                    Timber.w(e, "Could not download ${font.name} ${variant.key}")
                }
                onProgress((index + 1).toFloat() / wanted.size)
            }

            if (installedVariants.isEmpty()) {
                FontStorage.delete(context, font.id)
                return@withContext Result.failure(FontException(R.string.fonts_error_download, font.name))
            }

            val installed = font.copy(
                installed = true,
                variants = installedVariants.sorted(),
                sizeBytes = FontStorage.sizeOf(context, font.id),
                installedAt = System.currentTimeMillis(),
            )
            putInManifest(installed)
            FontManager.invalidate(font.id)
            onProgress(1f)
            Result.success(installed)
        } catch (e: Exception) {
            Timber.e(e, "Font download failed")
            Result.failure(e)
        }
    }

    /** Copies a user-picked `.ttf`/`.otf` into app storage after checking it is really a font. */
    suspend fun importFromUri(uri: Uri): Result<AppFont> = withContext(Dispatchers.IO) {
        try {
            val displayName = queryDisplayName(uri)
            val baseName = displayName.substringBeforeLast('.').trim().ifEmpty { "Custom font" }
            val id = baseName.toFontId()

            if (FontStorage.isInstalled(context, id)) {
                return@withContext Result.failure(FontException(R.string.fonts_error_already_installed, baseName))
            }

            val file = context.contentResolver.openInputStream(uri)?.use { input ->
                FontStorage.save(context, id, FontVariant.Regular, input)
            } ?: return@withContext Result.failure(FontException(R.string.fonts_error_unreadable))

            if (!isFontFile(file)) {
                file.delete()
                return@withContext Result.failure(FontException(R.string.fonts_error_not_a_font, displayName))
            }

            val font = AppFont(
                id = id,
                name = baseName,
                provider = FontProvider.USER_UPLOAD,
                variants = listOf(FontVariant.Regular),
                installed = true,
                sizeBytes = file.length(),
                installedAt = System.currentTimeMillis(),
            )
            putInManifest(font)
            FontManager.invalidate(id)
            Result.success(font)
        } catch (e: Exception) {
            Timber.e(e, "Font import failed")
            Result.failure(e)
        }
    }

    suspend fun delete(fontId: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!FontStorage.delete(context, fontId)) {
            return@withContext Result.failure(FontException(R.string.fonts_error_not_installed))
        }
        removeFromManifest(fontId)
        FontManager.invalidate(fontId)
        Result.success(Unit)
    }

    /**
     * TrueType/OpenType sfnt magic. Cheap and reliable way to reject a mis-picked file before it
     * ever reaches the text renderer.
     */
    private fun isFontFile(file: File): Boolean = try {
        file.inputStream().use { input ->
            val header = ByteArray(4)
            if (input.read(header) != 4) {
                false
            } else {
                val tag = String(header, Charsets.ISO_8859_1)
                tag == "OTTO" || tag == "true" || tag == "ttcf" ||
                    (header[0] == 0x00.toByte() && header[1] == 0x01.toByte() &&
                        header[2] == 0x00.toByte() && header[3] == 0x00.toByte())
            }
        }
    } catch (e: Exception) {
        false
    }

    private fun queryDisplayName(uri: Uri): String {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) {
                    cursor.getString(index)?.takeIf { it.isNotBlank() }?.let { return it }
                }
            }
        return uri.lastPathSegment?.substringAfterLast('/') ?: "Custom font"
    }

    // ---- manifest -------------------------------------------------------------------------

    private data class ManifestEntry(
        val name: String,
        val provider: FontProvider,
        val category: String?,
        val installedAt: Long,
    )

    private fun readManifest(): Map<String, ManifestEntry> = try {
        val file = FontStorage.manifestFile(context)
        if (!file.exists()) {
            emptyMap()
        } else {
            val array = JSONArray(file.readText())
            buildMap {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val id = item.optString("id").takeIf { it.isNotBlank() } ?: continue
                    put(
                        id,
                        ManifestEntry(
                            name = item.optString("name", id),
                            provider = runCatching {
                                FontProvider.valueOf(item.optString("provider"))
                            }.getOrDefault(FontProvider.USER_UPLOAD),
                            category = item.optString("category").takeIf { it.isNotBlank() },
                            installedAt = item.optLong("installedAt", 0L),
                        ),
                    )
                }
            }
        }
    } catch (e: Exception) {
        Timber.w(e, "Could not read font manifest")
        emptyMap()
    }

    @Synchronized
    private fun putInManifest(font: AppFont) {
        val entries = readManifest().toMutableMap()
        entries[font.id] = ManifestEntry(font.name, font.provider, font.category, font.installedAt)
        writeManifest(entries)
    }

    @Synchronized
    private fun removeFromManifest(fontId: String) {
        val entries = readManifest().toMutableMap()
        if (entries.remove(fontId) != null) writeManifest(entries)
    }

    private fun writeManifest(entries: Map<String, ManifestEntry>) {
        try {
            val array = JSONArray()
            entries.forEach { (id, entry) ->
                array.put(
                    JSONObject().apply {
                        put("id", id)
                        put("name", entry.name)
                        put("provider", entry.provider.name)
                        put("category", entry.category ?: JSONObject.NULL)
                        put("installedAt", entry.installedAt)
                    },
                )
            }
            FontStorage.manifestFile(context).writeText(array.toString())
        } catch (e: Exception) {
            Timber.w(e, "Could not write font manifest")
        }
    }

    // ---- catalog cache --------------------------------------------------------------------

    private fun readCachedCatalog(): List<AppFont>? = try {
        val file = FontStorage.catalogCacheFile(context)
        if (!file.exists() || System.currentTimeMillis() - file.lastModified() > CATALOG_TTL_MS) {
            null
        } else {
            val array = JSONArray(file.readText())
            (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                val name = item.optString("name").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val variantsArray = item.optJSONArray("variants")
                val urlsObject = item.optJSONObject("urls")
                AppFont(
                    id = item.optString("id", name.toFontId()),
                    name = name,
                    provider = FontProvider.GOOGLE_FONTS,
                    category = item.optString("category").takeIf { it.isNotBlank() },
                    variants = (0 until (variantsArray?.length() ?: 0))
                        .mapNotNull { FontVariant.fromKey(variantsArray!!.optString(it)) }
                        .ifEmpty { listOf(FontVariant.Regular) },
                    urls = buildMap {
                        urlsObject?.keys()?.forEach { key ->
                            urlsObject.optString(key).takeIf { it.isNotBlank() }?.let { put(key, it) }
                        }
                    },
                )
            }.takeIf { it.isNotEmpty() }
        }
    } catch (e: Exception) {
        Timber.w(e, "Could not read cached font catalog")
        null
    }

    private fun writeCachedCatalog(fonts: List<AppFont>) {
        try {
            val array = JSONArray()
            fonts.forEach { font ->
                array.put(
                    JSONObject().apply {
                        put("id", font.id)
                        put("name", font.name)
                        put("category", font.category ?: JSONObject.NULL)
                        put("variants", JSONArray(font.variants.map { it.key }))
                        put("urls", JSONObject(font.urls))
                    },
                )
            }
            FontStorage.catalogCacheFile(context).writeText(array.toString())
        } catch (e: Exception) {
            Timber.w(e, "Could not cache font catalog")
        }
    }

    private companion object {
        const val CATALOG_TTL_MS = 24L * 60 * 60 * 1000
    }
}
