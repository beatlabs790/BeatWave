package iad1tya.echo.music.ui.component

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Picture
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import iad1tya.echo.music.R
import iad1tya.echo.music.db.entities.Song
import iad1tya.echo.music.db.entities.Artist
import iad1tya.echo.music.db.entities.AlbumEntity
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WrappedStoryDialog(
    totalPlayTime: Long,
    uniqueSongs: Int,
    topSongs: List<Song>,
    topArtists: List<Artist>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var currentSlide by remember { mutableStateOf(0) }
    val totalSlides = 5

    val slideColors = listOf(
        Brush.verticalGradient(listOf(Color(0xFF8E24AA), Color(0xFF3F51B5))),
        Brush.verticalGradient(listOf(Color(0xFFFF5252), Color(0xFFFF7A00))),
        Brush.verticalGradient(listOf(Color(0xFF00E676), Color(0xFF00B0FF))),
        Brush.verticalGradient(listOf(Color(0xFF795548), Color(0xFF9E9E9E))),
        Brush.verticalGradient(listOf(Color(0xFF111111), Color(0xFF222222)))
    )

    val picture = remember { Picture() }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        title = null,
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .drawWithCache {
                        onDrawWithContent {
                            val pictureCanvas = android.graphics.Canvas(picture.beginRecording(size.width.toInt(), size.height.toInt()))
                            drawIntoCanvas { canvas ->
                                pictureCanvas.drawColor(android.graphics.Color.TRANSPARENT)
                            }
                            picture.endRecording()
                            drawContent()
                        }
                    }
                    .background(slideColors[currentSlide])
                    .padding(24.dp)
            ) {
                // Progress indicator at the top
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    repeat(totalSlides) { index ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(if (index <= currentSlide) Color.White else Color.White.copy(alpha = 0.3f))
                        )
                    }
                }

                // Slide content
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 24.dp, bottom = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (currentSlide) {
                        0 -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                painter = painterResource(R.drawable.music_note),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(72.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Your BeatWave Wrapped",
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Let's take a look at your musical journey!",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                        1 -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Time spent listening",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "${totalPlayTime / (1000 * 60)}",
                                color = Color.White,
                                fontSize = 64.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                "minutes of pure vibe",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        2 -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Your Top Track",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                topSongs.firstOrNull()?.song?.title ?: "No songs played yet",
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                topSongs.firstOrNull()?.song?.artistsText ?: "",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                        3 -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Your Top Artist",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                topArtists.firstOrNull()?.artist?.name ?: "No artists found",
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                        4 -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Your Musical Diversity",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "You listened to $uniqueSongs unique songs",
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Next / Prev triggers
                Row(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable {
                                if (currentSlide > 0) currentSlide--
                            }
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable {
                                if (currentSlide < totalSlides - 1) currentSlide++
                            }
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                exportWrappedSlide(context, picture)
            }) {
                Text("Share this Slide")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    )
}

private fun exportWrappedSlide(context: Context, picture: Picture) {
    try {
        val width = 600
        val height = 600
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        picture.draw(canvas)

        val cachePath = File(context.cacheDir, "images")
        cachePath.mkdirs()
        val file = File(cachePath, "wrapped_card.png")
        val stream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()

        val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Wrapped Story"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
