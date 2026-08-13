package iad1tya.echo.music.ui.component

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Picture
import kotlin.math.roundToInt
import android.net.Uri
import android.os.Environment
import androidx.compose.foundation.Canvas as JCanvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import iad1tya.echo.music.R
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricCardGenerator(
    songTitle: String,
    artistName: String,
    albumArtUrl: String?,
    lyricsLines: List<String>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var selectedLines by remember { mutableStateOf(lyricsLines.take(3).toSet()) }
    var selectedBackgroundIndex by remember { mutableStateOf(0) }
    var fontSize by remember { mutableStateOf(18f) }
    var alignCenter by remember { mutableStateOf(true) }

    val backgrounds = listOf(
        Brush.verticalGradient(listOf(Color(0xFF2193b0), Color(0xFF6dd5ed))),
        Brush.verticalGradient(listOf(Color(0xFFee9ca7), Color(0xFFffdde1))),
        Brush.verticalGradient(listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))),
        Brush.verticalGradient(listOf(Color(0xFF83a4d4), Color(0xFFb6fbff)))
    )

    val picture = remember { Picture() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Generate Lyric Card") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                // Card Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .drawWithCache {
                            onDrawWithContent {
                                val pictureCanvas = android.graphics.Canvas(picture.beginRecording(size.width.toInt(), size.height.toInt()))
                                drawIntoCanvas { canvas ->
                                    pictureCanvas.drawColor(android.graphics.Color.TRANSPARENT)
                                    // Redirect drawing to picture canvas
                                    // Simply capture content
                                }
                                picture.endRecording()
                                drawContent()
                            }
                        }
                        .background(backgrounds[selectedBackgroundIndex])
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = if (alignCenter) Alignment.CenterHorizontally else Alignment.Start,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = selectedLines.joinToString("\n"),
                            fontSize = fontSize.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = if (alignCenter) TextAlign.Center else TextAlign.Left,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (albumArtUrl != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(albumArtUrl)
                                        .build(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Column {
                                Text(songTitle, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                Text(artistName, fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Select Lyric Lines
                Text("Select Lyrics lines:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                lyricsLines.forEach { line ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedLines = if (selectedLines.contains(line)) {
                                    selectedLines - line
                                } else {
                                    selectedLines + line
                                }
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = selectedLines.contains(line),
                            onCheckedChange = {
                                selectedLines = if (it) selectedLines + line else selectedLines - line
                            }
                        )
                        Text(line, fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Background Style
                Text("Card Background Style:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    backgrounds.forEachIndexed { index, brush ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(brush)
                                .border(
                                    2.dp,
                                    if (selectedBackgroundIndex == index) Color.White else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedBackgroundIndex = index }
                        )
                    }
                }

                // Adjust FontSize
                Text("Font Size: ${fontSize.roundToInt()}sp", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Slider(
                    value = fontSize,
                    onValueChange = { fontSize = it },
                    valueRange = 12f..28f,
                    steps = 16
                )

                // Text Alignment
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Center Align Text", modifier = Modifier.weight(1f))
                    Switch(checked = alignCenter, onCheckedChange = { alignCenter = it })
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                exportCard(context, picture)
            }) {
                Text("Export & Share")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun exportCard(context: Context, picture: Picture) {
    try {
        val width = 600
        val height = 600
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        picture.draw(canvas)

        val cachePath = File(context.cacheDir, "images")
        cachePath.mkdirs()
        val file = File(cachePath, "lyric_card.png")
        val stream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()

        val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Lyric Card"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
