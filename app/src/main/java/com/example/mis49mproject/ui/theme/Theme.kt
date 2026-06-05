package com.example.mis49mproject.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val FormwardDarkColorScheme = darkColorScheme(
    primary = FormwardPrimary,
    onPrimary = Color(0xFF001018),

    secondary = FormwardAccent,
    onSecondary = Color.White,

    tertiary = FormwardAccentRed,
    onTertiary = Color.White,

    background = FormwardBackground,
    onBackground = FormwardText,

    surface = FormwardSurface,
    onSurface = FormwardText,

    surfaceVariant = FormwardSurfaceSoft,
    onSurfaceVariant = FormwardTextMuted,

    primaryContainer = FormwardPrimaryDark,
    onPrimaryContainer = FormwardText,

    secondaryContainer = Color(0xFF7C2D12),
    onSecondaryContainer = FormwardText,

    error = FormwardError,
    onError = Color.White,

    outline = Color(0xFF334155),
    outlineVariant = Color(0xFF1E293B)
)

@Composable
fun MIS49MProjectTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = FormwardDarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as Activity).window

        window.statusBarColor = android.graphics.Color.parseColor("#05070D")
        window.navigationBarColor = android.graphics.Color.parseColor("#05070D")

        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}