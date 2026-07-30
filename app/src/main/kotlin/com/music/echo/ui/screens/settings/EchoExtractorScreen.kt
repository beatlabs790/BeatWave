package com.music.echo.ui.screens.settings

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.music.echo.utils.cipher.CipherHistoryStore
import com.music.echo.utils.cipher.PlayerConfigStore
import com.music.echo.utils.cipher.PlayerDatesStore
import iad1tya.echo.music.LocalPlayerAwareWindowInsets
import iad1tya.echo.music.ui.component.IconButton
import iad1tya.echo.music.ui.utils.backToMain
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EchoExtractorScreen(navController: NavController) {
    val lastConfigFetchMs by PlayerConfigStore.lastFetchTimeMs.collectAsStateWithLifecycle()
    val lastDatesFetchMs by PlayerDatesStore.lastFetchTimeMs.collectAsStateWithLifecycle()
    val updateHistory by CipherHistoryStore.history.collectAsStateWithLifecycle()
    
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    
    val now = System.currentTimeMillis()
    val maxTime = if (lastConfigFetchMs != null && lastDatesFetchMs != null) {
        maxOf(lastConfigFetchMs!!, lastDatesFetchMs!!)
    } else {
        lastConfigFetchMs ?: lastDatesFetchMs
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Echo Extractor") },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain
                    ) {
                        Icon(
                            painterResource(iad1tya.echo.music.R.drawable.arrow_back),
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal))
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // Icon
            Icon(
                painter = painterResource(iad1tya.echo.music.R.drawable.sync),
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Countdown / Last Updated
            val timeString = if (maxTime != null) {
                DateUtils.getRelativeTimeSpanString(maxTime, now, DateUtils.MINUTE_IN_MILLIS)
            } else {
                "Never updated"
            }
            
            val nextUpdateStr = if (maxTime != null) {
                val nextTime = maxTime + (6 * 60 * 60 * 1000L)
                if (nextTime > now) {
                    val remainingMs = nextTime - now
                    val hours = remainingMs / (1000 * 60 * 60)
                    val mins = (remainingMs % (1000 * 60 * 60)) / (1000 * 60)
                    "Next auto check in: ${hours}h ${mins}m"
                } else {
                    "Next auto check in: very soon"
                }
            } else {
                ""
            }
            
            Text(
                text = "Last updated: $timeString",
                style = MaterialTheme.typography.titleMedium
            )
            
            if (nextUpdateStr.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = nextUpdateStr,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Update Button
            Button(
                onClick = {
                    if (isRefreshing) return@Button
                    isRefreshing = true
                    scope.launch {
                        PlayerConfigStore.forceManualRefresh()
                        PlayerDatesStore.forceManualRefresh()
                        // Ensure we log the manual check
                        CipherHistoryStore.recordUpdate(System.currentTimeMillis())
                        isRefreshing = false
                    }
                },
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Check for Updates")
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // History List
            Text(
                text = "Update History",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.align(Alignment.Start)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
                items(updateHistory) { timestamp ->
                    HistoryItem(timestamp)
                }
                
                if (updateHistory.isEmpty()) {
                    item {
                        Text(
                            text = "No history available.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryItem(timestamp: Long) {
    val sdf = remember { SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()) }
    val dateStr = remember(timestamp) { sdf.format(Date(timestamp)) }
    val relStr = remember(timestamp) {
        DateUtils.getRelativeTimeSpanString(timestamp, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Cipher updated",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = dateStr,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = relStr,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
