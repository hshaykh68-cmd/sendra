package com.sendra.ui.screens.radar

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sendra.domain.model.Device
import com.sendra.domain.model.DeviceType
import kotlinx.coroutines.delay

@Composable
fun RadarScreen(
    onNavigateToTransfer: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: RadarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    // Handle events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is RadarEvent.NavigateToTransfer -> onNavigateToTransfer(event.sessionId)
                is RadarEvent.ShowError -> {
                    // Show snackbar
                }
                is RadarEvent.ShowToast -> {
                    // Show toast
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            RadarTopBar(
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
        ) {
            // Radar sweep animation
            val infiniteTransition = rememberInfiniteTransition(label = "sweep")
            val sweepAngle by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(3000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "sweep"
            )
            
            // Concentric circles (radar rings)
            RadarBackground(
                sweepAngle = sweepAngle,
                modifier = Modifier.fillMaxSize()
            )
            
            // Center point (you are here)
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                RadarCenterPoint()
            }
            
            // Devices on radar
            uiState.devices.forEach { deviceNode ->
                key(deviceNode.device.id) {
                    DeviceNodeView(
                        node = deviceNode,
                        onClick = { viewModel.onDeviceSelected(deviceNode.device) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            
            // Scanning indicator
            if (uiState.isScanning) {
                ScanningIndicator(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                )
            }
            
            // Device count badge
            if (uiState.deviceCount > 0) {
                DeviceCountBadge(
                    count = uiState.deviceCount,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp)
                )
            } else {
                EmptyStateMessage(
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
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable(enabled = false) { },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Connecting to ${uiState.selectedDevice?.name ?: "device"}...",
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RadarBackground(
    sweepAngle: Float,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    
    Canvas(modifier = modifier) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val maxRadius = minOf(centerX, centerY) * 0.85f
        
        // Draw concentric circles
        val ringCount = 4
        repeat(ringCount) { i ->
            val radius = maxRadius * (i + 1) / ringCount
            drawCircle(
                color = colorScheme.outlineVariant.copy(alpha = 0.3f),
                radius = radius,
                center = Offset(centerX, centerY),
                style = Stroke(width = 1.dp.toPx())
            )
        }
        
        // Draw crosshair lines
        drawLine(
            color = colorScheme.outlineVariant.copy(alpha = 0.2f),
            start = Offset(centerX - maxRadius, centerY),
            end = Offset(centerX + maxRadius, centerY),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = colorScheme.outlineVariant.copy(alpha = 0.2f),
            start = Offset(centerX, centerY - maxRadius),
            end = Offset(centerX, centerY + maxRadius),
            strokeWidth = 1.dp.toPx()
        )
        
        // Draw sweep arc
        val sweepGradient = androidx.compose.ui.graphics.Brush.sweepGradient(
            0f to Color.Transparent,
            0.15f to colorScheme.primary.copy(alpha = 0.1f),
            0.3f to colorScheme.primary.copy(alpha = 0.05f),
            center = Offset(centerX, centerY)
        )
        
        drawArc(
            brush = sweepGradient,
            startAngle = sweepAngle - 30,
            sweepAngle = 60f,
            useCenter = true,
            topLeft = Offset(centerX - maxRadius, centerY - maxRadius),
            size = androidx.compose.ui.geometry.Size(maxRadius * 2, maxRadius * 2)
        )
    }
}

@Composable
private fun RadarCenterPoint(
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    
    Box(
        modifier = modifier
            .size(56.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer ring
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(
                    color = colorScheme.primaryContainer.copy(alpha = 0.3f),
                    shape = CircleShape
                )
        )
        
        // Inner circle
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    color = colorScheme.primary,
                    shape = CircleShape
                )
        )
        
        // Icon
        Icon(
            imageVector = Icons.Default.MyLocation,
            contentDescription = "Your device",
            modifier = Modifier.size(14.dp),
            tint = colorScheme.onPrimary
        )
    }
}

@Composable
private fun DeviceNodeView(
    node: DeviceNode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    
    // Calculate position
    val (x, y) = node.position.toCartesian()
    
    val baseColor = when (node.visualState) {
        DeviceVisualState.TRUSTED -> colorScheme.tertiary
        DeviceVisualState.PROMINENT -> colorScheme.primary
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
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "scale"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(300),
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
                // Device icon bubble
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = baseColor.copy(alpha = 0.15f),
                            shape = CircleShape
                        )
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                color = baseColor,
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
                            tint = colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Device name label
                Text(
                    text = node.device.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
                
                // Trust indicator
                if (node.device.isTrusted) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Trusted device",
                        modifier = Modifier.size(12.dp),
                        tint = colorScheme.tertiary
                    )
                }
            }
        }
    }
}

@Composable
private fun ScanningIndicator(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(12.dp),
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Scanning for devices...",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun DeviceCountBadge(
    count: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = "$count device${if (count > 1) "s" else ""} nearby",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun EmptyStateMessage(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.WifiTethering,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No devices found nearby",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Make sure both devices have Sendra open and are on the same WiFi network",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RadarTopBar(
    fileCount: Int,
    totalSize: String,
    onBack: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text("Select Device")
                Text(
                    "$fileCount files ($totalSize)",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
        }
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
