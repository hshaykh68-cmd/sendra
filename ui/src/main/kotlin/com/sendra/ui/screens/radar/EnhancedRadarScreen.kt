package com.sendra.ui.screens.radar

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sendra.domain.model.DeviceType
import com.sendra.ui.theme.RadarRingColor
import com.sendra.ui.theme.RadarSweepColor
import com.sendra.ui.theme.SendraBlue
import com.sendra.ui.theme.SendraBlueLight
import kotlinx.coroutines.delay

@Composable
fun EnhancedRadarScreen(
    onNavigateToTransfer: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: RadarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // Handle events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is RadarEvent.NavigateToTransfer -> onNavigateToTransfer(event.sessionId)
                is RadarEvent.ShowError -> { }
                is RadarEvent.ShowToast -> { }
            }
        }
    }
    
    Scaffold(
        topBar = {
            EnhancedRadarTopBar(
                fileCount = uiState.selectedFiles.size,
                totalSize = formatFileSize(uiState.totalSize),
                onBack = onNavigateBack
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Animated sweep angle
            val infiniteTransition = rememberInfiniteTransition(label = "sweep")
            val sweepAngle by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2500, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "sweep"
            )
            
            // Background gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                SendraBlue.copy(alpha = 0.03f),
                                Color.Transparent
                            ),
                            center = Offset(
                                LocalDensity.current.run { (LocalDensity.current.density * 200) },
                                LocalDensity.current.run { (LocalDensity.current.density * 300) }
                            ),
                            radius = LocalDensity.current.run { (LocalDensity.current.density * 400) }
                        )
                    )
            )
            
            // Enhanced Radar Background
            EnhancedRadarBackground(
                sweepAngle = sweepAngle,
                modifier = Modifier.fillMaxSize()
            )
            
            // Center point with glow
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                EnhancedRadarCenterPoint()
            }
            
            // Devices on radar
            uiState.devices.forEach { deviceNode ->
                key(deviceNode.device.id) {
                    EnhancedDeviceNodeView(
                        node = deviceNode,
                        onClick = { viewModel.onDeviceSelected(deviceNode.device) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            
            // Scanning indicator
            if (uiState.isScanning) {
                EnhancedScanningIndicator(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                )
            }
            
            // Device count badge
            if (uiState.deviceCount > 0) {
                EnhancedDeviceCountBadge(
                    count = uiState.deviceCount,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp)
                )
            } else {
                EnhancedEmptyStateMessage(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 32.dp)
                )
            }
            
            // Transfer loading overlay
            if (uiState.isTransferring) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable(enabled = false) { },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 4.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Connecting to ${uiState.selectedDevice?.name ?: "device"}...",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EnhancedRadarBackground(
    sweepAngle: Float,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val primaryColor = SendraBlue
    
    Canvas(modifier = modifier) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val maxRadius = minOf(centerX, centerY) * 0.85f
        
        // Draw gradient concentric circles
        val ringCount = 4
        repeat(ringCount) { i ->
            val radius = maxRadius * (i + 1) / ringCount
            val alpha = 0.4f - (i * 0.08f)
            
            // Outer glow
            drawCircle(
                color = primaryColor.copy(alpha = alpha * 0.3f),
                radius = radius,
                center = Offset(centerX, centerY),
                style = Stroke(width = 3.dp.toPx())
            )
            
            // Inner line
            drawCircle(
                color = primaryColor.copy(alpha = alpha * 0.6f),
                radius = radius,
                center = Offset(centerX, centerY),
                style = Stroke(width = 1.dp.toPx())
            )
        }
        
        // Crosshair with gradient effect
        drawLine(
            color = primaryColor.copy(alpha = 0.15f),
            start = Offset(centerX - maxRadius, centerY),
            end = Offset(centerX + maxRadius, centerY),
            strokeWidth = 2.dp.toPx()
        )
        drawLine(
            color = primaryColor.copy(alpha = 0.15f),
            start = Offset(centerX, centerY - maxRadius),
            end = Offset(centerX, centerY + maxRadius),
            strokeWidth = 2.dp.toPx()
        )
        
        // Enhanced sweep with glow
        val glowBrush = Brush.sweepGradient(
            0f to Color.Transparent,
            0.05f to SendraBlueLight.copy(alpha = 0.4f),
            0.15f to SendraBlue.copy(alpha = 0.3f),
            0.25f to SendraBlueLight.copy(alpha = 0.15f),
            0.4f to Color.Transparent,
            center = Offset(centerX, centerY)
        )
        
        // Draw sweep arc with glow
        drawArc(
            brush = glowBrush,
            startAngle = sweepAngle - 45,
            sweepAngle = 70f,
            useCenter = true,
            topLeft = Offset(centerX - maxRadius, centerY - maxRadius),
            size = androidx.compose.ui.geometry.Size(maxRadius * 2, maxRadius * 2)
        )
        
        // Leading edge highlight
        val edgeBrush = Brush.sweepGradient(
            0f to Color.Transparent,
            0.02f to Color.White.copy(alpha = 0.6f),
            0.08f to SendraBlueLight.copy(alpha = 0.5f),
            0.15f to Color.Transparent,
            center = Offset(centerX, centerY)
        )
        
        drawArc(
            brush = edgeBrush,
            startAngle = sweepAngle - 20,
            sweepAngle = 25f,
            useCenter = true,
            topLeft = Offset(centerX - maxRadius, centerY - maxRadius),
            size = androidx.compose.ui.geometry.Size(maxRadius * 2, maxRadius * 2)
        )
    }
}

@Composable
private fun EnhancedRadarCenterPoint(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseOutQuad),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale"
    )
    
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseOutQuad),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )
    
    Box(
        modifier = modifier.size(80.dp),
        contentAlignment = Alignment.Center
    ) {
        // Pulsing outer ring
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(pulseScale)
                .alpha(pulseAlpha)
                .background(
                    color = SendraBlue.copy(alpha = 0.3f),
                    shape = CircleShape
                )
        )
        
        // Outer glow ring
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(
                    color = SendraBlue.copy(alpha = 0.2f),
                    shape = CircleShape
                )
        )
        
        // Main container
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            SendraBlue,
                            SendraBlue.copy(alpha = 0.8f)
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Inner dot
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(
                        color = Color.White.copy(alpha = 0.9f),
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
private fun EnhancedDeviceNodeView(
    node: DeviceNode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val colorScheme = MaterialTheme.colorScheme
    
    val (x, y) = node.position.toCartesian()
    
    val baseColor = when (node.visualState) {
        DeviceVisualState.TRUSTED -> colorScheme.tertiary
        DeviceVisualState.PROMINENT -> SendraBlue
        DeviceVisualState.NORMAL -> colorScheme.secondary
    }
    
    // Entrance animation
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(node.device.id) {
        delay(100)
        visible = true
    }
    
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
        label = "scale"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(400),
        label = "alpha"
    )
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .offset(
                    x = with(density) { (x * 300).dp },
                    y = with(density) { (y * 300).dp }
                )
                .scale(scale)
                .alpha(alpha)
                .clickable(
                    onClick = onClick,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Device icon bubble with glow
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            color = baseColor.copy(alpha = 0.2f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Inner colored circle
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        baseColor,
                                        baseColor.copy(alpha = 0.8f)
                                    )
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (node.device.type) {
                                DeviceType.PHONE -> Icons.Default.Smartphone
                                DeviceType.TABLET -> Icons.Default.TabletAndroid
                                DeviceType.LAPTOP -> Icons.Default.Laptop
                                else -> Icons.Default.Devices
                            },
                            contentDescription = node.device.name,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                
                // Device name label with background
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colorScheme.surface.copy(alpha = 0.9f),
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = node.device.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                        
                        if (node.device.isTrusted) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Trusted",
                                modifier = Modifier.size(10.dp),
                                tint = colorScheme.tertiary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EnhancedScanningIndicator(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(
                        color = SendraBlue.copy(alpha = alpha),
                        shape = CircleShape
                    )
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Scanning for devices...",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun EnhancedDeviceCountBadge(
    count: Int,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = colorScheme.primaryContainer,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(
                        color = colorScheme.primary,
                        shape = CircleShape
                    )
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "$count device${if (count > 1) "s" else ""} nearby",
                style = MaterialTheme.typography.labelLarge,
                color = colorScheme.onPrimaryContainer,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
            )
        }
    }
}

@Composable
private fun EnhancedEmptyStateMessage(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.WifiTethering,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = SendraBlue.copy(alpha = 0.6f)
            )
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        Text(
            text = "No devices found nearby",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Make sure both devices have Sendra open\nand are on the same WiFi network",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnhancedRadarTopBar(
    fileCount: Int,
    totalSize: String,
    onBack: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = "Select Device",
                    style = MaterialTheme.typography.titleMedium
                )
                if (fileCount > 0) {
                    Text(
                        text = "$fileCount files ($totalSize)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surface
        )
    )
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1_000_000_000 -> "%.1f GB".format(bytes / 1_000_000_000.0)
        bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
        bytes >= 1_000 -> "%.1f KB".format(bytes / 1_000.0)
        else -> "$bytes B"
    }
}
