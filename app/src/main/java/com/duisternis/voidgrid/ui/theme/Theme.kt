package com.duisternis.voidgrid.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Neutral80,
    secondary = Neutral40,
    tertiary = Neutral80,
    background = Color.Black,        // Preto OLED
    surface = Color.Black,           // Preto OLED
    onBackground = Color.White,
    onSurface = Color.White,
    onPrimary = Color.Black,
    onSecondary = Color.Black
)

private val LightColorScheme = lightColorScheme(
    primary = Neutral40,
    secondary = Neutral80,
    tertiary = Neutral40,
    background = Color.White,
    surface = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black
)

@Composable
fun ImageSearchTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // IMPORTANTE: Definimos false como padrão para que o Preto Puro não seja
    // sobrescrito pelas cores dinâmicas do Android.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Certifique-se de que o objeto Typography existe
        content = content
    )
}