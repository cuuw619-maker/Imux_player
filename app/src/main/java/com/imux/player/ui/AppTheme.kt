package com.imux.player.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.imux.player.data.AppearanceMode

@Composable
fun ImuxTheme(
    appearance: AppearanceMode,
    dynamicColor: Boolean,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (appearance) {
        AppearanceMode.Dark, AppearanceMode.Amoled -> true
        AppearanceMode.Light -> false
        AppearanceMode.System -> systemDark
    }
    val scheme = if (dynamicColor && Build.VERSION.SDK_INT >= 31) {
        if (dark) dynamicDarkColorScheme(androidx.compose.ui.platform.LocalContext.current)
        else dynamicLightColorScheme(androidx.compose.ui.platform.LocalContext.current)
    } else if (dark) darkColorScheme() else lightColorScheme()

    val finalScheme = if (appearance == AppearanceMode.Amoled && dark) {
        scheme.copy(background = Color.Black, surface = Color.Black, surfaceContainer = Color.Black)
    } else scheme

    MaterialTheme(
        colorScheme = finalScheme,
        typography = Typography(),
        shapes = Shapes(),
        content = content
    )
}
