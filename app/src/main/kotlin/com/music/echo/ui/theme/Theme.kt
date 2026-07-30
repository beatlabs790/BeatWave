

package iad1tya.echo.music.ui.theme

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.palette.graphics.Palette
import iad1tya.echo.music.fonts.LocalLyricsFontFamily
import iad1tya.echo.music.fonts.LocalPlayerFontFamily
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import com.materialkolor.score.Score

val DefaultThemeColor = Color(0xFFED5564)

@Composable
fun echomusicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    pureBlack: Boolean = false,
    themeColor: Color = DefaultThemeColor,
    fontFamily: FontFamily? = null,
    lyricsFontFamily: FontFamily? = null,
    playerFontFamily: FontFamily? = null,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    
    val useSystemDynamicColor = (themeColor == DefaultThemeColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)

    
    val baseColorScheme = if (useSystemDynamicColor) {
        
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        
        rememberDynamicColorScheme(
            seedColor = themeColor, 
            isDark = darkTheme,
            specVersion = ColorSpec.SpecVersion.SPEC_2025,
            style = PaletteStyle.TonalSpot 
        )
    }

    
    val colorScheme = remember(baseColorScheme, pureBlack, darkTheme) {
        if (darkTheme && pureBlack) {
            baseColorScheme.pureBlack(true)
        } else {
            baseColorScheme
        }
    }

    val typography = remember(fontFamily) { appTypography(fontFamily) }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
    ) {
        // Lyrics and the player build text styles from scratch, so Material's typography cannot
        // reach them; they read these instead.
        CompositionLocalProvider(
            LocalLyricsFontFamily provides lyricsFontFamily,
            LocalPlayerFontFamily provides playerFontFamily,
            content = content,
        )
    }
}

fun Bitmap.extractThemeColor(): Color {
    val colorsToPopulation = Palette.from(this)
        .maximumColorCount(8)
        .generate()
        .swatches
        .associate { it.rgb to it.population }
    val rankedColors = Score.score(colorsToPopulation)
    return Color(rankedColors.first())
}

fun Bitmap.extractGradientColors(): List<Color> {
    val extractedColors = Palette.from(this)
        .maximumColorCount(64)
        .generate()
        .swatches
        .associate { it.rgb to it.population }

    val orderedColors = Score.score(extractedColors, 2, 0xff4285f4.toInt(), true)
        .sortedByDescending { Color(it).luminance() }

    return if (orderedColors.size >= 2)
        listOf(Color(orderedColors[0]), Color(orderedColors[1]))
    else
        listOf(Color(0xFF595959), Color(0xFF0D0D0D))
}

fun ColorScheme.pureBlack(apply: Boolean) =
    if (apply) copy(
        surface = Color.Black,
        background = Color.Black
    ) else this

val ColorSaver = object : Saver<Color, Int> {
    override fun restore(value: Int): Color = Color(value)
    override fun SaverScope.save(value: Color): Int = value.toArgb()
}
