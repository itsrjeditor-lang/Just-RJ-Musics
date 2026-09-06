package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
  primary = RjSilverAccent,
  onPrimary = RjBackground,
  primaryContainer = RjCardElevated,
  onPrimaryContainer = RjTextPrimary,
  secondary = RjSilverMuted,
  onSecondary = RjBackground,
  secondaryContainer = RjCard,
  onSecondaryContainer = RjTextPrimary,
  tertiary = Color(0xFFCCCCCC),
  onTertiary = RjBackground,
  background = RjBackground,
  onBackground = RjTextPrimary,
  surface = RjBackgroundSecondary,
  onSurface = RjTextPrimary,
  surfaceVariant = RjCard,
  onSurfaceVariant = RjTextSecondary,
  outline = RjBorder,
  outlineVariant = RjBorderLight
)

@Composable
fun RjMusicsTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = DarkColorScheme,
    typography = Typography,
    content = content
  )
}

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  RjMusicsTheme(darkTheme, dynamicColor, content)
}

