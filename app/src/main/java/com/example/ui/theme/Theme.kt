package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = TechBlue,
    primaryContainer = TechBlueContainer,
    onPrimaryContainer = OnTechBlueContainer,
    secondary = CharcoalMuted,
    background = Color(0xFF121318),
    surface = Color(0xFF1E1F25),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFFE2E2E6),
    onSurface = Color(0xFFE2E2E6),
    outline = CharcoalMuted
  )

private val LightColorScheme =
  lightColorScheme(
    primary = TechBlue,
    primaryContainer = TechBlueContainer,
    onPrimaryContainer = OnTechBlueContainer,
    secondary = CharcoalMuted,
    background = LightSlate,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Charcoal,
    onSurface = Charcoal,
    outline = BorderGray
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
