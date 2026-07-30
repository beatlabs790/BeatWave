package iad1tya.echo.music.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import iad1tya.echo.music.LocalPlayerAwareWindowInsets
import iad1tya.echo.music.R
import iad1tya.echo.music.fonts.AppFont
import iad1tya.echo.music.ui.component.EmptyPlaceholder
import iad1tya.echo.music.ui.component.IconButton as AppIconButton
import iad1tya.echo.music.ui.utils.backToMain

/**
 * Browsable Google Fonts catalog. Families are downloaded in every weight the app's typography
 * can use, so applying one afterwards is instant and offline.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FontsBrowseScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    snackbarHostState: SnackbarHostState? = null,
    viewModel: FontsViewModel = rememberFontsViewModel(navController),
) {
    val context = LocalContext.current
    val catalog by viewModel.catalog.collectAsState()
    val loading by viewModel.catalogLoading.collectAsState()
    val progress by viewModel.downloadProgress.collectAsState()
    val message by viewModel.message.collectAsState()

    var query by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) {
        if (catalog.isEmpty()) viewModel.loadCatalog()
    }

    LaunchedEffect(message) {
        val pending = message ?: return@LaunchedEffect
        // Resolved here rather than in the ViewModel so it follows the app's language.
        val text = pending.formatArg
            ?.let { context.getString(pending.messageRes, it) }
            ?: context.getString(pending.messageRes)
        snackbarHostState?.showSnackbar(text)
        viewModel.consumeMessage()
    }

    val results = remember(catalog, query) {
        if (query.isBlank()) catalog
        else catalog.filter { it.name.contains(query.trim(), ignoreCase = true) }
    }

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal)
            ),
    ) {
        Spacer(
            Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)
            )
        )

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(stringResource(R.string.fonts_search_placeholder)) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )

        when {
            loading && catalog.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            results.isEmpty() -> {
                EmptyPlaceholder(
                    icon = R.drawable.search,
                    text = stringResource(R.string.fonts_no_results),
                )
            }

            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(results, key = { it.id }) { font ->
                        CatalogFontRow(
                            font = font,
                            progress = progress[font.id],
                            onDownload = { viewModel.download(font) },
                        )
                    }

                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }

    TopAppBar(
        title = { Text(stringResource(R.string.fonts_browse_google_fonts)) },
        navigationIcon = {
            AppIconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        },
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun CatalogFontRow(
    font: AppFont,
    progress: Float?,
    onDownload: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = font.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                font.category?.let { category ->
                    Text(
                        text = category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }

            when {
                progress != null -> {
                    // Indeterminate until the first face reports in, then it tracks the faces.
                    if (progress <= 0f) {
                        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                }

                font.installed -> {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = stringResource(R.string.fonts_installed_title),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }

                else -> {
                    IconButton(onClick = onDownload) {
                        Icon(
                            Icons.Rounded.Download,
                            contentDescription = stringResource(R.string.fonts_download),
                        )
                    }
                }
            }
        }
    }
}

/**
 * View model shared by both fonts screens, scoped to the settings entry rather than to this one.
 *
 * Scoped to this screen it would be cleared the moment the user navigates back, cancelling any
 * download still in flight and leaving a half-installed family behind. Hanging it off the parent
 * entry also means the installed list on the settings screen reflects a download as it lands.
 */
@Composable
private fun rememberFontsViewModel(navController: NavController): FontsViewModel {
    val parentEntry = remember(navController) {
        runCatching { navController.getBackStackEntry(FONTS_SETTINGS_ROUTE) }.getOrNull()
    }
    return if (parentEntry != null) hiltViewModel(parentEntry) else hiltViewModel()
}
