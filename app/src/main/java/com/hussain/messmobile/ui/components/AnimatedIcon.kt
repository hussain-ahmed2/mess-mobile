package com.hussain.messmobile.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.hussain.messmobile.R
import kotlinx.coroutines.launch

private const val GLOW_ROTATION_DEGREES = 7200f
private const val GLOW_ROTATION_DURATION_MILLIS = 240_000

/**
 * The animated logo of the home screen.
 *
 * The background scales down with an elastic easing, the logo fades in on top of it and the
 * glow behind it rotates very slowly.
 */
@Composable
fun AnimatedIcon(modifier: Modifier = Modifier) {
    val configuration = LocalConfiguration.current
    val initialScaleFactor = (configuration.screenHeightDp / 90f).coerceAtLeast(1f)

    val glowRotation by rememberInfiniteTransition(label = "glow").animateFloat(
        initialValue = 0f,
        targetValue = GLOW_ROTATION_DEGREES,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = GLOW_ROTATION_DURATION_MILLIS, easing = LinearEasing),
        ),
        label = "glowRotation",
    )

    val backgroundScale = remember { Animatable(initialValue = initialScaleFactor) }
    val logoScale = remember { Animatable(initialValue = 1.3f) }
    val logoAlpha = remember { Animatable(initialValue = 0f) }

    LaunchedEffect(Unit) {
        launch {
            backgroundScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow),
            )
        }
        launch {
            logoScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow),
            )
            logoAlpha.animateTo(targetValue = 1f, animationSpec = tween(durationMillis = 240))
        }
    }

    Box(
        modifier = modifier.size(128.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.logo_glow),
            contentDescription = null,
            modifier = Modifier
                .size(201.dp)
                .rotate(glowRotation),
        )

        Box(
            modifier = Modifier
                .size(128.dp)
                .scale(backgroundScale.value)
                .clip(RoundedCornerShape(40.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF3C9FFE), Color(0xFF0274DF)),
                    ),
                ),
        )

        Image(
            painter = painterResource(R.drawable.expo_logo),
            contentDescription = null,
            modifier = Modifier
                .size(width = 76.dp, height = 71.dp)
                .scale(logoScale.value)
                .alpha(logoAlpha.value),
        )
    }
}
