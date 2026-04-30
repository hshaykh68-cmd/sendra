package com.sendra.ui.screens.settings

import android.os.Build
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            SettingsTopBar(onBack = onNavigateBack)
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Device Info Section
            SettingsSection(title = "Device") {
                DeviceInfoCard(
                    deviceName = uiState.deviceName,
                    onEditName = viewModel::updateDeviceName
                )
            }
            
            // Transfer Settings
            SettingsSection(title = "Transfer") {
                SettingsCard {
                    // Auto-accept transfers
                    SettingsSwitchItem(
                        icon = Icons.Default.CheckCircle,
                        iconTint = MaterialTheme.colorScheme.tertiary,
                        title = "Auto-accept from trusted devices",
                        subtitle = "Skip confirmation for known devices",
                        checked = uiState.autoAcceptFromTrusted,
                        onCheckedChange = viewModel::setAutoAcceptFromTrusted
                    )
                    
                    SettingsDivider()
                    
                    // Default download location
                    SettingsClickableItem(
                        icon = Icons.Default.Folder,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = "Download location",
                        subtitle = uiState.downloadLocation,
                        onClick = viewModel::changeDownloadLocation
                    )
                    
                    SettingsDivider()
                    
                    // Chunk size
                    SettingsClickableItem(
                        icon = Icons.Default.Storage,
                        iconTint = MaterialTheme.colorScheme.secondary,
                        title = "Transfer chunk size",
                        subtitle = uiState.chunkSize,
                        onClick = viewModel::changeChunkSize
                    )
                }
            }
            
            // Network Settings
            SettingsSection(title = "Network") {
                SettingsCard {
                    // Preferred connection method
                    SettingsClickableItem(
                        icon = Icons.Default.Wifi,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = "Connection method",
                        subtitle = uiState.connectionMethod,
                        onClick = viewModel::changeConnectionMethod
                    )
                    
                    SettingsDivider()
                    
                    // Discovery visibility
                    SettingsSwitchItem(
                        icon = Icons.Default.Visibility,
                        iconTint = MaterialTheme.colorScheme.secondary,
                        title = "Visible to others",
                        subtitle = "Allow other devices to discover you",
                        checked = uiState.isDiscoveryEnabled,
                        onCheckedChange = viewModel::setDiscoveryEnabled
                    )
                }
            }
            
            // Appearance
            SettingsSection(title = "Appearance") {
                SettingsCard {
                    // Dark mode
                    SettingsClickableItem(
                        icon = Icons.Default.DarkMode,
                        iconTint = MaterialTheme.colorScheme.secondary,
                        title = "Theme",
                        subtitle = uiState.themeMode,
                        onClick = viewModel::changeTheme
                    )
                    
                    SettingsDivider()
                    
                    // Dynamic colors (Android 12+)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        SettingsSwitchItem(
                            icon = Icons.Default.Palette,
                            iconTint = MaterialTheme.colorScheme.tertiary,
                            title = "Dynamic colors",
                            subtitle = "Use system accent colors",
                            checked = uiState.useDynamicColors,
                            onCheckedChange = viewModel::setDynamicColors
                        )
                    }
                }
            }
            
            // Notifications
            SettingsSection(title = "Notifications") {
                SettingsCard {
                    SettingsSwitchItem(
                        icon = Icons.Default.Notifications,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = "Transfer notifications",
                        subtitle = "Show progress in notification bar",
                        checked = uiState.showTransferNotifications,
                        onCheckedChange = viewModel::setShowTransferNotifications
                    )
                    
                    SettingsDivider()
                    
                    SettingsSwitchItem(
                        icon = Icons.Default.Vibration,
                        iconTint = MaterialTheme.colorScheme.secondary,
                        title = "Vibration",
                        subtitle = "Haptic feedback on actions",
                        checked = uiState.enableVibration,
                        onCheckedChange = viewModel::setEnableVibration
                    )
                }
            }
            
            // About
            SettingsSection(title = "About") {
                SettingsCard {
                    SettingsClickableItem(
                        icon = Icons.Default.Info,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = "Version",
                        subtitle = uiState.appVersion,
                        onClick = { }
                    )
                    
                    SettingsDivider()
                    
                    SettingsClickableItem(
                        icon = Icons.Default.Description,
                        iconTint = MaterialTheme.colorScheme.secondary,
                        title = "Privacy Policy",
                        subtitle = null,
                        onClick = viewModel::openPrivacyPolicy
                    )
                    
                    SettingsDivider()
                    
                    SettingsClickableItem(
                        icon = Icons.Default.Help,
                        iconTint = MaterialTheme.colorScheme.tertiary,
                        title = "Help & Support",
                        subtitle = null,
                        onClick = viewModel::openHelp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            fontWeight = FontWeight.Bold
        )
        content()
    }
}

@Composable
private fun SettingsCard(
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            content()
        }
    }
}

@Composable
private fun DeviceInfoCard(
    deviceName: String,
    onEditName: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PhoneAndroid,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Device Name",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Text(
                    text = deviceName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            IconButton(onClick = onEditName) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun SettingsSwitchItem(
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = iconTint.copy(alpha = 0.1f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SettingsClickableItem(
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    title: String,
    subtitle: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = iconTint.copy(alpha = 0.1f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun SettingsDivider() {
    Divider(
        modifier = Modifier.padding(start = 72.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsTopBar(onBack: () -> Unit) {
    TopAppBar(
        title = { Text("Settings") },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
        }
    )
}
