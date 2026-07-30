

package iad1tya.echo.music.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp








val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal, 
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp 
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

/**
 * [AppTypography] with every text role rebound to [fontFamily].
 *
 * Passing `null` (the default, "system font") returns [AppTypography] untouched, so the app pays
 * nothing when no custom font is installed.
 */
fun appTypography(fontFamily: FontFamily?): Typography {
    if (fontFamily == null) return AppTypography

    return AppTypography.copy(
        displayLarge = AppTypography.displayLarge.copy(fontFamily = fontFamily),
        displayMedium = AppTypography.displayMedium.copy(fontFamily = fontFamily),
        displaySmall = AppTypography.displaySmall.copy(fontFamily = fontFamily),
        headlineLarge = AppTypography.headlineLarge.copy(fontFamily = fontFamily),
        headlineMedium = AppTypography.headlineMedium.copy(fontFamily = fontFamily),
        headlineSmall = AppTypography.headlineSmall.copy(fontFamily = fontFamily),
        titleLarge = AppTypography.titleLarge.copy(fontFamily = fontFamily),
        titleMedium = AppTypography.titleMedium.copy(fontFamily = fontFamily),
        titleSmall = AppTypography.titleSmall.copy(fontFamily = fontFamily),
        bodyLarge = AppTypography.bodyLarge.copy(fontFamily = fontFamily),
        bodyMedium = AppTypography.bodyMedium.copy(fontFamily = fontFamily),
        bodySmall = AppTypography.bodySmall.copy(fontFamily = fontFamily),
        labelLarge = AppTypography.labelLarge.copy(fontFamily = fontFamily),
        labelMedium = AppTypography.labelMedium.copy(fontFamily = fontFamily),
        labelSmall = AppTypography.labelSmall.copy(fontFamily = fontFamily),
    )
}
