package com.alpha.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    // AlphaPurple (0xFF4724BB) → Cobalt60 (0xFF5B8DEF) — warmer purple to electric cobalt
    primary              = Cobalt60,
    // NEW — was unset (defaulted to Material baseline black)
    onPrimary            = Color(0xFF000000),
    // NEW — was unset
    primaryContainer     = Cobalt20,
    // NEW — was unset
    onPrimaryContainer   = Cobalt80,

    // PurpleGrey80 (0xFFCCC2DC) → Slate80 (0xFFB0BEC5) — warm grey-purple to cool blue-grey
    secondary            = Slate80,
    // NEW — was unset
    onSecondary          = Color(0xFF0D0F14),
    // NEW — was unset
    secondaryContainer   = Slate20,
    // NEW — was unset
    onSecondaryContainer = Slate60,

    // Pink80 (0xFFEFB8C8) → Amber80 (0xFFFFD180) — pink accent to HUD amber
    tertiary             = Amber80,
    // NEW — was unset
    onTertiary           = Color(0xFF0D0F14),
    // NEW — was unset
    tertiaryContainer    = Amber20,
    // NEW — was unset
    onTertiaryContainer  = Amber60,

    // NEW — was unset (defaulted to Material baseline red)
    error                = SignalRed80,
    // NEW — was unset
    onError              = Color(0xFF000000),
    // NEW — was unset
    errorContainer       = Color(0xFF4A0A0A),
    // NEW — was unset
    onErrorContainer     = Color(0xFFFF8A80),

    // DarkBg (0xFF1C1C1C) → DarkBg (0xFF0D0F14) — warm near-black to cool-undertone near-black
    background           = DarkBg,
    // Color.White → Color(0xFFE2E6EF) — pure white to cool off-white (less harsh on dark bg)
    onBackground         = Color(0xFFE2E6EF),
    // DarkCard (0xFF0C0C0C) → DarkSurface (0xFF13161E) — neutral black to cool-tinted surface
    surface              = DarkSurface,
    // Color.White → Color(0xFFE2E6EF) — pure white to cool off-white
    onSurface            = Color(0xFFE2E6EF),
    // NEW — was unset
    surfaceVariant       = DarkSurface2,
    // NEW — was unset
    onSurfaceVariant     = Slate80,
    // NEW — was unset
    outline              = DarkOutline,
)

private val LightColorScheme = lightColorScheme(
    // AlphaPurple (0xFF4724BB) → Cobalt40 (0xFF1A56DB) — same hue family, cobalt shift
    primary              = Cobalt40,
    // UNCHANGED — Color.White
    onPrimary            = Color(0xFFFFFFFF),
    // NEW — was unset
    primaryContainer     = Color(0xFFD6E4FF),
    // NEW — was unset
    onPrimaryContainer   = Cobalt20,

    // PurpleGrey40 (0xFF625b71) → Slate40 (0xFF546E7A) — warm muted purple to cool slate
    secondary            = Slate40,
    // NEW — was unset
    onSecondary          = Color(0xFFFFFFFF),
    // NEW — was unset
    secondaryContainer   = LightSurface2,
    // NEW — was unset
    onSecondaryContainer = Slate20,

    // Pink40 (0xFF7D5260) → Amber40 (0xFFFF8F00) — pink accent to HUD amber
    tertiary             = Amber40,
    // NEW — was unset
    onTertiary           = Color(0xFFFFFFFF),
    // NEW — was unset
    tertiaryContainer    = Color(0xFFFFE0B2),
    // NEW — was unset
    onTertiaryContainer  = Amber20,

    // NEW — was unset (defaulted to Material baseline red)
    error                = SignalRed40,
    // NEW — was unset
    onError              = Color(0xFFFFFFFF),
    // NEW — was unset
    errorContainer       = Color(0xFFFFDAD6),
    // NEW — was unset
    onErrorContainer     = Color(0xFF7A0000),

    // Color(0xFFFFFBFE) → LightBg (0xFFF2F4F8) — warm off-white to cool off-white
    background           = LightBg,
    // Color(0xFF1C1B1F) → Color(0xFF0D0F14) — warm near-black to cool-undertone near-black
    onBackground         = Color(0xFF0D0F14),
    // Color.White → LightSurface (0xFFFFFFFF) — same value, now named for consistency
    surface              = LightSurface,
    // Color(0xFF1C1B1F) → Color(0xFF0D0F14) — warm near-black to cool-undertone near-black
    onSurface            = Color(0xFF0D0F14),
    // NEW — was unset
    surfaceVariant       = LightSurface2,
    // NEW — was unset
    onSurfaceVariant     = Slate40,
    // NEW — was unset
    outline              = LightOutline,
)

@Composable
fun AlphaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}