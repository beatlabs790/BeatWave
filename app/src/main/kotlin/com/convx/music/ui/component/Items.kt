/**
 * BeatWave Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 * 
 * Optimized for minimal recomposition during navigation
 */

package com.convx.music.ui.component

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.zIndex
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.Download.STATE_COMPLETED
import androidx.media3.exoplayer.offline.Download.STATE_DOWNLOADING
import androidx.media3.exoplayer.offline.Download.STATE_QUEUED
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.size.Size as CoilSize
import com.music.innertube.YouTube
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.ArtistItem
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.SongItem
import com.music.innertube.models.YTItem
import com.convx.music.LocalDatabase
import com.convx.music.LocalDownloadUtil
import com.convx.music.LocalPlayerConnection
import com.convx.music.R
import com.convx.music.constants.CropAlbumArtKey
import com.convx.music.constants.GridItemSize
import com.convx.music.constants.GridItemsSizeKey
import com.convx.music.constants.GridThumbnailHeight
import com.convx.music.constants.ListItemHeight
import com.convx.music.constants.ListThumbnailSize
import com.convx.music.constants.SmallGridThumbnailHeight
import com.convx.music.constants.SwipeToSongKey
import com.convx.music.constants.ThumbnailCornerRadius
import com.convx.music.constants.ThumbnailRoundedShape
import com.convx.music.db.entities.Album
import com.convx.music.db.entities.Artist
import com.convx.music.db.entities.Playlist
import com.convx.music.db.entities.Song
import com.convx.music.extensions.toMediaItem
import com.convx.music.models.MediaMetadata
import com.convx.music.playback.queues.LocalAlbumRadio
import com.convx.music.ui.theme.AppleTokens
import com.convx.music.ui.theme.rememberArtworkTint
import com.convx.music.ui.component.shapes.ContinuousRoundedRectangle
import com.convx.music.ui.utils.resize
import com.convx.music.utils.joinByBullet
import com.convx.music.utils.makeTimeString
import com.convx.music.utils.rememberEnumPreference
import com.convx.music.utils.rememberPreference
import com.convx.music.utils.reportException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

const val ActiveBoxAlpha = 0.6f

@Composable
fun currentGridThumbnailHeight(): Dp {
    val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)
    return if (gridItemSize == GridItemSize.BIG) GridThumbnailHeight else SmallGridThumbnailHeight
}

// Basic list item - optimized with inline to reduce recomposition
@Composable
inline fun ListItem(
    modifier: Modifier = Modifier,
    title: String,
    noinline subtitle: (@Composable RowScope.() -> Unit)? = null,
    thumbnailContent: @Composable () -> Unit,
    crossinline trailingContent: @Composable RowScope.() -> Unit = {},
    isSelected: Boolean? = false,
    isActive: Boolean = false,
    isAvailable: Boolean = true,
    shape: Shape = RectangleShape,
    drawHighlight: Boolean = true,
    flat: Boolean = false,
) {
    val onSurface = LocalContentColor.current
    val isLibraryScreen = modifier.toString().contains("Library", ignoreCase = true) || 
                         title.lowercase().let { it == "songs" || it == "artists" || it == "albums" || it == "playlists" }
    
    Column(modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(vertical = 2.dp)
                .height(ListItemHeight)
                .padding(horizontal = 16.dp)
                .clip(if (flat) RectangleShape else shape)
                .background(
                    color = when {
                        isActive -> onSurface.copy(alpha = 0.15f)
                        isSelected == true && drawHighlight -> onSurface.copy(alpha = 0.1f)
                        else -> Color.Transparent
                    }
                )
        ) {
            Box(
                modifier = Modifier.padding(start = 12.dp, top = 6.dp, end = 6.dp, bottom = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                thumbnailContent()
                if (!isAvailable) {
                    Box(
                        modifier = Modifier
                            .size(ListThumbnailSize)
                            .align(Alignment.Center)
                            .background(
                                Color.Black.copy(alpha = 0.25f),
                                ThumbnailRoundedShape
                            )
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.offline),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .size(ListThumbnailSize / 2)
                                .align(Alignment.Center)
                                .graphicsLayer { alpha = 1f }
                        )
                    }
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (subtitle != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        CompositionLocalProvider(LocalContentColor provides onSurface.copy(alpha = 0.6f)) {
                            subtitle()
                        }
                    }
                }
            }

            CompositionLocalProvider(LocalContentColor provides onSurface.copy(alpha = 0.7f)) {
                trailingContent()
            }
        }
        if (flat) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = AppleTokens.Divider,
                thickness = 0.5.dp,
            )
        }
    }
}

@Composable
fun ListItem(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: AnnotatedString?,
    badges: @Composable RowScope.() -> Unit = {},
    thumbnailContent: @Composable () -> Unit,
    trailingContent: @Composable RowScope.() -> Unit = {},
    isSelected: Boolean? = false,
    isActive: Boolean = false,
    shape: Shape = RectangleShape,
    drawHighlight: Boolean = true,
    flat: Boolean = false,
) = ListItem(
    title = title,
    subtitle = {
        badges()
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    },
    thumbnailContent = thumbnailContent,
    trailingContent = trailingContent,
    modifier = modifier,
    isSelected = isSelected,
    isActive = isActive,
    shape = shape,
    drawHighlight = drawHighlight,
    flat = flat
)

// merge badges and subtitle text and pass to basic list item
@Composable
fun ListItem(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String?,
    badges: @Composable RowScope.() -> Unit = {},
    thumbnailContent: @Composable () -> Unit,
    trailingContent: @Composable RowScope.() -> Unit = {},
    isSelected: Boolean? = false,
    isActive: Boolean = false,
    shape: Shape = RectangleShape,
    drawHighlight: Boolean = true,
    flat: Boolean = false,
) = ListItem(
    title = title,
    subtitle = {
        badges()

        if (!subtitle.isNullOrEmpty()) {
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    },
    thumbnailContent = thumbnailContent,
    trailingContent = trailingContent,
    modifier = modifier,
    isSelected = isSelected,
    isActive = isActive,
    shape = shape,
    drawHighlight = drawHighlight,
    flat = flat
)

@Composable
fun GridItem(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    subtitle: @Composable () -> Unit,
    badges: @Composable RowScope.() -> Unit = {},
    thumbnailContent: @Composable BoxWithConstraintsScope.() -> Unit,
    thumbnailRatio: Float = 1f,
    fillMaxWidth: Boolean = false,
    // When set, the whole card gets a colorful fill of this color (the
    // artwork's dominant tint) instead of the plain transparent layout.
    containerColor: Color? = null,
) {
    val gridHeight = currentGridThumbnailHeight()
    val isLibrary = modifier.toString().contains("Library", ignoreCase = true)
    
    val sizeModifier = if (fillMaxWidth) Modifier.fillMaxWidth() else Modifier.width(gridHeight * thumbnailRatio)
    val cardModifier = if (containerColor != null) {
        Modifier
            .padding(8.dp)
            .background(containerColor.copy(alpha = 0.55f), ContinuousRoundedRectangle(AppleTokens.CardCorner))
            .padding(8.dp)
    } else {
        Modifier.padding(12.dp)
    }
    Column(
        modifier = modifier
            .then(sizeModifier)
            .then(cardModifier)
    ) {
        BoxWithConstraints(
            contentAlignment = Alignment.Center,
            modifier = if (fillMaxWidth) {
                Modifier.fillMaxWidth()
            } else {
                Modifier.height(gridHeight)
            }
                .aspectRatio(thumbnailRatio)
                .clip(ContinuousRoundedRectangle(AppleTokens.CardCorner))
        ) {
            thumbnailContent()
        }

        Spacer(modifier = Modifier.height(6.dp))

        title()

        Row(verticalAlignment = Alignment.CenterVertically) {
            badges()

            subtitle()
        }
    }
}

@Composable
fun GridItem(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    badges: @Composable RowScope.() -> Unit = {},
    thumbnailContent: @Composable BoxWithConstraintsScope.() -> Unit,
    thumbnailRatio: Float = 1f,
    fillMaxWidth: Boolean = false,
) = GridItem(
    modifier = modifier,
    title = {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
    },
    subtitle = {
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    },
    thumbnailContent = thumbnailContent,
    thumbnailRatio = thumbnailRatio,
    fillMaxWidth = fillMaxWidth
)

@Composable
fun SongListItem(
    song: Song,
    modifier: Modifier = Modifier,
    albumIndex: Int? = null,
    showLikedIcon: Boolean = true,
    showInLibraryIcon: Boolean = false,
    showDownloadIcon: Boolean = true,
    badges: @Composable RowScope.() -> Unit = {
        if (showLikedIcon && song.song.liked) {
            Icon.Favorite()
        }
        if (song.song.explicit) {
            Icon.Explicit()
        }
        if (showInLibraryIcon && song.song.inLibrary != null) {
            Icon.Library()
        }
        if (showDownloadIcon) {
            val download by LocalDownloadUtil.current.getDownload(song.id)
                .collectAsState(initial = null)
            Icon.Download(download?.state)
        }
    },
    isSelected: Boolean = false,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    isSwipeable: Boolean = true,
    onSelectionChange: (Boolean) -> Unit = {},
    trailingContent: @Composable RowScope.() -> Unit = {},
    drawHighlight: Boolean = true,
    shape: Shape = RectangleShape,
    flat: Boolean = false,
    showIconOnly: Boolean = false,
) {
    val swipeEnabled by rememberPreference(SwipeToSongKey, defaultValue = false)

    val content: @Composable () -> Unit = {
        ListItem(
            title = song.song.title,
            subtitle = joinByBullet(
                song.artists.joinToString { it.name },
                makeTimeString(song.song.duration * 1000L)
            ),
            badges = badges,
            thumbnailContent = {
                if (showIconOnly) {
                    Box(
                        modifier = Modifier
                            .size(ListThumbnailSize)
                            .clip(ThumbnailRoundedShape)
                            .background(LocalContentColor.current.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.music_note),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = LocalContentColor.current.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    ItemThumbnail(
                        thumbnailUrl = song.song.thumbnailUrl,
                        albumIndex = albumIndex,
                        isSelected = isSelected,
                        isActive = isActive,
                        isPlaying = isPlaying,
                        shape = ThumbnailRoundedShape,
                        modifier = Modifier.size(ListThumbnailSize)
                    )
                }
            },
            trailingContent = trailingContent,
            modifier = modifier,
            isSelected = isSelected,
            isActive = isActive,
            shape = shape,
            drawHighlight = drawHighlight,
            flat = flat
        )
    }

    if (isSwipeable && swipeEnabled) {
        SwipeToSongBox(
            mediaItem = song.toMediaItem(),
            modifier = Modifier.fillMaxWidth()
        ) {
            content()
        }
    } else {
        content()
    }
}

@Composable
fun SongGridItem(
    song: Song,
    modifier: Modifier = Modifier,
    showLikedIcon: Boolean = true,
    showInLibraryIcon: Boolean = false,
    showDownloadIcon: Boolean = true,
    badges: @Composable RowScope.() -> Unit = {
        if (showLikedIcon && song.song.liked) {
            Icon.Favorite()
        }
        if (showInLibraryIcon && song.song.inLibrary != null) {
            Icon.Library()
        }
        if (showDownloadIcon) {
            val download by LocalDownloadUtil.current.getDownload(song.id).collectAsState(initial = null)
            Icon.Download(download?.state)
        }
    },
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    fillMaxWidth: Boolean = false,
    showIconOnly: Boolean = false,
) = GridItem(
    title = {
        Text(
            text = song.song.title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.basicMarquee().fillMaxWidth()
        )
    },
    subtitle = {
        Text(
            text = joinByBullet(
                song.artists.joinToString { it.name },
                makeTimeString(song.song.duration * 1000L)
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    },
    badges = badges,
    thumbnailContent = {
        if (showIconOnly) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LocalContentColor.current.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.music_note),
                    contentDescription = null,
                    modifier = Modifier.size(maxWidth / 2.5f),
                    tint = LocalContentColor.current.copy(alpha = 0.6f)
                )
            }
        } else {
            val gridHeight = currentGridThumbnailHeight()
            ItemThumbnail(
                thumbnailUrl = song.song.thumbnailUrl,
                isActive = isActive,
                isPlaying = isPlaying,
                shape = ThumbnailRoundedShape,
                modifier = Modifier.size(gridHeight)
            )
            if (!isActive) {
                OverlayPlayButton(
                    visible = true
                )
            }
        }
    },
    fillMaxWidth = fillMaxWidth,
    modifier = modifier
)

@Composable
fun ArtistListItem(
    artist: Artist,
    modifier: Modifier = Modifier,
    flat: Boolean = false,
    badges: @Composable RowScope.() -> Unit = {
        if (artist.artist.bookmarkedAt != null) {
            Icon(
                painter = painterResource(R.drawable.favorite),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .size(18.dp)
                    .padding(end = 2.dp),
            )
        }
    },
    trailingContent: @Composable RowScope.() -> Unit = {},
    showIconOnly: Boolean = false,
) = ListItem(
    title = artist.artist.name,
    subtitle = pluralStringResource(R.plurals.n_song, artist.songCount, artist.songCount),
    badges = badges,
    thumbnailContent = {
        if (showIconOnly) {
            Box(
                modifier = Modifier
                    .size(ListThumbnailSize)
                    .clip(CircleShape)
                    .background(LocalContentColor.current.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.artist),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = LocalContentColor.current.copy(alpha = 0.6f)
                )
            }
        } else {
            val context = LocalContext.current
            val imageRequest = remember(artist.artist.thumbnailUrl) {
                ImageRequest.Builder(context)
                    .data(artist.artist.thumbnailUrl?.resize(544, 544))
                    .memoryCachePolicy(coil3.request.CachePolicy.ENABLED)
                    .diskCachePolicy(coil3.request.CachePolicy.ENABLED)
                    .networkCachePolicy(coil3.request.CachePolicy.ENABLED)
                    .build()
            }
            AsyncImage(
                model = imageRequest,
                contentDescription = null,
                modifier = Modifier
                    .size(ListThumbnailSize)
                    .clip(CircleShape),
            )
        }
    },
    trailingContent = trailingContent,
    modifier = modifier,
    flat = flat
)

@Composable
fun ArtistGridItem(
    artist: Artist,
    modifier: Modifier = Modifier,
    badges: @Composable RowScope.() -> Unit = {
        if (artist.artist.bookmarkedAt != null) {
            Icon.Favorite()
        }
    },
    fillMaxWidth: Boolean = false,
    showIconOnly: Boolean = false,
) = GridItem(
    title = artist.artist.name,
    subtitle = pluralStringResource(R.plurals.n_song, artist.songCount, artist.songCount),
    badges = badges,
    thumbnailContent = {
        if (showIconOnly) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(LocalContentColor.current.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.artist),
                    contentDescription = null,
                    modifier = Modifier.size(ListThumbnailSize / 2),
                    tint = LocalContentColor.current.copy(alpha = 0.6f)
                )
            }
        } else {
            val context = LocalContext.current
            val imageRequest = remember(artist.artist.thumbnailUrl) {
                ImageRequest.Builder(context)
                    .data(artist.artist.thumbnailUrl?.resize(544, 544))
                    .memoryCachePolicy(coil3.request.CachePolicy.ENABLED)
                    .diskCachePolicy(coil3.request.CachePolicy.ENABLED)
                    .networkCachePolicy(coil3.request.CachePolicy.ENABLED)
                    .build()
            }
            AsyncImage(
                model = imageRequest,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        }
    },
    fillMaxWidth = fillMaxWidth,
    modifier = modifier
)

@Composable
fun AlbumListItem(
    album: Album,
    modifier: Modifier = Modifier,
    flat: Boolean = false,
    showLikedIcon: Boolean = true,
    badges: @Composable RowScope.() -> Unit = {
        val downloadUtil = LocalDownloadUtil.current
        val database = LocalDatabase.current

        val songs by produceState<List<Song>>(initialValue = emptyList(), album.id) {
            withContext(Dispatchers.IO) {
                value = database.albumSongs(album.id).first()
            }
        }

        val allDownloads by downloadUtil.downloads.collectAsState()

        val downloadState by remember(songs, allDownloads) {
            androidx.compose.runtime.mutableIntStateOf(
                if (songs.isEmpty()) {
                    Download.STATE_STOPPED
                } else {
                    when {
                        songs.all { allDownloads[it.id]?.state == STATE_COMPLETED } -> STATE_COMPLETED
                        songs.any { allDownloads[it.id]?.state in listOf(STATE_QUEUED, STATE_DOWNLOADING) } -> STATE_DOWNLOADING
                        else -> Download.STATE_STOPPED
                    }
                }
            )
        }

        if (showLikedIcon && album.album.bookmarkedAt != null) {
            Icon.Favorite()
        }
        if (album.album.explicit) {
            Icon.Explicit()
        }
        Icon.Download(downloadState)
    },
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    trailingContent: @Composable RowScope.() -> Unit = {},
    showIconOnly: Boolean = false,
) = ListItem(
    title = album.album.title,
    subtitle = joinByBullet(
        album.artists.joinToString { it.name },
        pluralStringResource(R.plurals.n_song, album.album.songCount, album.album.songCount),
        album.album.year?.toString()
    ),
    badges = badges,
    thumbnailContent = {
        if (showIconOnly) {
            Box(
                modifier = Modifier
                    .size(ListThumbnailSize)
                    .clip(ThumbnailRoundedShape)
                    .background(LocalContentColor.current.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.album),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = LocalContentColor.current.copy(alpha = 0.6f)
                )
            }
        } else {
            ItemThumbnail(
                thumbnailUrl = album.album.thumbnailUrl,
                isActive = isActive,
                isPlaying = isPlaying,
                shape = ThumbnailRoundedShape,
                modifier = Modifier.size(ListThumbnailSize)
            )
        }
    },
    trailingContent = trailingContent,
    modifier = modifier,
    flat = flat
)

@Composable
fun AlbumGridItem(
    album: Album,
    modifier: Modifier = Modifier,
    coroutineScope: CoroutineScope,
    badges: @Composable RowScope.() -> Unit = {
        val downloadUtil = LocalDownloadUtil.current
        val database = LocalDatabase.current

        val songs by produceState<List<Song>>(initialValue = emptyList(), album.id) {
            withContext(Dispatchers.IO) {
                value = database.albumSongs(album.id).first()
            }
        }

        val allDownloads by downloadUtil.downloads.collectAsState()

        val downloadState by remember(songs, allDownloads) {
            androidx.compose.runtime.mutableIntStateOf(
                if (songs.isEmpty()) {
                    Download.STATE_STOPPED
                } else {
                    when {
                        songs.all { allDownloads[it.id]?.state == STATE_COMPLETED } -> STATE_COMPLETED
                        songs.any { allDownloads[it.id]?.state in listOf(STATE_QUEUED, STATE_DOWNLOADING) } -> STATE_DOWNLOADING
                        else -> Download.STATE_STOPPED
                    }
                }
            )
        }

        if (album.album.bookmarkedAt != null) {
            Icon.Favorite()
        }
        if (album.album.explicit) {
            Icon.Explicit()
        }
        Icon.Download(downloadState)
    },
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    fillMaxWidth: Boolean = false,
    showIconOnly: Boolean = false,
) = GridItem(
    title = {
        Text(
            text = album.album.title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.basicMarquee().fillMaxWidth()
        )
    },
    subtitle = {
        Text(
            text = album.artists.joinToString { it.name },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    },
    badges = badges,
    thumbnailContent = {
        if (showIconOnly) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(ThumbnailRoundedShape)
                    .background(LocalContentColor.current.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.album),
                    contentDescription = null,
                    modifier = Modifier.size(ListThumbnailSize / 2),
                    tint = LocalContentColor.current.copy(alpha = 0.6f)
                )
            }
        } else {
            val database = LocalDatabase.current
            val playerConnection = LocalPlayerConnection.current ?: return@GridItem
            val scope = rememberCoroutineScope()

            ItemThumbnail(
                thumbnailUrl = album.album.thumbnailUrl,
                isActive = isActive,
                isPlaying = isPlaying,
                shape = ThumbnailRoundedShape,
            )

            AlbumPlayButton(
                visible = !isActive,
                onClick = {
                    scope.launch {
                        val albumWithSongs = withContext(Dispatchers.IO) {
                            database.albumWithSongs(album.id).firstOrNull()
                        }
                        albumWithSongs?.let {
                            playerConnection.playQueue(LocalAlbumRadio(it))
                        }
                    }
                }
            )
        }
    },
    fillMaxWidth = fillMaxWidth,
    containerColor = if (showIconOnly) null else rememberArtworkTint(album.album.thumbnailUrl).getOrNull(0)?.asDeepTint(),
    modifier = modifier
)

@Composable
private fun getAutoPlaylistIcon(name: String): Int = when {
    name == stringResource(R.string.liked) -> R.drawable.favorite_border
    name == stringResource(R.string.offline) -> R.drawable.offline
    name == stringResource(R.string.cached_playlist) -> R.drawable.cached
    name == stringResource(R.string.uploaded_playlist) -> R.drawable.backup
    name == stringResource(R.string.filter_local) -> R.drawable.local_songs
    name.startsWith(stringResource(R.string.my_top)) -> R.drawable.trending_up
    else -> R.drawable.queue_music
}

@Composable
fun PlaylistListItem(
    playlist: Playlist,
    modifier: Modifier = Modifier,
    autoPlaylist: Boolean = false,
    // When set, the card shows this image (a user-picked content:// URI or a
    // first-song artwork URL) instead of the playlist thumbnails / auto icon.
    thumbnailOverrideUrl: String? = null,
    flat: Boolean = false,
    badges: @Composable RowScope.() -> Unit = {
        val downloadUtil = LocalDownloadUtil.current
        val database = LocalDatabase.current

        val songs by produceState<List<Song>>(initialValue = emptyList(), playlist.id) {
            withContext(Dispatchers.IO) {
                value = database.playlistSongs(playlist.id).first().map { it.song }
            }
        }

        val allDownloads by downloadUtil.downloads.collectAsState()

        val downloadState by remember(songs, allDownloads) {
            androidx.compose.runtime.mutableIntStateOf(
                if (songs.isEmpty()) {
                    Download.STATE_STOPPED
                } else {
                    when {
                        songs.all { allDownloads[it.id]?.state == STATE_COMPLETED } -> STATE_COMPLETED
                        songs.any { allDownloads[it.id]?.state in listOf(STATE_QUEUED, STATE_DOWNLOADING) } -> STATE_DOWNLOADING
                        else -> Download.STATE_STOPPED
                    }
                }
            )
        }

        Icon.Download(downloadState)
    },
    trailingContent: @Composable RowScope.() -> Unit = {},
    shape: Shape = androidx.compose.ui.graphics.RectangleShape,
    showIconOnly: Boolean = false,
) = ListItem(
    title = playlist.playlist.name,
    subtitle = if (autoPlaylist) {
        ""
    } else {
        if (playlist.songCount == 0 && playlist.playlist.remoteSongCount != null) {
            pluralStringResource(
                R.plurals.n_song,
                playlist.playlist.remoteSongCount,
                playlist.playlist.remoteSongCount
            )
        } else {
            pluralStringResource(
                R.plurals.n_song,
                playlist.songCount,
                playlist.songCount
            )
        }
    },
    badges = badges,
    thumbnailContent = {
        if (thumbnailOverrideUrl != null) {
            AsyncImage(
                model = thumbnailOverrideUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(ListThumbnailSize)
                    .clip(ThumbnailRoundedShape),
            )
        } else if (showIconOnly) {
            Box(
                modifier = Modifier
                    .size(ListThumbnailSize)
                    .clip(ThumbnailRoundedShape)
                    .background(LocalContentColor.current.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(getAutoPlaylistIcon(playlist.playlist.name)),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = LocalContentColor.current.copy(alpha = 0.6f)
                )
            }
        } else {
            PlaylistThumbnail(
                thumbnails = playlist.thumbnails,
                size = ListThumbnailSize,
                placeHolder = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(LocalContentColor.current.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(getAutoPlaylistIcon(playlist.playlist.name)),
                            contentDescription = null,
                            tint = LocalContentColor.current.copy(alpha = 0.6f),
                            modifier = Modifier.size(ListThumbnailSize / 2)
                        )
                    }
                },
                shape = ThumbnailRoundedShape
            )
        }
    },
    trailingContent = trailingContent,
    modifier = modifier,
    shape = shape,
    flat = flat
)

@Composable
fun PlaylistGridItem(
    playlist: Playlist,
    modifier: Modifier = Modifier,
    autoPlaylist: Boolean = false,
    // When set, the card shows this image (a user-picked content:// URI or a
    // first-song artwork URL) instead of the playlist thumbnails / auto icon.
    thumbnailOverrideUrl: String? = null,
    badges: @Composable RowScope.() -> Unit = {
        val downloadUtil = LocalDownloadUtil.current
        val database = LocalDatabase.current

        val songs by produceState<List<Song>>(initialValue = emptyList(), playlist.id) {
            withContext(Dispatchers.IO) {
                value = database.playlistSongs(playlist.id).first().map { it.song }
            }
        }

        val allDownloads by downloadUtil.downloads.collectAsState()

        val downloadState by remember(songs, allDownloads) {
            mutableIntStateOf(
                if (songs.isEmpty()) {
                    Download.STATE_STOPPED
                } else {
                    when {
                        songs.all { allDownloads[it.id]?.state == STATE_COMPLETED } -> STATE_COMPLETED
                        songs.any { allDownloads[it.id]?.state in listOf(STATE_QUEUED, STATE_DOWNLOADING) } -> STATE_DOWNLOADING
                        else -> Download.STATE_STOPPED
                    }
                }
            )
        }

        Icon.Download(downloadState)
    },
    fillMaxWidth: Boolean = false,
    showIconOnly: Boolean = false,
) = GridItem(
    title = {
        Text(
            text = playlist.playlist.name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.basicMarquee().fillMaxWidth()
        )
    },
    subtitle = {
        val subtitle = if (autoPlaylist) {
            ""
        } else {
            if (playlist.songCount == 0 && playlist.playlist.remoteSongCount != null) {
                pluralStringResource(
                    R.plurals.n_song,
                    playlist.playlist.remoteSongCount,
                    playlist.playlist.remoteSongCount
                )
            } else {
                pluralStringResource(
                    R.plurals.n_song,
                    playlist.songCount,
                    playlist.songCount
                )
            }
        }
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    },
    badges = badges,
    thumbnailContent = {
        val width = maxWidth
        if (thumbnailOverrideUrl != null) {
            AsyncImage(
                model = thumbnailOverrideUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else if (showIconOnly) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LocalContentColor.current.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(getAutoPlaylistIcon(playlist.playlist.name)),
                    contentDescription = null,
                    modifier = Modifier.size(width / 3),
                    tint = LocalContentColor.current.copy(alpha = 0.6f)
                )
            }
        } else {
        PlaylistThumbnail(
            thumbnails = playlist.thumbnails,
            size = width,
            placeHolder = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LocalContentColor.current.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(getAutoPlaylistIcon(playlist.playlist.name)),
                        contentDescription = null,
                        tint = LocalContentColor.current.copy(alpha = 0.6f),
                        modifier = Modifier.size(width / 2)
                    )
                }
            },
            shape = ThumbnailRoundedShape
        )
        }
    },
    fillMaxWidth = fillMaxWidth,
    containerColor = if (showIconOnly) null else rememberArtworkTint(playlist.thumbnails.firstOrNull()).getOrNull(0)?.asDeepTint(),
    modifier = modifier
)

@Composable
fun MediaMetadataListItem(
    mediaMetadata: MediaMetadata,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    shape: Shape = RectangleShape,
    flat: Boolean = false,
    trailingContent: @Composable RowScope.() -> Unit = {},
) {
    ListItem(
        title = mediaMetadata.title,
        subtitle = if (mediaMetadata.suggestedBy != null) {
            buildAnnotatedString {
                append(mediaMetadata.artists.joinToString { it.name })
                append(" • ")
                append(makeTimeString(mediaMetadata.duration * 1000L))
                append(" • ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(mediaMetadata.suggestedBy)
                }
            }
        } else {
            AnnotatedString(
                joinByBullet(
                    mediaMetadata.artists.joinToString { it.name },
                    makeTimeString(mediaMetadata.duration * 1000L)
                )
            )
        },
        badges = { if (mediaMetadata.explicit) Icon.Explicit()},
        thumbnailContent = {
            ItemThumbnail(
                thumbnailUrl = mediaMetadata.thumbnailUrl,
                albumIndex = null,
                isSelected = isSelected,
                isActive = isActive,
                isPlaying = isPlaying,
                shape = ThumbnailRoundedShape,
                modifier = Modifier.size(ListThumbnailSize)
            )
        },
        trailingContent = trailingContent,
        modifier = modifier,
        isActive = isActive,
        shape = shape,
        flat = flat
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouTubeListItem(
    item: YTItem,
    modifier: Modifier = Modifier,
    albumIndex: Int? = null,
    isSelected: Boolean = false,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    isSwipeable: Boolean = true,
    trailingContent: @Composable RowScope.() -> Unit = {},
    badges: @Composable RowScope.() -> Unit = {
        val database = LocalDatabase.current
        val song by produceState<Song?>(initialValue = null, item.id) {
            if (item is SongItem) value = database.song(item.id).firstOrNull()
        }
        val album by produceState<Album?>(initialValue = null, item.id) {
            if (item is AlbumItem) value = database.album(item.id).firstOrNull()
        }

        if ((item is SongItem && song?.song?.liked == true) ||
            (item is AlbumItem && album?.album?.bookmarkedAt != null)
        ) {
            Icon.Favorite()
        }
        if (item.explicit) Icon.Explicit()
        if (item is SongItem) {
            val download by LocalDownloadUtil.current.getDownload(item.id).collectAsState(null)
            Icon.Download(download?.state)
        }
    },
    shape: Shape = RectangleShape,
    drawHighlight: Boolean = true,
    flat: Boolean = false,
) {
    val swipeEnabled by rememberPreference(SwipeToSongKey, defaultValue = false)

    val content: @Composable () -> Unit = {
        ListItem(
            title = item.title,
            subtitle = when (item) {
                is SongItem -> joinByBullet(item.artists.joinToString { it.name }, makeTimeString(item.duration?.times(1000L)))
                is AlbumItem -> joinByBullet(item.artists?.joinToString { it.name }, item.year?.toString())
                is ArtistItem -> null
                is PlaylistItem -> joinByBullet(item.author?.name, item.songCountText)
            },
            badges = badges,
            thumbnailContent = {
                ItemThumbnail(
                    thumbnailUrl = item.thumbnail,
                    albumIndex = albumIndex,
                    isSelected = isSelected,
                    isActive = isActive,
                    isPlaying = isPlaying,
                    shape = if (item is ArtistItem) CircleShape else ThumbnailRoundedShape,
                    modifier = Modifier.size(ListThumbnailSize)
                )
            },
            trailingContent = trailingContent,
            modifier = modifier,
            isSelected = isSelected,
            isActive = isActive,
            shape = shape,
            drawHighlight = drawHighlight,
            flat = flat
        )
    }

    if (item is SongItem && isSwipeable && swipeEnabled) {
        SwipeToSongBox(
            mediaItem = item.copy(thumbnail = item.thumbnail.resize(544,544)).toMediaItem(),
            modifier = Modifier.fillMaxWidth()
        ) {
            content()
        }
    } else {
        content()
    }
}

@Composable
fun YouTubeGridItem(
    item: YTItem,
    modifier: Modifier = Modifier,
    coroutineScope: CoroutineScope? = null,
    badges: @Composable RowScope.() -> Unit = {
        val database = LocalDatabase.current
        val song by produceState<Song?>(initialValue = null, item.id) {
            if (item is SongItem) value = database.song(item.id).firstOrNull()
        }
        val album by produceState<Album?>(initialValue = null, item.id) {
            if (item is AlbumItem) value = database.album(item.id).firstOrNull()
        }

        if (item is SongItem && song?.song?.liked == true ||
            item is AlbumItem && album?.album?.bookmarkedAt != null
        ) {
            Icon.Favorite()
        }
        if (item.explicit) Icon.Explicit()
        // if (item is SongItem && song?.song?.inLibrary != null) Icon.Library()
        if (item is SongItem) {
            val download by LocalDownloadUtil.current.getDownload(item.id).collectAsState(null)
            Icon.Download(download?.state)
        }
    },
    thumbnailRatio: Float = if (item is SongItem) 16f / 9 else 1f,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    fillMaxWidth: Boolean = false,
) = GridItem(
    title = {
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = if (item is ArtistItem) TextAlign.Center else TextAlign.Start,
            modifier = Modifier.basicMarquee().fillMaxWidth()
        )
    },
    subtitle = {
        val subtitle = when (item) {
            is SongItem -> joinByBullet(item.artists.joinToString { it.name }, makeTimeString(item.duration?.times(1000L)))
            is AlbumItem -> joinByBullet(item.artists?.joinToString { it.name }, item.year?.toString())
            is ArtistItem -> null
            is PlaylistItem -> joinByBullet(item.author?.name, item.songCountText)
        }
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    },
    badges = badges,
    thumbnailContent = {
        val database = LocalDatabase.current
        val playerConnection = LocalPlayerConnection.current ?: return@GridItem
        val scope = rememberCoroutineScope()

        ItemThumbnail(
            thumbnailUrl = item.thumbnail,
            isActive = isActive,
            isPlaying = isPlaying,
            shape = if (item is ArtistItem) CircleShape else ThumbnailRoundedShape,
        )

        if (item is SongItem && !isActive) {
            OverlayPlayButton(
                visible = true
            )
        }

        AlbumPlayButton(
            visible = item is AlbumItem && !isActive,
            onClick = {
                scope.launch(Dispatchers.IO) {
                    var albumWithSongs = database.albumWithSongs(item.id).first()
                    if (albumWithSongs?.songs.isNullOrEmpty()) {
                        YouTube.album(item.id).onSuccess { albumPage ->
                            database.transaction { insert(albumPage) }
                            albumWithSongs = database.albumWithSongs(item.id).first()
                        }.onFailure { reportException(it) }
                    }
                    albumWithSongs?.let {
                        withContext(Dispatchers.Main) {
                            playerConnection.playQueue(LocalAlbumRadio(it))
                        }
                    }
                }
            }
        )
    },
    thumbnailRatio = thumbnailRatio,
    fillMaxWidth = fillMaxWidth,
    modifier = modifier
)

@Composable
fun LocalSongsGrid(
    title: String,
    subtitle: String,
    badges: @Composable RowScope.() -> Unit = {},
    thumbnailUrl: String?,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    fillMaxWidth: Boolean = false,
    modifier: Modifier = Modifier
) = GridItem(
    title = title,
    subtitle = subtitle,
    badges = badges,
    thumbnailContent = {
        LocalThumbnail(
            thumbnailUrl = thumbnailUrl,
            isActive = isActive,
            isPlaying = isPlaying,
            shape = ThumbnailRoundedShape,
            modifier = if (fillMaxWidth) Modifier.fillMaxWidth() else Modifier,
            showCenterPlay = true,
            playButtonVisible = false
        )
    },
    fillMaxWidth = fillMaxWidth,
    modifier = modifier
)

@Composable
fun LocalArtistsGrid(
    title: String,
    subtitle: String,
    badges: @Composable RowScope.() -> Unit = {},
    thumbnailUrl: String?,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    fillMaxWidth: Boolean = false,
    modifier: Modifier = Modifier
) = GridItem(
    title = title,
    subtitle = subtitle,
    badges = badges,
    thumbnailContent = {
        LocalThumbnail(
            thumbnailUrl = thumbnailUrl,
            isActive = false,
            isPlaying = false,
            shape = CircleShape,
            modifier = if (fillMaxWidth) Modifier.fillMaxWidth() else Modifier,
            showCenterPlay = false,
            playButtonVisible = false
        )
    },
    fillMaxWidth = fillMaxWidth,
    modifier = modifier
)

@Composable
fun LocalAlbumsGrid(
    title: String,
    subtitle: String,
    badges: @Composable RowScope.() -> Unit = {},
    thumbnailUrl: String?,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    fillMaxWidth: Boolean = false,
    modifier: Modifier = Modifier
) = GridItem(
    title = title,
    subtitle = subtitle,
    badges = badges,
    thumbnailContent = {
        LocalThumbnail(
            thumbnailUrl = thumbnailUrl,
            isActive = isActive,
            isPlaying = isPlaying,
            shape = ThumbnailRoundedShape,
            modifier = if (fillMaxWidth) Modifier.fillMaxWidth() else Modifier,
            showCenterPlay = false,
            playButtonVisible = true
        )
    },
    fillMaxWidth = fillMaxWidth,
    modifier = modifier
)

@Composable
fun ItemThumbnail(
    thumbnailUrl: String?,
    isActive: Boolean,
    isPlaying: Boolean,
    shape: Shape,
    modifier: Modifier = Modifier,
    albumIndex: Int? = null,
    isSelected: Boolean = false,
    thumbnailRatio: Float = 1f
) {
    val cropAlbumArt by rememberPreference(CropAlbumArtKey, false)
    val context = LocalContext.current
    val imageRequest = remember(thumbnailUrl) {
        ImageRequest.Builder(context)
            .data(thumbnailUrl?.resize(544, 544))
            .size(CoilSize(544, 544))
            .memoryCachePolicy(coil3.request.CachePolicy.ENABLED)
            .diskCachePolicy(coil3.request.CachePolicy.ENABLED)
            .networkCachePolicy(coil3.request.CachePolicy.ENABLED)
            .build()
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .aspectRatio(thumbnailRatio)
            .clip(shape)
    ) {
        if (albumIndex == null) {
            AsyncImage(
                model = imageRequest,
                contentDescription = null,
                contentScale = if (cropAlbumArt) ContentScale.Crop else ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
            )
        }

        if (albumIndex != null) {
            AnimatedVisibility(
                visible = !isActive,
                enter = fadeIn() + expandIn(expandFrom = Alignment.Center),
                exit = shrinkOut(shrinkTowards = Alignment.Center) + fadeOut()
            ) {
                Text(
                    text = albumIndex.toString(),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        if (isSelected) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1f)
                    .clip(shape)
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                Icon(
                    painter = painterResource(R.drawable.done),
                    contentDescription = null
                )
            }
        }

        PlayingIndicatorBox(
            isActive = isActive,
            playWhenReady = isPlaying,
            color = if (albumIndex != null) MaterialTheme.colorScheme.onBackground else Color.White,
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = if (albumIndex != null)
                        Color.Transparent
                    else
                        Color.Black.copy(alpha = ActiveBoxAlpha),
                    shape = shape
                )
        )
    }
}

@Composable
fun LocalThumbnail(
    thumbnailUrl: String?,
    isActive: Boolean,
    isPlaying: Boolean,
    shape: Shape,
    modifier: Modifier = Modifier,
    showCenterPlay: Boolean = false,
    playButtonVisible: Boolean = false,
    thumbnailRatio: Float = 1f
) {
    val cropAlbumArt by rememberPreference(CropAlbumArtKey, false)
    val context = LocalContext.current
    val imageRequest = remember(thumbnailUrl) {
        ImageRequest.Builder(context)
            .data(thumbnailUrl?.resize(544, 544))
            .size(CoilSize(544, 544))
            .memoryCachePolicy(coil3.request.CachePolicy.ENABLED)
            .diskCachePolicy(coil3.request.CachePolicy.ENABLED)
            .networkCachePolicy(coil3.request.CachePolicy.ENABLED)
            .build()
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .aspectRatio(thumbnailRatio)
            .clip(shape)
    ) {
        AsyncImage(
            model = imageRequest,
            contentDescription = null,
            contentScale = if (cropAlbumArt) ContentScale.Crop else ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )

        AnimatedVisibility(
            visible = isActive,
            enter = fadeIn(tween(500)),
            exit = fadeOut(tween(500))
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f), shape)
            ) {
                if (isPlaying) {
                    PlayingIndicator(
                        color = Color.White,
                        modifier = Modifier.height(24.dp)
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.play),
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
        }

        if (showCenterPlay) {
            AnimatedVisibility(
                visible = !(isActive && isPlaying),
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(8.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                ) {
                    Icon(
                        painter = painterResource(R.drawable.play),
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
        }

        if (playButtonVisible) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = ActiveBoxAlpha))
                ) {
                    Icon(
                        painter = painterResource(R.drawable.play),
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun PlaylistThumbnail(
    thumbnails: List<String>,
    size: Dp,
    placeHolder: @Composable () -> Unit,
    shape: Shape,
    cacheKey: String? = null
) {
    val cropAlbumArt by rememberPreference(CropAlbumArtKey, false)
    val context = LocalContext.current

    when (thumbnails.size) {
        0 -> Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(size)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            placeHolder()
        }
        1 -> AsyncImage(
            model = remember(thumbnails) {
                ImageRequest.Builder(context)
                    .data(thumbnails[0].resize(544, 544))
                    .apply { /* Removed cache key extensions due to unresolved in env */ }
                    .memoryCachePolicy(coil3.request.CachePolicy.ENABLED)
                    .diskCachePolicy(coil3.request.CachePolicy.ENABLED)
                    .networkCachePolicy(coil3.request.CachePolicy.ENABLED)
                    .build()
            },
            contentDescription = null,
            contentScale = if (cropAlbumArt) ContentScale.Crop else ContentScale.Fit,
            placeholder = painterResource(R.drawable.queue_music),
            error = painterResource(R.drawable.queue_music),
            modifier = Modifier
                .size(size)
                .clip(shape)
        )
        else -> Box(
            modifier = Modifier
                .size(size)
                .clip(shape)
        ) {
            listOf(
                Alignment.TopStart,
                Alignment.TopEnd,
                Alignment.BottomStart,
                Alignment.BottomEnd
            ).fastForEachIndexed { index, alignment ->
                AsyncImage(
                    model = remember(thumbnails, index) {
                        ImageRequest.Builder(context)
                            .data(thumbnails.getOrNull(index)?.resize(544, 544))
                            .apply { /* Removed cache key extensions due to unresolved in env */ }
                            .memoryCachePolicy(coil3.request.CachePolicy.ENABLED)
                            .diskCachePolicy(coil3.request.CachePolicy.ENABLED)
                            .networkCachePolicy(coil3.request.CachePolicy.ENABLED)
                            .build()
                    },
                    contentDescription = null,
                    contentScale = if (cropAlbumArt) ContentScale.Crop else ContentScale.Fit,
                    placeholder = painterResource(R.drawable.queue_music),
                    error = painterResource(R.drawable.queue_music),
                    modifier = Modifier
                        .align(alignment)
                        .size(size / 2)
                )
            }
        }
    }
}

@Composable
fun BoxScope.OverlayPlayButton(
    visible: Boolean
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier
            .align(Alignment.Center)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = ActiveBoxAlpha))
        ) {
            Icon(
                painter = painterResource(R.drawable.play),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun BoxScope.OverlayEditButton(
    visible: Boolean,
    onClick: () -> Unit,
    alignment: Alignment = Alignment.Center,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier
            .align(alignment)
            .then(if (alignment == Alignment.BottomEnd) Modifier.padding(8.dp) else Modifier)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = ActiveBoxAlpha))
                .padding(0.dp)
                .clickable(onClick = onClick)
        ) {
            Icon(
                painter = painterResource(R.drawable.edit),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun BoxScope.AlbumPlayButton(
    visible: Boolean,
    onClick: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = ActiveBoxAlpha))
                .clickable(onClick = onClick)
        ) {
            Icon(
                painter = painterResource(R.drawable.play),
                contentDescription = null,
                tint = Color.White
            )
        }
    }
}

@Composable
fun SwipeToSongBox(
    modifier: Modifier = Modifier,
    mediaItem: MediaItem,
    content: @Composable BoxScope.() -> Unit
) {
    val ctx = LocalContext.current
    val player = LocalPlayerConnection.current
    val scope = rememberCoroutineScope()
    val offset = remember { mutableFloatStateOf(0f) }
    val threshold = 300f

    val dragState = rememberDraggableState { delta ->
        offset.floatValue = (offset.floatValue + delta).coerceIn(-threshold, threshold)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .draggable(
                orientation = Orientation.Horizontal,
                state = dragState,
                onDragStopped = {
                    when {
                        offset.floatValue >= threshold -> {
                            player?.playNext(listOf(mediaItem))
                            Toast.makeText(ctx, R.string.play_next, Toast.LENGTH_SHORT).show()
                            reset(offset, scope)
                        }

                        offset.floatValue <= -threshold -> {
                            player?.addToQueue(listOf(mediaItem))
                            Toast.makeText(ctx, R.string.add_to_queue, Toast.LENGTH_SHORT).show()
                            reset(offset, scope)
                        }

                        else -> reset(offset, scope)
                    }
                }
            )
    ) {
        if (offset.floatValue != 0f) {
            val (iconRes, bg, tint, align) = if (offset.floatValue > 0)
                Quadruple(
                    R.drawable.playlist_play,
                    MaterialTheme.colorScheme.secondary,
                    MaterialTheme.colorScheme.onSecondary,
                    Alignment.CenterStart
                ) else
                Quadruple(
                    R.drawable.queue_music,
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.onPrimary,
                    Alignment.CenterEnd
                )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .align(Alignment.Center)
                    .background(bg),
                contentAlignment = align
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .size(30.dp)
                        .alpha(0.9f),
                    tint = tint
                )
            }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(offset.floatValue.roundToInt(), 0) }
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface),
            content = content
        )
    }
}

// Helper to animate reset of swipe offset
private fun reset(offset: MutableState<Float>, scope: CoroutineScope) {
    scope.launch {
        animate(
            initialValue = offset.value,
            targetValue = 0f,
            animationSpec = tween(durationMillis = 300)
        ) { value, _ -> offset.value = value }
    }
}

// Data holder for swipe visuals
data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

object Icon {
    @Composable
    fun Favorite() {
        Icon(
            painter = painterResource(R.drawable.favorite),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier
                .size(18.dp)
                .padding(end = 2.dp)
        )
    }

    @Composable
    fun Library() {
        Icon(
            painter = painterResource(R.drawable.library_add_check),
            contentDescription = null,
            modifier = Modifier
                .size(18.dp)
                .padding(end = 2.dp)
        )
    }

    @Composable
    fun Download(state: Int?) {
        when (state) {
            STATE_COMPLETED -> Icon(
                painter = painterResource(R.drawable.offline),
                contentDescription = null,
                modifier = Modifier
                    .size(18.dp)
                    .padding(end = 2.dp)
            )
            STATE_QUEUED, STATE_DOWNLOADING -> CircularProgressIndicator(
                strokeWidth = 2.dp,
                modifier = Modifier
                    .size(16.dp)
                    .padding(end = 2.dp)
            )
            else -> { /* no icon */ }
        }
    }

    @Composable
    fun Explicit() {
        Icon(
            painter = painterResource(R.drawable.explicit),
            contentDescription = null,
            modifier = Modifier
                .size(18.dp)
                .padding(end = 2.dp)
        )
    }
}
