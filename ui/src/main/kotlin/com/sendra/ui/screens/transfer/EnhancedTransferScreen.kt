package com.sendra.ui.screens.transfer

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sendra.ui.components.SendraIconButton
import com.sendra.ui.components.SendraStatusBadge
import com.sendra.ui.theme.SendraBlue
import com.sendra.ui.theme.SendraTeal
import kotlinx.coroutines.delay

@Composable
fun EnhancedTransferScreen(
    onNavigateBack: () -> Unit,
    viewModel: TransferViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // Handle navigation events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is TransferEvent.NavigateBack -> onNavigateBack()
                is TransferEvent.TransferComplete -> { }
                is TransferEvent.ShowSnackbar -> { }
            }
        }
    }
    
    val status = uiState.status
    
    Scaffold(
        topBar = {
            TransferTopBar(
                title = when (status) {
                    TransferScreenStatus.PREPARING -> "Preparing..."
                    TransferScreenStatus.ACTIVE -> "Sending..."
                    TransferScreenStatus.PAUSED -> "Paused"
                    TransferScreenStatus.RECONNECTING -> "Reconnecting..."
                    TransferScreenStatus.COMPLETED -> "Complete"
                    TransferScreenStatus.FAILED -> "Failed"
                    TransferScreenStatus.CANCELLED -> "Cancelled"
                },
                onClose = {
                    if (status == TransferScreenStatus.ACTIVE || 
                        status == TransferScreenStatus.PAUSED) {
                        viewModel.onCancelClicked()
                    } else {
                        onNavigateBack()
                    }
                },
                canClose = status != TransferScreenStatus.PREPARING && 
                          status != TransferScreenStatus.RECONNECTING
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
        ) {
            // Animated content switching
            AnimatedContent(
                targetState = status,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith
                    fadeOut(animationSpec = tween(200))
                },
                label = "content"
            ) { targetStatus ->
                when (targetStatus) {
                    TransferScreenStatus.PREPARING, 
                    TransferScreenStatus.RECONNECTING -> {
                        EnhancedLoadingState(message = uiState.statusMessage ?: "Preparing transfer...")
                    }
                    
                    TransferScreenStatus.ACTIVE, 
                    TransferScreenStatus.PAUSED -> {
                        EnhancedActiveTransferContent(
                            uiState = uiState,
                            onPause = viewModel::onPauseClicked,
                            onResume = viewModel::onResumeClicked,
                            onCancel = viewModel::onCancelClicked
                        )
                    }
                    
                    TransferScreenStatus.COMPLETED -> {
                        EnhancedCompletedContent(onDone = viewModel::onDoneClicked)
                    }
                    
                    TransferScreenStatus.FAILED -> {
                        EnhancedFailedContent(
                            errorMessage = uiState.errorMessage ?: "Transfer failed",
                            canResume = uiState.canResume,
                            onRetry = viewModel::onRetryClicked,
                            onCancel = viewModel::onCancelClicked
                        )
                    }
                    
                    TransferScreenStatus.CANCELLED -> {
                        EnhancedCancelledContent(onBack = viewModel::onDoneClicked)
                    }
                }
            }
        }
    }
}

@Composable
private fun EnhancedActiveTransferContent(
    uiState: TransferUiState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        
        // Animated file icon
        AnimatedFileIcon(
            isPaused = uiState.isPaused,
            modifier = Modifier.weight(0.25f)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Progress section with animations
        EnhancedProgressSection(
            progress = uiState.progress,
            bytesTransferred = uiState.bytesTransferred,
            totalBytes = uiState.totalBytes,
            speedText = uiState.speedText,
            etaText = uiState.etaText,
            isPaused = uiState.isPaused,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Multi-file indicator with animation
        AnimatedVisibility(
            visible = uiState.totalFiles > 1,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut()
        ) {
            SendraStatusBadge(
                text = "File ${uiState.currentFileIndex + 1} of ${uiState.totalFiles}",
                isActive = !uiState.isPaused
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Animated controls
        EnhancedTransferControls(
            isPaused = uiState.isPaused,
            onPause = onPause,
            onResume = onResume,
            onCancel = onCancel
        )
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun AnimatedFileIcon(
    isPaused: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "float")
    
    // Floating animation when active
    val offsetY by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )
    
    // Pulsing scale
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isPaused) 1f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.2f)
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        SendraBlue.copy(alpha = 0.1f),
                        SendraBlue.copy(alpha = 0.05f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                translationY = if (isPaused) 0f else offsetY
                scaleX = scale
                scaleY = scale
            }
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        color = SendraBlue.copy(alpha = 0.15f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CloudUpload,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = SendraBlue
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = if (isPaused) "Paused" else "Transferring...",
                style = MaterialTheme.typography.bodyLarge,
                color = if (isPaused) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    SendraBlue
                }
            )
        }
    }
}

@Composable
private fun EnhancedProgressSection(
    progress: Int,
    bytesTransferred: Long,
    totalBytes: Long,
    speedText: String,
    etaText: String,
    isPaused: Boolean,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress / 100f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "progress"
    )
    
    Column(modifier = modifier) {
        // Progress bar with shimmer effect when active
        Box(modifier = Modifier.fillMaxWidth()) {
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp)),
                color = if (isPaused) 
                    MaterialTheme.colorScheme.surfaceVariant 
                else 
                    SendraBlue,
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                strokeCap = StrokeCap.Round
            )
            
            // Shimmer overlay when active
            if (!isPaused && progress < 100) {
                ShimmerProgressOverlay()
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Progress percentage with animation
        AnimatedContent(
            targetState = progress,
            transitionSpec = {
                slideInVertically { it } + fadeIn() togetherWith
                slideOutVertically { -it } + fadeOut()
            },
            label = "percentage"
        ) { targetProgress ->
            Text(
                text = "$targetProgress%",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Bytes transferred
        Text(
            text = "${formatBytes(bytesTransferred)} / ${formatBytes(totalBytes)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Speed and ETA with animated values
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            EnhancedInfoChip(
                icon = Icons.Default.Speed,
                label = "Speed",
                value = if (isPaused) "Paused" else speedText,
                isPaused = isPaused
            )
            EnhancedInfoChip(
                icon = Icons.Default.Schedule,
                label = if (isPaused) "Waiting..." else "Time left",
                value = if (isPaused) "--" else etaText,
                isPaused = isPaused
            )
        }
    }
}

@Composable
private fun ShimmerProgressOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    
    val translateX by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.3f),
                        Color.Transparent
                    ),
                    startX = { width -> width * translateX },
                    endX = { width -> width * (translateX + 0.5f) }
                )
            )
    )
}

@Composable
private fun EnhancedInfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    isPaused: Boolean,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isPaused) 0.95f else 1f,
        animationSpec = tween(300),
        label = "scale"
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .scale(scale)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = if (isPaused) {
                        MaterialTheme.colorScheme.surfaceVariant
                    } else {
                        SendraBlue.copy(alpha = 0.1f)
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (isPaused) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    SendraBlue
                }
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (isPaused) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
        
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EnhancedTransferControls(
    isPaused: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Pause/Resume button with animation
        AnimatedContent(
            targetState = isPaused,
            transitionSpec = {
                scaleIn(animationSpec = spring(stiffness = 500f)) + fadeIn() togetherWith
                scaleOut(animationSpec = spring(stiffness = 500f)) + fadeOut()
            },
            modifier = Modifier.weight(1f)
        ) { paused ->
            if (paused) {
                Button(
                    onClick = onResume,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SendraTeal
                    )
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Resume")
                }
            } else {
                OutlinedButton(
                    onClick = onPause,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Pause, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pause")
                }
            }
        }
        
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(Icons.Default.Close, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Cancel")
        }
    }
}

@Composable
private fun EnhancedLoadingState(message: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "rotate")
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(64.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .graphicsLayer { rotationZ = rotation },
                    tint = SendraBlue
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Pulsing dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(3) { index ->
                    val delay = index * 200
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600, delayMillis = delay, easing = EaseInOutQuad),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dot$index"
                    )
                    
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = SendraBlue.copy(alpha = alpha),
                                shape = CircleShape
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun EnhancedCompletedContent(onDone: () -> Unit) {
    var showContent by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(100)
        showContent = true
    }
    
    val scale by animateFloatAsState(
        targetValue = if (showContent) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f),
        label = "scale"
    )
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Success icon with bounce animation
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(scale)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                SendraTeal.copy(alpha = 0.3f),
                                SendraTeal.copy(alpha = 0.1f)
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                SuccessCheckmark()
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn() + slideInVertically { it / 2 }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Transfer Complete!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = SendraTeal
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Files received successfully",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Button(
                        onClick = onDone,
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SendraTeal
                        )
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Done", fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SuccessCheckmark() {
    val infiniteTransition = rememberInfiniteTransition(label = "success")
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    Box(
        modifier = Modifier
            .size(80.dp)
            .scale(scale)
            .background(
                color = SendraTeal,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "Success",
            modifier = Modifier.size(48.dp),
            tint = Color.White
        )
    }
}

@Composable
private fun EnhancedFailedContent(
    errorMessage: String,
    canResume: Boolean,
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    var shake by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(100)
        shake = true
        delay(500)
        shake = false
    }
    
    val shakeOffset by animateFloatAsState(
        targetValue = if (shake) 10f else 0f,
        animationSpec = spring(stiffness = 1000f, dampingRatio = 0.2f),
        label = "shake"
    )
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                translationX = shakeOffset * if (shake) kotlin.random.Random.nextFloat() - 0.5f else 0f
            }
        ) {
            // Error icon with pulse
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = "Error",
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Transfer Failed",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (canResume) {
                    Button(
                        onClick = onRetry,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Retry")
                    }
                }
                
                OutlinedButton(
                    onClick = onCancel,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
private fun EnhancedCancelledContent(onBack: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(100)
        visible = true
    }
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + slideInVertically { it / 3 }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Cancel,
                        contentDescription = "Cancelled",
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Transfer Cancelled",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "The transfer was cancelled by user",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = onBack,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.width(160.dp)
                ) {
                    Text("Back")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransferTopBar(
    title: String,
    onClose: () -> Unit,
    canClose: Boolean
) {
    TopAppBar(
        title = { 
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        },
        navigationIcon = {
            if (canClose) {
                SendraIconButton(
                    onClick = onClose,
                    icon = Icons.Default.Close,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surface
        )
    )
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1_000_000_000 -> "%.2f GB".format(bytes / 1_000_000_000.0)
        bytes >= 1_000_000 -> "%.2f MB".format(bytes / 1_000_000.0)
        bytes >= 1_000 -> "%.2f KB".format(bytes / 1_000.0)
        else -> "$bytes B"
    }
}
