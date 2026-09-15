package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/**
 * Tema principal para AudioStudio.
 * Utiliza un esquema visual enfocado en modo oscuro de alta fidelidad,
 * adecuado para edición y conversión de audio en dispositivos móviles.
 */
private val StudioDarkColorScheme = darkColorScheme(
    primary = AudioCyan,
    onPrimary = Color(0xFF00363D),
    primaryContainer = Color(0xFF004F58),
    onPrimaryContainer = Color(0xFF97F0FF),

    secondary = AudioOrange,
    onSecondary = Color(0xFF492400),
    secondaryContainer = Color(0xFF673600),
    onSecondaryContainer = Color(0xFFFFDCC0),

    tertiary = AudioGreen,
    onTertiary = Color(0xFF003919),
    tertiaryContainer = Color(0xFF005327),
    onTertiaryContainer = Color(0xFF70FF9B),

    background = StudioBackground,
    onBackground = TextPrimary,

    surface = StudioSurface,
    onSurface = TextPrimary,
    surfaceVariant = StudioSurfaceVariant,
    onSurfaceVariant = TextSecondary,

    outline = StudioCardBorder,
    outlineVariant = Color(0xFF1E293B)
)

@Composable
fun AudioStudioTheme(
    content: @Composable () -> Unit
) {
    // Para asegurar que el tamaño de fuente configurado en el sistema del teléfono
    // del usuario no colisione con el diseño de la cuadrícula de herramientas tipo AudioLab
    // ni rompa los textos fijos de las tarjetas o botones, fijamos el fontScale en 1.0f estricto.
    val currentDensity = LocalDensity.current
    val fixedDensity = Density(
        density = currentDensity.density,
        fontScale = 1.0f
    )

    CompositionLocalProvider(LocalDensity provides fixedDensity) {
        MaterialTheme(
            colorScheme = StudioDarkColorScheme,
            typography = Typography,
            content = content
        )
    }
}

