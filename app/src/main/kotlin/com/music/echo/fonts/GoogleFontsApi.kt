package iad1tya.echo.music.fonts

import iad1tya.echo.music.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import timber.log.Timber

/**
 * Read-only access to the Google Fonts catalog and to the TTF files behind it.
 *
 * Two catalog sources are tried in order:
 *
 *  1. The Web Fonts Developer API, if [BuildConfig.GOOGLE_FONTS_API_KEY] was supplied at build
 *     time. It returns direct file urls, so no extra request is needed to download a family.
 *  2. `fonts.google.com/metadata/fonts`, the public endpoint the Google Fonts website itself
 *     uses. No key, no quota, but no file urls either — those come from [resolveDownloadUrls].
 *
 * If both fail (offline, blocked, endpoint changed) callers fall back to [FallbackFamilies] so the
 * browser is never empty.
 */
object GoogleFontsApi {
    private const val METADATA_URL = "https://fonts.google.com/metadata/fonts"
    private const val WEBFONTS_URL = "https://www.googleapis.com/webfonts/v1/webfonts"
    private const val CSS2_URL = "https://fonts.googleapis.com/css2"

    /**
     * Google serves WOFF2 to modern browsers and Android cannot load it, so we ask as an old
     * Android WebView, which gets plain TrueType.
     */
    private const val LEGACY_UA =
        "Mozilla/5.0 (Linux; U; Android 4.4; en-us; Nexus 5 Build/JSS15Q) " +
            "AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30"

    /** Anti-hijacking prefix Google puts in front of its internal JSON payloads. */
    private const val JSON_XSSI_PREFIX = ")]}'"

    private val apiKey: String
        get() = BuildConfig.GOOGLE_FONTS_API_KEY

    suspend fun fetchCatalog(client: OkHttpClient): List<AppFont> = withContext(Dispatchers.IO) {
        if (apiKey.isNotBlank()) {
            runCatching { fetchDeveloperApiCatalog(client) }
                .onFailure { Timber.w(it, "Google Fonts Developer API failed, falling back to metadata") }
                .getOrNull()
                ?.takeIf { it.isNotEmpty() }
                ?.let { return@withContext it }
        }

        runCatching { fetchMetadataCatalog(client) }
            .onFailure { Timber.w(it, "Google Fonts metadata endpoint failed, using bundled list") }
            .getOrNull()
            ?.takeIf { it.isNotEmpty() }
            ?: FallbackFamilies
    }

    /**
     * Resolves a direct TTF url per available face of [family].
     *
     * The css2 endpoint silently drops weights a family does not ship, so we can always ask for
     * the full [FontVariant.Desired] set and simply keep what comes back.
     */
    suspend fun resolveDownloadUrls(
        client: OkHttpClient,
        family: String,
    ): Map<FontVariant, String> = withContext(Dispatchers.IO) {
        val spec = FontVariant.Desired.sorted().joinToString(";") { variant ->
            "${if (variant.italic) 1 else 0},${variant.weight}"
        }
        val url = "$CSS2_URL?family=${family.replace(" ", "+")}:ital,wght@$spec&display=swap"

        val css = runCatching { get(client, url, LEGACY_UA) }.getOrNull()
            ?: runCatching {
                get(client, "$CSS2_URL?family=${family.replace(" ", "+")}", LEGACY_UA)
            }.getOrNull()
            ?: return@withContext emptyMap()

        parseCss(css)
    }

    /** Streams a font file straight into [FontStorage]. */
    fun openStream(client: OkHttpClient, url: String): okhttp3.Response {
        val response = client.newCall(
            Request.Builder().url(url).header("User-Agent", LEGACY_UA).build()
        ).execute()
        if (!response.isSuccessful) {
            response.close()
            throw IllegalStateException("HTTP ${response.code} for $url")
        }
        return response
    }

    private fun fetchDeveloperApiCatalog(client: OkHttpClient): List<AppFont> {
        val body = get(client, "$WEBFONTS_URL?sort=popularity&key=$apiKey", LEGACY_UA)
        val items = JSONObject(body).optJSONArray("items") ?: return emptyList()

        return (0 until items.length()).mapNotNull { index ->
            val item = items.optJSONObject(index) ?: return@mapNotNull null
            val family = item.optString("family").takeIf { it.isNotBlank() } ?: return@mapNotNull null

            val files = item.optJSONObject("files")
            val urls = buildMap {
                files?.keys()?.forEach { variantName ->
                    val variant = variantFromDeveloperApi(variantName) ?: return@forEach
                    val fileUrl = files.optString(variantName).takeIf { it.isNotBlank() } ?: return@forEach
                    // The API still hands out http urls for some families.
                    put(variant.key, fileUrl.replaceFirst("http://", "https://"))
                }
            }

            AppFont(
                id = family.toFontId(),
                name = family,
                provider = FontProvider.GOOGLE_FONTS,
                category = item.optString("category").takeIf { it.isNotBlank() },
                variants = urls.keys.mapNotNull { FontVariant.fromKey(it) }.sorted()
                    .ifEmpty { listOf(FontVariant.Regular) },
                urls = urls,
            )
        }
    }

    private fun fetchMetadataCatalog(client: OkHttpClient): List<AppFont> {
        val body = get(client, METADATA_URL, LEGACY_UA).removePrefix(JSON_XSSI_PREFIX).trimStart()
        val list = JSONObject(body).optJSONArray("familyMetadataList") ?: return emptyList()

        return (0 until list.length()).mapNotNull { index ->
            val item = list.optJSONObject(index) ?: return@mapNotNull null
            val family = item.optString("family").takeIf { it.isNotBlank() } ?: return@mapNotNull null

            val variants = item.optJSONObject("fonts")
                ?.keys()
                ?.asSequence()
                ?.mapNotNull { FontVariant.fromKey(it) }
                ?.sorted()
                ?.toList()
                .orEmpty()

            val popularity = item.optInt("popularity", Int.MAX_VALUE)

            popularity to AppFont(
                id = family.toFontId(),
                name = family,
                provider = FontProvider.GOOGLE_FONTS,
                category = item.optString("category").takeIf { it.isNotBlank() },
                variants = variants.ifEmpty { listOf(FontVariant.Regular) },
            )
        }
            // The endpoint answers in alphabetical order, which buries the families anyone is
            // actually looking for. Rank 1 is the most popular; anything unranked sinks.
            .sortedBy { it.first }
            .map { it.second }
    }

    /** `"regular"`, `"italic"`, `"700"`, `"700italic"` -> [FontVariant]. */
    private fun variantFromDeveloperApi(name: String): FontVariant? = when {
        name == "regular" -> FontVariant(400, false)
        name == "italic" -> FontVariant(400, true)
        name.endsWith("italic") -> name.removeSuffix("italic").toIntOrNull()?.let { FontVariant(it, true) }
        else -> name.toIntOrNull()?.let { FontVariant(it, false) }
    }

    // Braces and parentheses stay escaped everywhere, including inside character classes:
    // Android compiles patterns with ICU, which rejects the bare forms the JVM accepts.
    private val FONT_FACE = Regex("@font-face\\s*\\{([^\\}]*)\\}", RegexOption.IGNORE_CASE)
    private val STYLE = Regex("font-style\\s*:\\s*([a-z]+)", RegexOption.IGNORE_CASE)
    private val WEIGHT = Regex("font-weight\\s*:\\s*(\\d+)", RegexOption.IGNORE_CASE)
    private val SRC = Regex("url\\(([^\\)]+\\.ttf)\\)", RegexOption.IGNORE_CASE)

    private fun parseCss(css: String): Map<FontVariant, String> = buildMap {
        FONT_FACE.findAll(css).forEach { match ->
            val block = match.groupValues[1]
            val url = SRC.find(block)?.groupValues?.get(1)?.trim('"', '\'', ' ') ?: return@forEach
            val weight = WEIGHT.find(block)?.groupValues?.get(1)?.toIntOrNull() ?: 400
            val italic = STYLE.find(block)?.groupValues?.get(1)?.equals("italic", ignoreCase = true) == true
            // css2 may emit one block per unicode subset; the first is good enough for UI text.
            putIfAbsent(FontVariant(weight, italic), url)
        }
    }

    private fun get(client: OkHttpClient, url: String, userAgent: String): String {
        client.newCall(Request.Builder().url(url).header("User-Agent", userAgent).build())
            .execute()
            .use { response ->
                if (!response.isSuccessful) throw IllegalStateException("HTTP ${response.code} for $url")
                return response.body.string()
            }
    }

    /**
     * Last-resort catalog so the browser still works when both endpoints are unreachable.
     * Variants are left empty on purpose — css2 tells us what actually exists at download time.
     */
    val FallbackFamilies: List<AppFont> = listOf(
        "Roboto" to "sans-serif",
        "Open Sans" to "sans-serif",
        "Noto Sans" to "sans-serif",
        "Montserrat" to "sans-serif",
        "Lato" to "sans-serif",
        "Poppins" to "sans-serif",
        "Inter" to "sans-serif",
        "Nunito" to "sans-serif",
        "Nunito Sans" to "sans-serif",
        "Raleway" to "sans-serif",
        "Ubuntu" to "sans-serif",
        "Rubik" to "sans-serif",
        "Work Sans" to "sans-serif",
        "Manrope" to "sans-serif",
        "Outfit" to "sans-serif",
        "DM Sans" to "sans-serif",
        "Figtree" to "sans-serif",
        "Plus Jakarta Sans" to "sans-serif",
        "Quicksand" to "sans-serif",
        "Josefin Sans" to "sans-serif",
        "Oswald" to "sans-serif",
        "Barlow" to "sans-serif",
        "Karla" to "sans-serif",
        "Mulish" to "sans-serif",
        "Merriweather" to "serif",
        "Playfair Display" to "serif",
        "Lora" to "serif",
        "PT Serif" to "serif",
        "Noto Serif" to "serif",
        "Bitter" to "serif",
        "Cormorant Garamond" to "serif",
        "Libre Baskerville" to "serif",
        "Crimson Text" to "serif",
        "JetBrains Mono" to "monospace",
        "Fira Code" to "monospace",
        "Source Code Pro" to "monospace",
        "IBM Plex Mono" to "monospace",
        "Space Mono" to "monospace",
        "Pacifico" to "handwriting",
        "Dancing Script" to "handwriting",
        "Caveat" to "handwriting",
        "Lobster" to "display",
        "Bebas Neue" to "display",
        "Righteous" to "display",
    ).map { (family, category) ->
        AppFont(
            id = family.toFontId(),
            name = family,
            provider = FontProvider.GOOGLE_FONTS,
            category = category,
        )
    }
}
