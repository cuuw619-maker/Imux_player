package com.imux.player.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.imux.player.data.AppearanceMode

@Composable
fun ImuxTheme(
    appearance: AppearanceMode,
    dynamicColor: Boolean,
    expressive: Boolean = true,
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
        motionScheme = if (expressive) MotionScheme.expressive() else MotionScheme.standard(),
        typography = Typography().run { copy(displayLarge = displayLarge.copy(fontWeight = FontWeight.Bold), displayMedium = displayMedium.copy(fontWeight = FontWeight.Bold), headlineLarge = headlineLarge.copy(fontWeight = FontWeight.Bold), headlineMedium = headlineMedium.copy(fontWeight = FontWeight.SemiBold), titleLarge = titleLarge.copy(fontWeight = FontWeight.SemiBold)) },
        shapes = Shapes(extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(10.dp), small = androidx.compose.foundation.shape.RoundedCornerShape(14.dp), medium = androidx.compose.foundation.shape.RoundedCornerShape(20.dp), large = androidx.compose.foundation.shape.RoundedCornerShape(28.dp), extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(36.dp)),
        content = content
    )
}
