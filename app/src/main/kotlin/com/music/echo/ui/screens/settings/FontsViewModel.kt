package iad1tya.echo.music.ui.screens.settings

import android.content.Context
import android.net.Uri
import androidx.annotation.StringRes
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import iad1tya.echo.music.R
import iad1tya.echo.music.fonts.AppFont
import iad1tya.echo.music.fonts.FontTarget
import iad1tya.echo.music.fonts.FontManager
import iad1tya.echo.music.fonts.FontException
import iad1tya.echo.music.fonts.FontRepository
import iad1tya.echo.music.utils.dataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Something to tell the user, kept as a resource id so it is resolved against the app's locale by
 * whichever screen shows it.
 */
data class FontMessage(
    @StringRes val messageRes: Int,
    val formatArg: String? = null,
)

/** Failures we raised carry their own message; anything else falls back to [fallbackRes]. */
private fun Throwable.toFontMessage(@StringRes fallbackRes: Int): FontMessage =
    if (this is FontException) FontMessage(messageRes, formatArg) else FontMessage(fallbackRes)

@HiltViewModel
class FontsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: FontRepository,
) : ViewModel() {

    private val _installedFonts = MutableStateFlow<List<AppFont>>(emptyList())
    val installedFonts: StateFlow<List<AppFont>> = _installedFonts.asStateFlow()

    private val _storageUsed = MutableStateFlow(0L)
    val storageUsed: StateFlow<Long> = _storageUsed.asStateFlow()

    private val _catalog = MutableStateFlow<List<AppFont>>(emptyList())
    val catalog: StateFlow<List<AppFont>> = _catalog.asStateFlow()

    private val _catalogLoading = MutableStateFlow(false)
    val catalogLoading: StateFlow<Boolean> = _catalogLoading.asStateFlow()

    /** Font id -> 0f..1f, for the families currently being downloaded. */
    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Float>> = _downloadProgress.asStateFlow()

    /** One-shot user-facing message; the screen resolves and clears it once it has been shown. */
    private val _message = MutableStateFlow<FontMessage?>(null)
    val message: StateFlow<FontMessage?> = _message.asStateFlow()

    init {
        refreshInstalled()
    }

    fun refreshInstalled() {
        viewModelScope.launch {
            val fonts = withContext(Dispatchers.IO) { repository.installedFonts() }
            val size = withContext(Dispatchers.IO) { repository.totalInstalledSize() }
            _installedFonts.value = fonts
            _storageUsed.value = size
        }
    }

    fun loadCatalog(forceRefresh: Boolean = false) {
        if (_catalogLoading.value) return
        viewModelScope.launch {
            _catalogLoading.value = true
            try {
                _catalog.value = repository.search(query = "", forceRefresh = forceRefresh)
            } catch (e: Exception) {
                _message.value = e.toFontMessage(R.string.fonts_error_catalog)
            } finally {
                _catalogLoading.value = false
            }
        }
    }

    fun download(font: AppFont) {
        if (font.id in _downloadProgress.value) return
        viewModelScope.launch {
            _downloadProgress.update { it + (font.id to 0f) }
            val result = repository.download(font) { progress ->
                _downloadProgress.update { it + (font.id to progress) }
            }
            _downloadProgress.update { it - font.id }

            result
                .onSuccess { installed ->
                    _message.value = FontMessage(R.string.fonts_msg_installed, installed.name)
                    markInstalledInCatalog(installed.id, installed.sizeBytes)
                    refreshInstalled()
                }
                .onFailure { error ->
                    _message.value = error.toFontMessage(R.string.fonts_error_download_generic)
                }
        }
    }

    fun import(uri: Uri) {
        viewModelScope.launch {
            repository.importFromUri(uri)
                .onSuccess { font ->
                    _message.value = FontMessage(R.string.fonts_msg_imported, font.name)
                    refreshInstalled()
                }
                .onFailure { error ->
                    _message.value = error.toFontMessage(R.string.fonts_error_import_generic)
                }
        }
    }

    fun delete(font: AppFont) {
        viewModelScope.launch {
            repository.delete(font.id)
                .onSuccess {
                    // Any target still pointing at the removed family would fall back to an
                    // arbitrary font, so send them all back to their default.
                    resetTargetsUsing(font.id)
                    _message.value = FontMessage(R.string.fonts_msg_removed, font.name)
                    markUninstalledInCatalog(font.id)
                    refreshInstalled()
                }
                .onFailure { error ->
                    _message.value = error.toFontMessage(R.string.fonts_error_remove_generic)
                }
        }
    }

    fun applyFont(target: FontTarget, fontId: String) {
        viewModelScope.launch {
            FontManager.invalidate(fontId)
            context.dataStore.edit { it[target.preferenceKey] = fontId }
        }
    }

    fun consumeMessage() {
        _message.value = null
    }

    private suspend fun resetTargetsUsing(fontId: String) {
        context.dataStore.edit { preferences ->
            FontTarget.entries.forEach { target ->
                if (preferences[target.preferenceKey] == fontId) {
                    preferences[target.preferenceKey] = target.defaultId
                }
            }
        }
    }

    private fun markInstalledInCatalog(fontId: String, sizeBytes: Long) {
        _catalog.update { fonts ->
            fonts.map { if (it.id == fontId) it.copy(installed = true, sizeBytes = sizeBytes) else it }
        }
    }

    private fun markUninstalledInCatalog(fontId: String) {
        _catalog.update { fonts ->
            fonts.map { if (it.id == fontId) it.copy(installed = false, sizeBytes = 0L) else it }
        }
    }
}
