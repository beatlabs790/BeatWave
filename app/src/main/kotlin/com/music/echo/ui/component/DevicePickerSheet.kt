package iad1tya.echo.music.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import iad1tya.echo.music.R
import iad1tya.echo.music.playback.RemoteControlClient
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicePickerSheet(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val discoveredDevices by RemoteControlClient.discoveredDevices.collectAsState()
    val connectedIp by RemoteControlClient.currentConnectedDeviceIp.collectAsState()

    var manualIp by remember { mutableStateOf("") }
    var isConnecting by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        RemoteControlClient.startDiscovery(context)
    }

    ModalBottomSheet(
        onDismissRequest = {
            RemoteControlClient.stopDiscovery()
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Connect to a device",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = {
                    RemoteControlClient.stopDiscovery()
                    RemoteControlClient.startDiscovery(context)
                }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Local device
                item {
                    val isLocalSelected = connectedIp == null
                    ListItem(
                        headlineContent = { Text("This Phone") },
                        supportingContent = { Text("Local Playback") },
                        leadingContent = {
                            Icon(
                                painter = painterResource(R.drawable.cast_speaker),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = if (isLocalSelected) MaterialTheme.colorScheme.primary else LocalContentColor.current
                            )
                        },
                        trailingContent = if (isLocalSelected) {
                            { Icon(Icons.Default.Check, contentDescription = "Active", tint = MaterialTheme.colorScheme.primary) }
                        } else null,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                scope.launch {
                                    RemoteControlClient.disconnect()
                                }
                            },
                        colors = ListItemDefaults.colors(
                            containerColor = if (isLocalSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else Color.Transparent
                        )
                    )
                }

                item {
                    Text(
                        text = "Discovered Devices",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                }

                if (discoveredDevices.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Scanning same Wi-Fi for BeatWave devices...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(discoveredDevices) { device ->
                        val isConnected = connectedIp == device.ip
                        ListItem(
                            headlineContent = { Text(device.name) },
                            supportingContent = { Text("Wi-Fi Connection • ${device.ip}") },
                            leadingContent = {
                                Icon(
                                    painter = painterResource(R.drawable.cast),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = if (isConnected) MaterialTheme.colorScheme.primary else LocalContentColor.current
                                )
                            },
                            trailingContent = if (isConnected) {
                                { Icon(Icons.Default.Check, contentDescription = "Connected", tint = MaterialTheme.colorScheme.primary) }
                            } else null,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    scope.launch {
                                        isConnecting = true
                                        val success = RemoteControlClient.connect(device.ip, device.port)
                                        isConnecting = false
                                        if (success) {
                                            sheetState.hide()
                                            onDismiss()
                                        }
                                    }
                                },
                            colors = ListItemDefaults.colors(
                                containerColor = if (isConnected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else Color.Transparent
                            )
                        )
                    }
                }

                // Manual Input Section
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Or Connect Manually",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = manualIp,
                            onValueChange = { manualIp = it },
                            placeholder = { Text("IP Address (e.g. 192.168.1.5)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                if (manualIp.isNotBlank()) {
                                    scope.launch {
                                        isConnecting = true
                                        val success = RemoteControlClient.connect(manualIp)
                                        isConnecting = false
                                        if (success) {
                                            sheetState.hide()
                                            onDismiss()
                                        }
                                    }
                                }
                            },
                            enabled = !isConnecting && manualIp.isNotBlank()
                        ) {
                            if (isConnecting) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Text("Connect")
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
