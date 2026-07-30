package iad1tya.echo.music.fonts

import android.content.Context
import java.io.File
import java.io.InputStream

/**
 * Owns everything under `filesDir/custom_fonts`.
 *
 * Internal app storage is used on purpose: no runtime permission is needed on any supported API
 * level, and the whole directory disappears when the app is uninstalled.
 *
 * Layout:
 * ```
 * files/custom_fonts/
 *   ├── installed.json        <- manifest (names, providers, variants, timestamps)
 *   ├── catalog.json          <- cached Google Fonts catalog
 *   ├── noto-sans-400.ttf
 *   ├── noto-sans-700.ttf
 *   └── my-font-400.ttf
 * ```
 */
object FontStorage {
    private const val DIR_NAME = "custom_fonts"
    private const val MANIFEST_NAME = "installed.json"
    private const val CATALOG_NAME = "catalog.json"

    private val FONT_EXTENSIONS = setOf("ttf", "otf")

    fun fontsDir(context: Context): File =
        File(context.filesDir, DIR_NAME).apply { if (!exists()) mkdirs() }

    fun manifestFile(context: Context): File = File(fontsDir(context), MANIFEST_NAME)

    fun catalogCacheFile(context: Context): File = File(fontsDir(context), CATALOG_NAME)

    fun fontFile(context: Context, fontId: String, variant: FontVariant): File =
        File(fontsDir(context), "$fontId-${variant.key}.ttf")

    /** Every installed face of [fontId], keyed by [FontVariant]. */
    fun variantFiles(context: Context, fontId: String): Map<FontVariant, File> =
        fontFiles(context)
            .mapNotNull { file ->
                val name = file.nameWithoutExtension
                if (!name.startsWith("$fontId-")) return@mapNotNull null
                val variant = FontVariant.fromKey(name.removePrefix("$fontId-")) ?: return@mapNotNull null
                variant to file
            }
            .toMap()

    /** Ids of every family with at least one face on disk. */
    fun installedFontIds(context: Context): Set<String> =
        fontFiles(context)
            .mapNotNull { file ->
                val name = file.nameWithoutExtension
                val separator = name.lastIndexOf('-')
                if (separator <= 0) return@mapNotNull null
                if (FontVariant.fromKey(name.substring(separator + 1)) == null) return@mapNotNull null
                name.substring(0, separator)
            }
            .toSet()

    fun isInstalled(context: Context, fontId: String): Boolean =
        fontId.isNotEmpty() && variantFiles(context, fontId).isNotEmpty()

    fun sizeOf(context: Context, fontId: String): Long =
        variantFiles(context, fontId).values.sumOf { it.length() }

    fun totalSize(context: Context): Long = fontFiles(context).sumOf { it.length() }

    fun save(context: Context, fontId: String, variant: FontVariant, input: InputStream): File {
        val target = fontFile(context, fontId, variant)
        // Write to a temp file first so a failed download never leaves a half-written face behind.
        val temp = File(target.parentFile, "${target.name}.part")
        try {
            temp.outputStream().use { output -> input.copyTo(output) }
            if (target.exists()) target.delete()
            if (!temp.renameTo(target)) {
                temp.copyTo(target, overwrite = true)
                temp.delete()
            }
        } catch (e: Exception) {
            temp.delete()
            throw e
        }
        return target
    }

    fun delete(context: Context, fontId: String): Boolean {
        val files = variantFiles(context, fontId).values
        if (files.isEmpty()) return false
        return files.all { it.delete() }
    }

    private fun fontFiles(context: Context): List<File> =
        fontsDir(context).listFiles()
            ?.filter { it.isFile && it.extension.lowercase() in FONT_EXTENSIONS }
            ?: emptyList()
}
