package iad1tya.echo.music.ui.screens.settings

import android.content.Context
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FontDownload
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import iad1tya.echo.music.LocalPlayerAwareWindowInsets
import iad1tya.echo.music.R
import iad1tya.echo.music.fonts.AppFont
import iad1tya.echo.music.fonts.FontTarget
import iad1tya.echo.music.fonts.rememberFontFamily
import iad1tya.echo.music.ui.component.IconButton as AppIconButton
import iad1tya.echo.music.ui.component.Material3SettingsGroup
import iad1tya.echo.music.ui.component.Material3SettingsItem
import iad1tya.echo.music.ui.component.DefaultDialog
import iad1tya.echo.music.ui.utils.backToMain
import iad1tya.echo.music.utils.rememberPreference

internal const val FONTS_SETTINGS_ROUTE = "settings/appearance/fonts"
internal const val FONTS_BROWSE_ROUTE = "settings/appearance/fonts/browse"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FontsSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    snackbarHostState: SnackbarHostState? = null,
    viewModel: FontsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val installedFonts by viewModel.installedFonts.collectAsState()
    val storageUsed by viewModel.storageUsed.collectAsState()
    val message by viewModel.message.collectAsState()

    val (appFontId) = rememberPreference(FontTarget.APP.preferenceKey, FontTarget.APP.defaultId)
    val (lyricsFontId) = rememberPreference(FontTarget.LYRICS.preferenceKey, FontTarget.LYRICS.defaultId)
    val (playerFontId) = rememberPreference(FontTarget.PLAYER.preferenceKey, FontTarget.PLAYER.defaultId)

    var pickerTarget by remember { mutableStateOf<FontTarget?>(null) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) viewModel.import(uri)
    }

    LaunchedEffect(Unit) {
        viewModel.refreshInstalled()
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

    fun selectedIdFor(target: FontTarget) = when (target) {
        FontTarget.APP -> appFontId
        FontTarget.LYRICS -> lyricsFontId
        FontTarget.PLAYER -> playerFontId
    }

    Column(
        Modifier
            .windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal)
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(
            Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)
            )
        )

        Material3SettingsGroup(
            title = stringResource(R.string.fonts_apply_to_title),
            items = FontTarget.entries.map { target ->
                Material3SettingsItem(
                    customIcon = { SettingsIcon(target.icon) },
                    title = { Text(stringResource(target.labelRes)) },
                    description = {
                        Text(
                            fontLabel(
                                fontId = selectedIdFor(target),
                                installedFonts = installedFonts,
                                inheritable = target.inheritable,
                            )
                        )
                    },
                    onClick = { pickerTarget = target },
                )
            },
        )

        Spacer(Modifier.height(27.dp))

        Material3SettingsGroup(
            title = stringResource(R.string.fonts_add_title),
            items = listOf(
                Material3SettingsItem(
                    customIcon = { SettingsIcon(Icons.Rounded.FontDownload) },
                    title = { Text(stringResource(R.string.fonts_browse_google_fonts)) },
                    description = { Text(stringResource(R.string.fonts_browse_google_fonts_desc)) },
                    onClick = { navController.navigate(FONTS_BROWSE_ROUTE) },
                ),
                Material3SettingsItem(
                    customIcon = { SettingsIcon(Icons.Rounded.Upload) },
                    title = { Text(stringResource(R.string.fonts_import)) },
                    description = { Text(stringResource(R.string.fonts_import_desc)) },
                    onClick = {
                        // Many file providers report TTF/OTF as octet-stream, so the generic
                        // types stay in the filter; the file itself is validated after copying.
                        importLauncher.launch(
                            arrayOf(
                                "font/ttf",
                                "font/otf",
                                "application/x-font-ttf",
                                "application/octet-stream",
                                "*/*",
                            )
                        )
                    },
                ),
            ),
        )

        Spacer(Modifier.height(27.dp))

        Material3SettingsGroup(
            title = stringResource(R.string.fonts_installed_title),
            items = if (installedFonts.isEmpty()) {
                listOf(
                    Material3SettingsItem(
                        customIcon = { SettingsIcon(Icons.Rounded.TextFields) },
                        title = { Text(stringResource(R.string.fonts_none_installed)) },
                        description = { Text(stringResource(R.string.fonts_none_installed_desc)) },
                    )
                )
            } else {
                installedFonts.map { font ->
                    Material3SettingsItem(
                        customIcon = { SettingsIcon(Icons.Rounded.TextFields) },
                        title = { FontPreviewTitle(font) },
                        description = {
                            FontPreviewSubtitle(font, storageLabel(context, font.sizeBytes))
                        },
                        trailingContent = {
                            IconButton(onClick = { viewModel.delete(font) }) {
                                Icon(
                                    Icons.Rounded.Delete,
                                    contentDescription = stringResource(R.string.fonts_delete),
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        },
                    )
                }
            },
        )

        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.fonts_storage_used),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = pluralStringResource(
                        R.plurals.fonts_storage_used_value,
                        installedFonts.size,
                        installedFonts.size,
                        storageLabel(context, storageUsed),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }

        Spacer(Modifier.height(27.dp))
    }

    pickerTarget?.let { target ->
        FontPickerDialog(
            target = target,
            selectedId = selectedIdFor(target),
            installedFonts = installedFonts,
            onSelect = { fontId ->
                viewModel.applyFont(target, fontId)
                pickerTarget = null
            },
            onDismiss = { pickerTarget = null },
        )
    }

    TopAppBar(
        title = { Text(stringResource(R.string.fonts_title)) },
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
private fun FontPickerDialog(
    target: FontTarget,
    selectedId: String,
    installedFonts: List<AppFont>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    // "Same as app" only makes sense for the targets that can defer to it.
    val choices = buildList {
        if (target.inheritable) add(AppFont.INHERIT_ID to stringResource(R.string.fonts_same_as_app))
        add(AppFont.SYSTEM_ID to stringResource(R.string.fonts_system_default))
        installedFonts.forEach { add(it.id to it.name) }
    }

    DefaultDialog(
        onDismiss = onDismiss,
        title = { Text(stringResource(target.labelRes)) }
    ) {
        LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
            items(choices, key = { it.first }) { (id, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(id) }
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = id == selectedId,
                        onClick = null,
                    )
                    Text(
                        text = label,
                        fontFamily = rememberFontFamily(id),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 16.dp),
                    )
                }
            }
        }
    }
}

/** Human-readable name of whatever [fontId] points at. */
@Composable
private fun fontLabel(
    fontId: String,
    installedFonts: List<AppFont>,
    inheritable: Boolean,
): String = when {
    fontId == AppFont.INHERIT_ID && inheritable -> stringResource(R.string.fonts_same_as_app)
    fontId == AppFont.SYSTEM_ID -> stringResource(R.string.fonts_system_default)
    else -> installedFonts.firstOrNull { it.id == fontId }?.name
        ?: stringResource(R.string.fonts_missing)
}

private val FontTarget.labelRes: Int
    get() = when (this) {
        FontTarget.APP -> R.string.fonts_target_app
        FontTarget.LYRICS -> R.string.fonts_target_lyrics
        FontTarget.PLAYER -> R.string.fonts_target_player
    }

private val FontTarget.icon: ImageVector
    get() = when (this) {
        FontTarget.APP -> Icons.Rounded.TextFields
        FontTarget.LYRICS -> Icons.Rounded.Lyrics
        FontTarget.PLAYER -> Icons.Rounded.PlayCircle
    }

@Composable
private fun FontPreviewTitle(font: AppFont) {
    Text(
        text = font.name,
        fontFamily = rememberFontFamily(font.id),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun FontPreviewSubtitle(font: AppFont, sizeLabel: String) {
    val family = rememberFontFamily(font.id)
    Column {
        Text(
            text = stringResource(R.string.fonts_preview_pangram),
            fontFamily = family,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = pluralStringResource(
                R.plurals.fonts_variant_summary,
                font.variants.size,
                font.variants.size,
                sizeLabel,
            ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

internal fun storageLabel(context: Context, bytes: Long): String =
    Formatter.formatShortFileSize(context, bytes)

/** Settings-row icon, sized and tinted like the painter-based ones in the rest of settings. */
@Composable
internal fun SettingsIcon(imageVector: ImageVector, contentDescription: String? = null) {
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(24.dp),
    )
}
