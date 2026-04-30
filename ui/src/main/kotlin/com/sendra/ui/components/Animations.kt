package com.sendra.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay

// Animation durations
const val ANIMATION_DURATION_FAST = 150
const val ANIMATION_DURATION_NORMAL = 300
const val ANIMATION_DURATION_SLOW = 500
const val ANIMATION_DURATION_VERY_SLOW = 800

// Animation specs
fun <T> fastSpring() = spring<T>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessMedium
)

fun <T> bouncySpring() = spring<T>(
    dampingRatio = Spring.DampingRatioLowBouncy,
    stiffness = Spring.StiffnessLow
)

fun <T> quickTween() = tween<T>(
    durationMillis = ANIMATION_DURATION_FAST,
    easing = FastOutSlowInEasing
)

fun <T> standardTween() = tween<T>(
    durationMillis = ANIMATION_DURATION_NORMAL,
    easing = FastOutSlowInEasing
)

/**
 * Scale animation modifier for button press effects
 */
fun Modifier.scaleOnPress(
    pressedScale: Float = 0.95f
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = fastSpring(),
        label = "scale"
    )

    this.graphicsLayer { scaleX = scale; scaleY = scale }
}

/**
 * Fade in animation for content entry
 */
@Composable
fun FadeInContent(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = standardTween()),
        exit = fadeOut(animationSpec = quickTween()),
        modifier = modifier
    ) {
        content()
    }
}

/**
 * Slide up animation for content entry
 */
@Composable
fun SlideUpContent(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it / 2 },
            animationSpec = standardTween()
        ) + fadeIn(standardTween()),
        exit = slideOutVertically(
            targetOffsetY = { it / 2 },
            animationSpec = quickTween()
        ) + fadeOut(quickTween()),
        modifier = modifier
    ) {
        content()
    }
}

/**
 * Scale and fade animation for list items
 */
@Composable
fun ScaleInListItem(
    visible: Boolean,
    delayMillis: Int = 0,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(
            initialScale = 0.8f,
            animationSpec = tween(ANIMATION_DURATION_NORMAL, delayMillis, FastOutSlowInEasing)
        ) + fadeIn(
            animationSpec = tween(ANIMATION_DURATION_NORMAL, delayMillis, FastOutSlowInEasing)
        ),
        exit = scaleOut(
            targetScale = 0.8f,
            animationSpec = quickTween()
        ) + fadeOut(quickTween()),
        modifier = modifier
    ) {
        content()
    }
}

/**
 * Pulsing animation for attention indicators
 */
@Composable
fun PulsingAnimation(
    content: @Composable (alpha: Float, scale: Float) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    content(alpha, scale)
}

/**
 * Rotating animation for loading indicators
 */
@Composable
fun RotatingAnimation(
    durationMillis: Int = 2000,
    content: @Composable (rotation: Float) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "rotate")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    content(rotation)
}

/**
 * Shimmer effect modifier
 */
fun Modifier.shimmerEffect(): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")

    val translateAnim by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    graphicsLayer {
        alpha = 0.3f + (0.5f * (translateAnim + 1) / 2)
    }
}

/**
 * Animated number counter
 */
@Composable
fun AnimatedNumber(
    target: Int,
    durationMillis: Int = 500,
    content: @Composable (current: Int) -> Unit
) {
    val animatedValue by animateIntAsState(
        targetValue = target,
        animationSpec = tween(durationMillis, easing = FastOutSlowInEasing),
        label = "number"
    )

    content(animatedValue)
}

/**
 * Crossfade animation for content switching
 */
@Composable
fun <T> CrossfadeContent(
    targetState: T,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit
) {
    Crossfade(
        targetState = targetState,
        animationSpec = standardTween(),
        modifier = modifier
    ) { state ->
        content(state)
    }
}

/**
 * Staggered list animation helper
 */
@Composable
fun StaggeredList(
    count: Int,
    modifier: Modifier = Modifier,
    staggerDelay: Int = 50,
    itemContent: @Composable (index: Int) -> Unit
) {
    Column(modifier = modifier) {
        repeat(count) { index ->
            var visible by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                delay(index * staggerDelay.toLong())
                visible = true
            }

            ScaleInListItem(
                visible = visible,
                delayMillis = 0
            ) {
                itemContent(index)
            }
        }
    }
}

/**
 * Bounce animation modifier
 */
fun Modifier.bounceEffect(): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "bounce")

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )

    graphicsLayer { scaleX = scale; scaleY = scale }
}

/**
 * Float animation for floating elements
 */
@Composable
fun FloatingAnimation(
    durationMillis: Int = 2000,
    verticalOffset: Float = 10f,
    content: @Composable (offsetY: Float) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "float")

    val offsetY by infiniteTransition.animateFloat(
        initialValue = -verticalOffset,
        targetValue = verticalOffset,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )

    content(offsetY)
}
