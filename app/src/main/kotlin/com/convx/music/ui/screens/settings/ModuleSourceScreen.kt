/**
 * BeatWave Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.convx.music.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.convx.music.LocalPlayerAwareWindowInsets
import com.convx.music.R
import com.convx.music.constants.EnabledModulesKey
import com.convx.music.constants.FetchedModulesKey
import com.convx.music.constants.ModuleSourcesKey
import com.convx.music.ui.component.IconButton
import com.convx.music.ui.component.ModernSwitch
import com.convx.music.ui.theme.AppleTokens
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.ui.utils.backToMain
import com.convx.music.utils.rememberPreference
import com.music.spine.ModuleManager
import com.music.spine.SpineModule
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.json.JSONArray

private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleSourceScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val scope = rememberCoroutineScope()
    val moduleManager = remember { ModuleManager() }

    val (sourcesJson, onSourcesJsonChange) = rememberPreference(ModuleSourcesKey, defaultValue = "[]")
    val (enabledJson, onEnabledJsonChange) = rememberPreference(EnabledModulesKey, defaultValue = "[]")

    val sourceUrls = remember(sourcesJson) {
        runCatching {
            val arr = JSONArray(sourcesJson)
            (0 until arr.length()).map { arr.getString(it) }
        }.getOrElse { emptyList() }
    }

    val enabledIds = remember(enabledJson) {
        runCatching {
            val arr = JSONArray(enabledJson)
            (0 until arr.length()).map { arr.getString(it) }.toSet()
        }.getOrElse { emptySet<String>() }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    val (fetchedModulesJson, onFetchedModulesJsonChange) = rememberPreference(FetchedModulesKey, defaultValue = "[]")
    val fetchedModules = remember(fetchedModulesJson) {
        runCatching {
            json.decodeFromString<List<SpineModule>>(fetchedModulesJson)
        }.getOrElse { emptyList() }
    }
    var isLoading by remember { mutableStateOf(false) }
    var loadingSource by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun saveSources(urls: List<String>) {
        val arr = JSONArray()
        urls.forEach { arr.put(it) }
        onSourcesJsonChange(arr.toString())
    }

    fun toggleModule(moduleId: String) {
        val current = enabledIds.toMutableSet()
        if (current.contains(moduleId)) current.remove(moduleId) else current.add(moduleId)
        val arr = JSONArray()
        current.forEach { arr.put(it) }
        onEnabledJsonChange(arr.toString())
    }

    fun removeSource(url: String) {
        saveSources(sourceUrls.filter { it != url })
    }

    Column(
        Modifier
            .windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal)
            )
            .fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom)
                )
        ) {
            item {
                Spacer(
                    Modifier.windowInsetsPadding(
                        LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)
                    )
                )
                Text(
                    text = stringResource(R.string.module_sources),
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 8.dp)
                )
            }

            item {
                Text(
                    text = "SOURCES",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 8.dp)
                )
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(AppleTokens.CardCorner))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    sourceUrls.forEachIndexed { index, url ->
                        if (index > 0) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 64.dp),
                                color = MaterialTheme.colorScheme.outlineVariant,
                                thickness = 0.5.dp,
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = url.substringAfter("://").take(40),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = url,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            IconButton(
                                onClick = { removeSource(url) },
                                onLongClick = { removeSource(url) },
                            ) {
                                Icon(
                                    painterResource(R.drawable.close),
                                    contentDescription = stringResource(R.string.remove),
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAddDialog = true }
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painterResource(R.drawable.add),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.add_source),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            if (sourceUrls.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(16.dp))
                    TextButton(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                errorMessage = null
                                val allModules = mutableListOf<SpineModule>()
                                for (url in sourceUrls) {
                                    loadingSource = url
                                    moduleManager.fetchIndex(url).onSuccess { allModules.addAll(it) }
                                }
                                onFetchedModulesJsonChange(
                                    json.encodeToString(
                                            kotlinx.serialization.builtins.ListSerializer(SpineModule.serializer()),
                                            allModules.distinctBy { it.id }
                                        )
                                )
                                isLoading = false
                                loadingSource = null
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        enabled = !isLoading,
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Fetching from ${loadingSource?.substringAfter("://")?.take(30) ?: "..."}")
                        } else {
                            Text(stringResource(R.string.fetch_modules))
                        }
                    }
                }
            }

            if (fetchedModules.isNotEmpty()) {
                item {
                    Text(
                        text = "AVAILABLE MODULES",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp)
                    )
                }

                items(fetchedModules, key = { it.id }) { module ->
                    val isEnabled = enabledIds.contains(module.id)
                    ModuleItem(
                        module = module,
                        isEnabled = isEnabled,
                        onToggle = { toggleModule(module.id) },
                        onClick = {
                            navController.navigate("settings/modules/${module.id}")
                        },
                    )
                }
            }

            if (fetchedModules.isEmpty() && !isLoading && sourceUrls.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.no_modules_found),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)
                    )
                }
            }

            item {
                Spacer(Modifier.height(80.dp))
            }
        }
    }

    if (showAddDialog) {
        var urlInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(stringResource(R.string.add_module_source)) },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.add_module_source_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        singleLine = true,
                        label = { Text(stringResource(R.string.source_url)) },
                        placeholder = { Text("https://example.com/index.json") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (urlInput.isNotBlank()) {
                            saveSources(sourceUrls + urlInput.trim())
                            showAddDialog = false
                        }
                    },
                    enabled = urlInput.isNotBlank(),
                ) {
                    Text(stringResource(R.string.add_to_an_playlist))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    TopAppBar(
        windowInsets = appTopBarWindowInsets(),
        title = { Text(stringResource(R.string.module_sources)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        },
    )
}

@Composable
private fun ModuleItem(
    module: SpineModule,
    isEnabled: Boolean,
    onToggle: () -> Unit,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(AppleTokens.CardCorner))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = module.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (module.isLossless) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "LOSSLESS",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${module.author} v${module.version}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (module.description.isNotBlank()) {
                Text(
                    text = module.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (module.tags.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    module.tags.take(4).forEach { tag ->
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }
        }

        ModernSwitch(
            checked = isEnabled,
            onCheckedChange = { onToggle() },
        )
    }
}
