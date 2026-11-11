package com.hussain.messmobile.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * The colour palette of the app.
 *
 * The palette is defined by hand instead of relying on the Material 3 roles so that the
 * light and the dark variant stay exactly in sync with each other.
 */
@Immutable
data class AppColors(
    val text: Color,
    val background: Color,
    val backgroundElement: Color,
    val backgroundSelected: Color,
    val textSecondary: Color,
)

val LightAppColors = AppColors(
    text = Color(0xFF000000),
    background = Color(0xFFFFFFFF),
    backgroundElement = Color(0xFFF0F0F3),
    backgroundSelected = Color(0xFFE0E1E6),
    textSecondary = Color(0xFF60646C),
)

val DarkAppColors = AppColors(
    text = Color(0xFFFFFFFF),
    background = Color(0xFF000000),
    backgroundElement = Color(0xFF212225),
    backgroundSelected = Color(0xFF2E3135),
    textSecondary = Color(0xFFB0B4BA),
)

/** Background of the splash screen and of the animated splash overlay. */
val SplashBackground = Color(0xFF208AEF)

/** Spacing scale. */
object Spacing {
    val half = 2.dp
    val one = 4.dp
    val two = 8.dp
    val three = 16.dp
    val four = 24.dp
    val five = 32.dp
    val six = 64.dp
}

/** Maximum width of the content, content is centred on wider screens. */
val MaxContentWidth = 800.dp
