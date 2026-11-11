package com.hussain.messmobile.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Text styles of the app.
 *
 * The names match the variants that the app exposes: `Default`, `Title`, `Small`, `SmallBold`,
 * `Subtitle`, `Link`, `LinkPrimary` and `Code`.
 */
object AppTextStyle {
    val Default = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Medium,
    )

    val Title = TextStyle(
        fontSize = 48.sp,
        lineHeight = 52.sp,
        fontWeight = FontWeight.SemiBold,
    )

    val AppBar = TextStyle(
        fontSize = 20.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.SemiBold,
    )

    val Subtitle = TextStyle(
        fontSize = 32.sp,
        lineHeight = 44.sp,
        fontWeight = FontWeight.SemiBold,
    )

    val Small = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium,
    )

    val SmallBold = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Bold,
    )

    val Link = TextStyle(
        fontSize = 14.sp,
        lineHeight = 30.sp,
        fontWeight = FontWeight.Normal,
    )

    val LinkPrimary = TextStyle(
        fontSize = 14.sp,
        lineHeight = 30.sp,
        fontWeight = FontWeight.Normal,
        color = Color(0xFF3C87F7),
    )

    val Code = TextStyle(
        fontSize = 12.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
    )
}
