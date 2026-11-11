package com.hussain.messmobile.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.hussain.messmobile.R
import com.hussain.messmobile.ui.theme.SplashBackground
import kotlinx.coroutines.delay

private const val SPLASH_HOLD_MILLIS = 350L
private const val SPLASH_FADE_MILLIS = 600

/**
 * The animated splash overlay.
 *
 * It covers the app while it starts up and fades out once the content is ready.
 *
 * @param onReady called as soon as the overlay is on screen, this releases the system splash
 * screen that is kept on screen by the activity.
 * @param onFinished called when the overlay faded out completely.
 */
@Composable
fun SplashOverlay(
    onReady: () -> Unit,
    onFinished: () -> Unit,
) {
    val overlayAlpha = remember { Animatable(initialValue = 1f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SplashBackground)
            .graphicsLayer { alpha = overlayAlpha.value },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.expo_logo),
            contentDescription = null,
            modifier = Modifier.size(width = 76.dp, height = 71.dp),
        )
    }

    LaunchedEffect(Unit) {
        onReady()
        delay(SPLASH_HOLD_MILLIS)
        overlayAlpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = SPLASH_FADE_MILLIS, easing = FastOutSlowInEasing),
        )
        onFinished()
    }
}
