/*
  Copyright 2024 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.example.compose
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily

val primaryLight = Color(0xFF6D5E0F)
val onPrimaryLight = Color(0xFFFFFFFF)
val primaryContainerLight = Color(0xFFF8E287)
val onPrimaryContainerLight = Color(0xFF221B00)
val secondaryLight = Color(0xFF665E40)
val onSecondaryLight = Color(0xFFFFFFFF)
val secondaryContainerLight = Color(0xFFEEE2BC)
val onSecondaryContainerLight = Color(0xFF211B04)
val tertiaryLight = Color(0xFF43664E)
val onTertiaryLight = Color(0xFFFFFFFF)
val tertiaryContainerLight = Color(0xFFC5ECCE)
val onTertiaryContainerLight = Color(0xFF00210F)
val errorLight = Color(0xFFBA1A1A)
val onErrorLight = Color(0xFFFFFFFF)
val errorContainerLight = Color(0xFFFFDAD6)
val onErrorContainerLight = Color(0xFF410002)
val backgroundLight = Color(0xFFFFF9EE)
val onBackgroundLight = Color(0xFF1E1B13)
val surfaceLight = Color(0xFFFFF9EE)
val onSurfaceLight = Color(0xFF1E1B13)
val surfaceVariantLight = Color(0xFFEAE2D0)
val onSurfaceVariantLight = Color(0xFF4B4739)
val outlineLight = Color(0xFF7C7767)
val outlineVariantLight = Color(0xFFCDC6B4)
val scrimLight = Color(0xFF000000)
val inverseSurfaceLight = Color(0xFF333027)
val inverseOnSurfaceLight = Color(0xFFF7F0E2)
val inversePrimaryLight = Color(0xFFDBC66E)
val surfaceDimLight = Color(0xFFE0D9CC)
val surfaceBrightLight = Color(0xFFFFF9EE)
val surfaceContainerLowestLight = Color(0xFFFFFFFF)
val surfaceContainerLowLight = Color(0xFFFAF3E5)
val surfaceContainerLight = Color(0xFFF4EDDF)
val surfaceContainerHighLight = Color(0xFFEEE8DA)
val surfaceContainerHighestLight = Color(0xFFE8E2D4)

val primaryDark = Color(0xFFDBC66E)
val onPrimaryDark = Color(0xFF3A3000)
val primaryContainerDark = Color(0xFF534600)
val onPrimaryContainerDark = Color(0xFFF8E287)
val secondaryDark = Color(0xFFD1C6A1)
val onSecondaryDark = Color(0xFF363016)
val secondaryContainerDark = Color(0xFF4E472A)
val onSecondaryContainerDark = Color(0xFFEEE2BC)
val tertiaryDark = Color(0xFFA9D0B3)
val onTertiaryDark = Color(0xFF143723)
val tertiaryContainerDark = Color(0xFF2C4E38)
val onTertiaryContainerDark = Color(0xFFC5ECCE)
val errorDark = Color(0xFFFFB4AB)
val onErrorDark = Color(0xFF690005)
val errorContainerDark = Color(0xFF93000A)
val onErrorContainerDark = Color(0xFFFFDAD6)
val backgroundDark = Color(0xFF15130B)
val onBackgroundDark = Color(0xFFE8E2D4)
val surfaceDark = Color(0xFF15130B)
val onSurfaceDark = Color(0xFFE8E2D4)
val surfaceVariantDark = Color(0xFF4B4739)
val onSurfaceVariantDark = Color(0xFFCDC6B4)
val outlineDark = Color(0xFF969080)
val outlineVariantDark = Color(0xFF4B4739)
val scrimDark = Color(0xFF000000)
val inverseSurfaceDark = Color(0xFFE8E2D4)
val inverseOnSurfaceDark = Color(0xFF333027)
val inversePrimaryDark = Color(0xFF6D5E0F)
val surfaceDimDark = Color(0xFF15130B)
val surfaceBrightDark = Color(0xFF3C3930)
val surfaceContainerLowestDark = Color(0xFF100E07)
val surfaceContainerLowDark = Color(0xFF1E1B13)
val surfaceContainerDark = Color(0xFF222017)
val surfaceContainerHighDark = Color(0xFF2D2A21)
val surfaceContainerHighestDark = Color(0xFF38352B)

private val lightScheme = lightColorScheme(
    primary = primaryLight,
    onPrimary = onPrimaryLight,
    primaryContainer = primaryContainerLight,
    onPrimaryContainer = onPrimaryContainerLight,
    secondary = secondaryLight,
    onSecondary = onSecondaryLight,
    secondaryContainer = secondaryContainerLight,
    onSecondaryContainer = onSecondaryContainerLight,
    tertiary = tertiaryLight,
    onTertiary = onTertiaryLight,
    tertiaryContainer = tertiaryContainerLight,
    onTertiaryContainer = onTertiaryContainerLight,
    error = errorLight,
    onError = onErrorLight,
    errorContainer = errorContainerLight,
    onErrorContainer = onErrorContainerLight,
    background = backgroundLight,
    onBackground = onBackgroundLight,
    surface = surfaceLight,
    onSurface = onSurfaceLight,
    surfaceVariant = surfaceVariantLight,
    onSurfaceVariant = onSurfaceVariantLight,
    outline = outlineLight,
    outlineVariant = outlineVariantLight,
    scrim = scrimLight,
    inverseSurface = inverseSurfaceLight,
    inverseOnSurface = inverseOnSurfaceLight,
    inversePrimary = inversePrimaryLight,
    surfaceDim = surfaceDimLight,
    surfaceBright = surfaceBrightLight,
    surfaceContainerLowest = surfaceContainerLowestLight,
    surfaceContainerLow = surfaceContainerLowLight,
    surfaceContainer = surfaceContainerLight,
    surfaceContainerHigh = surfaceContainerHighLight,
    surfaceContainerHighest = surfaceContainerHighestLight,
)

private val darkScheme = darkColorScheme(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    primaryContainer = primaryContainerDark,
    onPrimaryContainer = onPrimaryContainerDark,
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    secondaryContainer = secondaryContainerDark,
    onSecondaryContainer = onSecondaryContainerDark,
    tertiary = tertiaryDark,
    onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark,
    onTertiaryContainer = onTertiaryContainerDark,
    error = errorDark,
    onError = onErrorDark,
    errorContainer = errorContainerDark,
    onErrorContainer = onErrorContainerDark,
    background = backgroundDark,
    onBackground = onBackgroundDark,
    surface = surfaceDark,
    onSurface = onSurfaceDark,
    surfaceVariant = surfaceVariantDark,
    onSurfaceVariant = onSurfaceVariantDark,
    outline = outlineDark,
    outlineVariant = outlineVariantDark,
    scrim = scrimDark,
    inverseSurface = inverseSurfaceDark,
    inverseOnSurface = inverseOnSurfaceDark,
    inversePrimary = inversePrimaryDark,
    surfaceDim = surfaceDimDark,
    surfaceBright = surfaceBrightDark,
    surfaceContainerLowest = surfaceContainerLowestDark,
    surfaceContainerLow = surfaceContainerLowDark,
    surfaceContainer = surfaceContainerDark,
    surfaceContainerHigh = surfaceContainerHighDark,
    surfaceContainerHighest = surfaceContainerHighestDark,
)

// Default Material 3 typography values
val baseline = Typography()

val AppTypography = Typography(
    displayLarge = baseline.displayLarge.copy(fontFamily = FontFamily.SansSerif),
    displayMedium = baseline.displayMedium.copy(fontFamily = FontFamily.SansSerif),
    displaySmall = baseline.displaySmall.copy(fontFamily = FontFamily.SansSerif),
    headlineLarge = baseline.headlineLarge.copy(fontFamily = FontFamily.SansSerif),
    headlineMedium = baseline.headlineMedium.copy(fontFamily = FontFamily.SansSerif),
    headlineSmall = baseline.headlineSmall.copy(fontFamily = FontFamily.SansSerif),
    titleLarge = baseline.titleLarge.copy(fontFamily = FontFamily.SansSerif),
    titleMedium = baseline.titleMedium.copy(fontFamily = FontFamily.SansSerif),
    titleSmall = baseline.titleSmall.copy(fontFamily = FontFamily.SansSerif),
    bodyLarge = baseline.bodyLarge.copy(fontFamily = FontFamily.SansSerif),
    bodyMedium = baseline.bodyMedium.copy(fontFamily = FontFamily.SansSerif),
    bodySmall = baseline.bodySmall.copy(fontFamily = FontFamily.SansSerif),
    labelLarge = baseline.labelLarge.copy(fontFamily = FontFamily.SansSerif),
    labelMedium = baseline.labelMedium.copy(fontFamily = FontFamily.SansSerif),
    labelSmall = baseline.labelSmall.copy(fontFamily = FontFamily.SansSerif),
)

@Composable
fun TestTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        useDarkTheme -> darkScheme
        else -> lightScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
