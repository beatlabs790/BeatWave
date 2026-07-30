package iad1tya.echo.music.fonts

/**
 * Where a font came from.
 *
 * [SYSTEM] is the implicit default (no custom font applied) and is never stored on disk.
 */
enum class FontProvider {
    SYSTEM,
    GOOGLE_FONTS,
    USER_UPLOAD,
}

/**
 * A single downloadable/installable face of a family, e.g. 700 italic.
 *
 * [key] is the on-disk and API-facing identifier: "400", "500", "700", "400i", "700i".
 */
data class FontVariant(
    val weight: Int,
    val italic: Boolean = false,
) : Comparable<FontVariant> {
    val key: String get() = if (italic) "${weight}i" else weight.toString()

    override fun compareTo(other: FontVariant): Int =
        compareValuesBy(this, other, { it.italic }, { it.weight })

    companion object {
        val Regular = FontVariant(400, false)

        fun fromKey(key: String): FontVariant? {
            val trimmed = key.trim()
            val italic = trimmed.endsWith("i", ignoreCase = true)
            val weight = (if (italic) trimmed.dropLast(1) else trimmed).toIntOrNull() ?: return null
            if (weight !in 1..1000) return null
            return FontVariant(weight, italic)
        }

        /**
         * The faces we try to install for a family. Material 3 typography only ever asks for
         * Normal (400) and Medium (500); 600/700 keep bold text from being synthesised, and the
         * two italics cover the few places the app emphasises text.
         */
        val Desired = listOf(
            FontVariant(400, false),
            FontVariant(500, false),
            FontVariant(600, false),
            FontVariant(700, false),
            FontVariant(400, true),
            FontVariant(700, true),
        )
    }
}

/**
 * A font family as shown in the UI, whether it is remote (browsable) or already installed.
 *
 * [urls] maps [FontVariant.key] to a direct TTF url and is only populated for catalog entries
 * that came from the Google Fonts Developer API; for the key-free metadata catalog the urls are
 * resolved lazily at download time (see [GoogleFontsApi.resolveDownloadUrls]).
 */
data class AppFont(
    val id: String,
    val name: String,
    val provider: FontProvider,
    val category: String? = null,
    val variants: List<FontVariant> = listOf(FontVariant.Regular),
    val urls: Map<String, String> = emptyMap(),
    val installed: Boolean = false,
    val sizeBytes: Long = 0L,
    val installedAt: Long = 0L,
) {
    companion object {
        /** Sentinel id meaning "use the system font". */
        const val SYSTEM_ID = ""

        /**
         * Sentinel id meaning "whatever the app font is". Only valid for the secondary targets;
         * it can never collide with a real id because [toFontId] strips underscores.
         */
        const val INHERIT_ID = "__inherit__"
    }
}

/** Stable, filesystem-safe id derived from a family name ("Noto Sans" -> "noto-sans"). */
fun String.toFontId(): String =
    lowercase()
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
        .ifEmpty { "font-${hashCode().toUInt().toString(16)}" }
