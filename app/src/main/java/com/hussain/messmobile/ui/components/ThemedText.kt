package com.hussain.messmobile.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.hussain.messmobile.ui.theme.AppTextStyle
import com.hussain.messmobile.ui.theme.appColors

/**
 * Text that follows the active colour palette.
 *
 * The [style] defaults to [AppTextStyle.Default] and the [color] to the regular text colour
 * of the current theme. A colour that is defined by the [style] wins over the theme colour,
 * pass an explicit [color] to override it.
 */
@Composable
fun ThemedText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = AppTextStyle.Default,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
) {
    Text(
        text = text,
        modifier = modifier,
        style = style,
        color = resolveColor(color, style),
        textAlign = textAlign,
        maxLines = maxLines,
    )
}

/** [ThemedText] for rich text, use [codeSpan] to style inline code. */
@Composable
fun ThemedText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    style: TextStyle = AppTextStyle.Default,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
) {
    Text(
        text = text,
        modifier = modifier,
        style = style,
        color = resolveColor(color, style),
        textAlign = textAlign,
        maxLines = maxLines,
    )
}

/**
 * The colour of the text: the explicit colour wins, then the colour of the style and
 * finally the text colour of the current theme.
 */
@Composable
private fun resolveColor(color: Color, style: TextStyle): Color = when {
    color != Color.Unspecified -> color
    style.color != Color.Unspecified -> style.color
    else -> appColors.text
}

/** Inline styling for code snippets inside a paragraph. */
fun codeSpan(): SpanStyle = SpanStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Bold,
    fontSize = 12.sp,
)
