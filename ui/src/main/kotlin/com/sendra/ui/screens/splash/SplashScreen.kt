package com.sendra.ui.screens.splash

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sendra.ui.theme.SendraBlue
import com.sendra.ui.theme.SendraBlueLight
import com.sendra.ui.theme.SendraPurple
import com.sendra.ui.theme.SendraPurpleLight
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigateToHome: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Handle navigation when splash completes
    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete) {
            delay(200) // Small delay for smooth transition
            onNavigateToHome()
        }
    }
    
    SplashContent(
        animationState = uiState.animationState,
        progress = uiState.loadingProgress
    )
}

@Composable
private fun SplashContent(
    animationState: SplashAnimationState,
    progress: Float
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        SendraBlue.copy(alpha = 0.05f),
                        Color.White
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Animated background circles
        AnimatedBackgroundCircles()
        
        // Main content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo container with animations
            LogoContainer(
                animationState = animationState,
                modifier = Modifier.size(200.dp)
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Brand name with staggered reveal
            BrandNameReveal(animationState = animationState)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Tagline fade in
            TaglineReveal(animationState = animationState)
            
            Spacer(modifier = Modifier.height(64.dp))
            
            // Loading progress indicator
            LoadingProgress(
                animationState = animationState,
                progress = progress
            )
        }
    }
}

@Composable
private fun AnimatedBackgroundCircles() {
    val infiniteTransition = rememberInfiniteTransition(label = "background")
    
    // Multiple floating circles
    val circles = listOf(
        CircleConfig(0.15f, 120.dp, SendraBlue.copy(alpha = 0.08f), 3000, -50f, 50f),
        CircleConfig(0.25f, 80.dp, SendraPurple.copy(alpha = 0.06f), 4000, 30f, -30f),
        CircleConfig(0.35f, 60.dp, SendraBlueLight.copy(alpha = 0.1f), 2500, -20f, 20f),
        CircleConfig(0.2f, 100.dp, SendraPurpleLight.copy(alpha = 0.05f), 3500, 40f, -40f)
    )
    
    circles.forEachIndexed { index, config ->
        val offsetX by infiniteTransition.animateFloat(
            initialValue = config.startX,
            targetValue = config.endX,
            animationSpec = infiniteRepeatable(
                animation = tween(config.duration, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "offsetX_$index"
        )
        
        val offsetY by infiniteTransition.animateFloat(
            initialValue = -30f,
            targetValue = 30f,
            animationSpec = infiniteRepeatable(
                animation = tween(config.duration + 500, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "offsetY_$index"
        )
        
        val scale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(config.duration / 2, easing = EaseInOutQuad),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale_$index"
        )
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = offsetX
                    translationY = offsetY
                    scaleX = scale
                    scaleY = scale
                },
            contentAlignment = if (index % 2 == 0) Alignment.TopStart else Alignment.BottomEnd
        ) {
            Box(
                modifier = Modifier
                    .padding(
                        start = if (index % 2 == 0) (config.size.value * config.position).dp else 0.dp,
                        end = if (index % 2 != 0) (config.size.value * config.position).dp else 0.dp,
                        top = if (index < 2) (config.size.value * 0.5f).dp else 0.dp,
                        bottom = if (index >= 2) (config.size.value * 0.5f).dp else 0.dp
                    )
                    .size(config.size)
                    .background(
                        color = config.color,
                        shape = CircleShape
                    )
            )
        }
    }
}

private data class CircleConfig(
    val position: Float,
    val size: androidx.compose.ui.unit.Dp,
    val color: Color,
    val duration: Int,
    val startX: Float,
    val endX: Float
)

@Composable
private fun LogoContainer(
    animationState: SplashAnimationState,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = when (animationState) {
            SplashAnimationState.IDLE -> 0f
            SplashAnimationState.LOGO_APPEAR -> 1f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "logoScale"
    )
    
    val alpha by animateFloatAsState(
        targetValue = when (animationState) {
            SplashAnimationState.IDLE -> 0f
            else -> 1f
        },
        animationSpec = tween(600, easing = EaseOutQuad),
        label = "logoAlpha"
    )
    
    // Rotating ring animation
    val infiniteTransition = rememberInfiniteTransition(label = "rotate")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    // Pulsing glow
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Outer pulsing glow
        Box(
            modifier = Modifier
                .size(180.dp)
                .scale(pulseScale)
                .alpha(pulseAlpha)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            SendraBlue.copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
        
        // Rotating ring
        Box(
            modifier = Modifier
                .size(160.dp)
                .graphicsLayer { rotationZ = rotation }
        ) {
            // Dotted ring effect
            DottedRing(
                dotCount = 8,
                radius = 80.dp,
                dotSize = 8.dp,
                color = SendraBlue.copy(alpha = 0.3f)
            )
        }
        
        // Secondary rotating ring (opposite direction)
        Box(
            modifier = Modifier
                .size(140.dp)
                .graphicsLayer { rotationZ = -rotation * 0.7f }
        ) {
            DottedRing(
                dotCount = 6,
                radius = 70.dp,
                dotSize = 6.dp,
                color = SendraPurple.copy(alpha = 0.25f)
            )
        }
        
        // Main logo container
        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(scale)
                .alpha(alpha)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(SendraBlue, SendraPurple)
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Inner highlight
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.2f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )
            
            // Icon
            Icon(
                imageVector = Icons.Default.CloudUpload,
                contentDescription = "Sendra",
                modifier = Modifier.size(56.dp),
                tint = Color.White
            )
        }
        
        // Sparkle effects around logo
        if (animationState != SplashAnimationState.IDLE) {
            SparkleEffects()
        }
    }
}

@Composable
private fun DottedRing(
    dotCount: Int,
    radius: androidx.compose.ui.unit.Dp,
    dotSize: androidx.compose.ui.unit.Dp,
    color: Color
) {
    val density = LocalDensity.current
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val radiusPx = with(density) { radius.toPx() }
        repeat(dotCount) { index ->
            val angle = (index * 360f / dotCount) * (Math.PI / 180f)
            val x = kotlin.math.cos(angle) * radiusPx
            val y = kotlin.math.sin(angle) * radiusPx
            
            Box(
                modifier = Modifier
                    .offset(
                        x = with(density) { x.toFloat().toDp() },
                        y = with(density) { y.toFloat().toDp() }
                    )
                    .size(dotSize)
                    .background(
                        color = color,
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
private fun SparkleEffects() {
    val infiniteTransition = rememberInfiniteTransition(label = "sparkles")
    val density = LocalDensity.current
    
    val sparkles = listOf(
        SparkleConfig(45f, 90.dp, 0),
        SparkleConfig(135f, 90.dp, 400),
        SparkleConfig(225f, 90.dp, 800),
        SparkleConfig(315f, 90.dp, 1200)
    )
    
    sparkles.forEach { config ->
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, delayMillis = config.delay, easing = EaseInOutQuad),
                repeatMode = RepeatMode.Reverse
            ),
            label = "sparkle_${config.delay}"
        )
        
        val scale by infiniteTransition.animateFloat(
            initialValue = 0.5f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, delayMillis = config.delay, easing = EaseOutBack),
                repeatMode = RepeatMode.Reverse
            ),
            label = "sparkleScale_${config.delay}"
        )
        
        val angleRad = config.angle * (Math.PI / 180f)
        val distancePx = with(density) { config.distance.toPx() }
        val x = kotlin.math.cos(angleRad) * distancePx
        val y = kotlin.math.sin(angleRad) * distancePx
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = x.toFloat()
                    translationY = y.toFloat()
                    scaleX = scale
                    scaleY = scale
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .alpha(alpha)
                    .background(
                        color = Color.White,
                        shape = CircleShape
                    )
            )
        }
    }
}

private data class SparkleConfig(
    val angle: Float,
    val distance: androidx.compose.ui.unit.Dp,
    val delay: Int
)

@Composable
private fun BrandNameReveal(
    animationState: SplashAnimationState
) {
    val brandText = "Sendra"
    
    AnimatedVisibility(
        visible = animationState >= SplashAnimationState.TEXT_REVEAL,
        enter = fadeIn(animationSpec = tween(600, easing = EaseOutQuad)) +
                slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = tween(600, easing = EaseOutQuad)
                ),
        exit = fadeOut()
    ) {
        Row {
            brandText.forEachIndexed { index, char ->
                val charDelay = index * 50
                var charVisible by remember { mutableStateOf(false) }
                
                LaunchedEffect(animationState) {
                    if (animationState >= SplashAnimationState.TEXT_REVEAL) {
                        delay(charDelay.toLong())
                        charVisible = true
                    }
                }
                
                val charScale by animateFloatAsState(
                    targetValue = if (charVisible) 1f else 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "char_$index"
                )
                
                Text(
                    text = char.toString(),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = SendraBlue,
                    modifier = Modifier.scale(charScale)
                )
            }
        }
    }
}

@Composable
private fun TaglineReveal(
    animationState: SplashAnimationState
) {
    AnimatedVisibility(
        visible = animationState >= SplashAnimationState.TAGLINE_FADE,
        enter = fadeIn(animationSpec = tween(500, delayMillis = 200)) +
                slideInVertically(
                    initialOffsetY = { it / 3 },
                    animationSpec = tween(500, delayMillis = 200, easing = EaseOutQuad)
                ),
        exit = fadeOut()
    ) {
        Text(
            text = "Fast. Secure. Offline.",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 2.sp
        )
    }
}

@Composable
private fun LoadingProgress(
    animationState: SplashAnimationState,
    progress: Float
) {
    AnimatedVisibility(
        visible = animationState >= SplashAnimationState.LOADING_START,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Progress bar
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(4.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape
                    )
            ) {
                val animatedProgress by animateFloatAsState(
                    targetValue = progress,
                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                    label = "progress"
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(SendraBlue, SendraPurple)
                            ),
                            shape = CircleShape
                        )
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Loading dots
            LoadingDots()
        }
    }
}

@Composable
private fun LoadingDots() {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val delay = index * 150
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(500, delayMillis = delay, easing = EaseInOutQuad),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot_$index"
            )
            
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.8f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(500, delayMillis = delay, easing = EaseInOutQuad),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dotScale_$index"
            )
            
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .scale(scale)
                    .alpha(alpha)
                    .background(
                        color = SendraBlue,
                        shape = CircleShape
                    )
            )
        }
    }
}
