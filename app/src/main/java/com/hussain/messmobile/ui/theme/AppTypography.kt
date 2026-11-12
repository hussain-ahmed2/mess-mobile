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
 * All sizes are scaled for a baseline of 720p (1280×720) density and will scale appropriately
 * on other screens via the `sp` unit, but the baseline ensures consistent readability.
 */
object AppTextStyle {
    // Body text used for most paragraphs, captions, and list items.
    val Default = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium,
    )

    // Page/page-section headers, should be concise.
    val Title = TextStyle(
        fontSize = 36.sp,
        lineHeight = 44.sp,
        fontWeight = FontWeight.SemiBold,
    )

    // Toolbar/section titles inside cards.
    val AppBar = TextStyle(
        fontSize = 18.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.SemiBold,
    )

    // Secondary informational text, e.g. dates, metadata.
    val Subtitle = TextStyle(
        fontSize = 24.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.SemiBold,
    )

    // Small body text, fine print, secondary labels.
    val Small = TextStyle(
        fontSize = 12.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Medium,
    )

    // Small bold, e.g. metric labels.
    val SmallBold = TextStyle(
        fontSize = 12.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Bold,
    )

    // Hyperlink text, regular weight.
    val Link = TextStyle(
        fontSize = 13.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Normal,
    )

    // Primary action link, usually the app's accent colour.
    val LinkPrimary = TextStyle(
        fontSize = 13.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Normal,
        color = Color(0xFF3C87F7),
    )

    // Monospace for code snippets or logs.
    val Code = TextStyle(
        fontSize = 11.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
    )
}
