package com.beatwave.music.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.beatwave.music.R
import com.beatwave.music.SuggestionRow
import com.beatwave.music.BugReportRow
import com.beatwave.music.api.SupabaseService
import com.beatwave.music.constants.SubmittedSuggestionIdsKey
import com.beatwave.music.constants.SubmittedBugReportIdsKey
import com.beatwave.music.ui.component.IconButton
import com.beatwave.music.ui.utils.appTopBarWindowInsets
import com.beatwave.music.utils.dataStore
import com.beatwave.music.utils.get
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.launch

sealed class HistoryItem {
    abstract val id: Long
    abstract val createdAt: String
    abstract val description: String
    abstract val status: String
    abstract val typeLabel: String

    data class Suggestion(val row: SuggestionRow) : HistoryItem() {
        override val id: Long = row.id ?: 0L
        override val createdAt: String = row.created_at ?: ""
        override val description: String = row.content
        override val status: String = row.status
        override val typeLabel: String = "Suggestion"
    }

    data class Bug(val row: BugReportRow) : HistoryItem() {
        override val id: Long = row.id ?: 0L
        override val createdAt: String = row.created_at ?: ""
        override val description: String = row.description
        override val status: String = row.status
        override val typeLabel: String = "Bug Report"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuggestionScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    // Submission Form State
    var feedbackType by rememberSaveable { mutableStateOf("suggestion") } // "suggestion" or "bug"
    var name by rememberSaveable { mutableStateOf("") }
    var instaId by rememberSaveable { mutableStateOf("") }
    var content by rememberSaveable { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    // History State
    var pastSuggestions by remember { mutableStateOf<List<SuggestionRow>>(emptyList()) }
    var pastBugs by remember { mutableStateOf<List<BugReportRow>>(emptyList()) }
    var isFetchingHistory by remember { mutableStateOf(false) }

    // Fetch history when tab is selected
    LaunchedEffect(selectedTab) {
        if (selectedTab == 1) {
            isFetchingHistory = true
            try {
                val suggestionIdsStr = context.dataStore.get(SubmittedSuggestionIdsKey, "")
                val bugIdsStr = context.dataStore.get(SubmittedBugReportIdsKey, "")

                val suggestionIds = suggestionIdsStr.split(",")
                    .mapNotNull { it.trim().toLongOrNull() }
                val bugIds = bugIdsStr.split(",")
                    .mapNotNull { it.trim().toLongOrNull() }

                val suggestionsResult = SupabaseService.fetchSuggestionsByIds(suggestionIds)
                val bugsResult = SupabaseService.fetchBugReportsByIds(bugIds)

                if (suggestionsResult.isSuccess) {
                    pastSuggestions = suggestionsResult.getOrDefault(emptyList())
                }
                if (bugsResult.isSuccess) {
                    pastBugs = bugsResult.getOrDefault(emptyList())
                }
            } catch (_: Exception) {}
            isFetchingHistory = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = appTopBarWindowInsets(),
                title = { Text("Feedback & Suggestions") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Tab Row
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Submit Feedback") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("My History") }
                )
            }

            if (selectedTab == 0) {
                // Submit feedback layout
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Help us improve BeatWave! Let us know what features you want or report any bugs you find.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Type Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { feedbackType = "suggestion" },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (feedbackType == "suggestion") MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                contentColor = if (feedbackType == "suggestion") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            ),
                            border = ButtonDefaults.outlinedButtonBorder.takeIf { feedbackType != "suggestion" }
                        ) {
                            Text("Suggestion")
                        }

                        OutlinedButton(
                            onClick = { feedbackType = "bug" },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (feedbackType == "bug") MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                contentColor = if (feedbackType == "bug") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            ),
                            border = ButtonDefaults.outlinedButtonBorder.takeIf { feedbackType != "bug" }
                        ) {
                            Text("Bug Report")
                        }
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Your Name (Required)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = instaId,
                        onValueChange = { instaId = it },
                        label = { Text("Instagram ID (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text(if (feedbackType == "suggestion") "Describe your suggestion..." else "Describe the bug/problem...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 6
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                Toast.makeText(context, "Please write your name first", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (content.isBlank()) {
                                Toast.makeText(context, "Please write a description first", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isSubmitting = true
                            coroutineScope.launch {
                                val success: Boolean
                                if (feedbackType == "suggestion") {
                                    val result = SupabaseService.submitSuggestion(
                                        userName = name,
                                        instaId = instaId.ifBlank { null },
                                        content = content
                                    )
                                    success = result.isSuccess
                                    if (result.isSuccess) {
                                        val generatedId = result.getOrNull()
                                        if (generatedId != null) {
                                            val currentIds = context.dataStore.get(SubmittedSuggestionIdsKey, "")
                                            val newIds = if (currentIds.isBlank()) "$generatedId" else "$currentIds,$generatedId"
                                            context.dataStore.edit { it[SubmittedSuggestionIdsKey] = newIds }
                                        }
                                    } else {
                                        Toast.makeText(context, "Failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                    }
                                } else {
                                    val deviceInfo = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (Android ${android.os.Build.VERSION.RELEASE}, API ${android.os.Build.VERSION.SDK_INT})"
                                    val result = SupabaseService.submitBugReport(
                                        userName = name,
                                        instaId = instaId.ifBlank { null },
                                        description = content,
                                        deviceInfo = deviceInfo
                                    )
                                    success = result.isSuccess
                                    if (result.isSuccess) {
                                        val generatedId = result.getOrNull()
                                        if (generatedId != null) {
                                            val currentIds = context.dataStore.get(SubmittedBugReportIdsKey, "")
                                            val newIds = if (currentIds.isBlank()) "$generatedId" else "$currentIds,$generatedId"
                                            context.dataStore.edit { it[SubmittedBugReportIdsKey] = newIds }
                                        }
                                    } else {
                                        Toast.makeText(context, "Failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                                isSubmitting = false
                                if (success) {
                                    Toast.makeText(context, "Thank you! Submitted successfully.", Toast.LENGTH_LONG).show()
                                    content = ""
                                    selectedTab = 1
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSubmitting,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(if (feedbackType == "suggestion") "Submit Suggestion" else "Submit Bug Report")
                        }
                    }
                }
            } else {
                // My History layout
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = if (isFetchingHistory) Arrangement.Center else Arrangement.spacedBy(12.dp)
                ) {
                    if (isFetchingHistory) {
                        CircularProgressIndicator()
                    } else {
                        val historyItems = (pastSuggestions.map { HistoryItem.Suggestion(it) } +
                                pastBugs.map { HistoryItem.Bug(it) })
                            .sortedByDescending { it.createdAt }

                        if (historyItems.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 64.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "You haven't submitted any feedback yet.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            historyItems.forEach { item ->
                                HistoryCard(item = item)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryCard(item: HistoryItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Type Header Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (item is HistoryItem.Bug) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = item.typeLabel,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (item is HistoryItem.Bug) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Status Badge
                val statusColor = when (item.status.lowercase()) {
                    "done", "fixed" -> Color(0xFF34C759)
                    "reviewed", "investigating" -> Color(0xFF007AFF)
                    "rejected", "closed" -> Color(0xFFFF3B30)
                    else -> Color(0xFFFF9500)
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = item.status.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = item.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )

            // Date
            val displayDate = if (item.createdAt.length >= 10) item.createdAt.substring(0, 10) else item.createdAt
            Text(
                text = "Submitted on: $displayDate",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
