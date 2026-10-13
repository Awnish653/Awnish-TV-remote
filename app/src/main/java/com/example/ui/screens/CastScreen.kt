package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.mediarouter.app.MediaRouteButton
import com.example.cast.LocalMediaServer
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private object CastMediaState {
    var server: LocalMediaServer? = null
    var cachedMedia: File? = null
}

@Composable
fun CastScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("Connect to a Cast-compatible TV first.") }

    fun castSelectedMedia(uri: Uri, isVideo: Boolean) {
        scope.launch {
            status = "Preparing media for the TV…"
            try {
                val castContext = CastContext.getSharedInstance(context)
                val session = castContext.sessionManager.currentCastSession
                val remoteClient = session?.remoteMediaClient
                if (session == null || remoteClient == null) {
                    status = "No Cast session. Tap the Cast icon and connect to your TV first."
                    return@launch
                }

                val mimeType = context.contentResolver.getType(uri)
                    ?: if (isVideo) "video/mp4" else "image/jpeg"
                val cacheFile = withContext(Dispatchers.IO) {
                    val target = File(context.cacheDir, "cast_media_" + System.currentTimeMillis())
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        target.outputStream().use { output -> input.copyTo(output) }
                    } ?: error("Could not open the selected media")
                    target
                }
                val (server, mediaUrl) = withContext(Dispatchers.IO) {
                    CastMediaState.server?.let { runCatching { it.stop() } }
                    LocalMediaServer.create(context, cacheFile, mimeType)
                }
                CastMediaState.server = server
                CastMediaState.cachedMedia?.delete()
                CastMediaState.cachedMedia = cacheFile

                val metadata = MediaMetadata(
                    if (isVideo) MediaMetadata.MEDIA_TYPE_MOVIE else MediaMetadata.MEDIA_TYPE_PHOTO
                ).apply {
                    putString(MediaMetadata.KEY_TITLE, cacheFile.name)
                }
                val mediaInfo = MediaInfo.Builder(mediaUrl)
                    .setContentType(mimeType)
                    .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                    .setMetadata(metadata)
                    .build()
                status = "Sending media to the TV…"
                remoteClient.load(mediaInfo).setResultCallback { result ->
                    status = if (result.status.isSuccess) {
                        if (isVideo) "Video playback started on the TV." else "Photo opened on the TV."
                    } else {
                        "TV could not play this media: " + (result.status.statusMessage ?: "unsupported format or receiver")
                    }
                }
            } catch (e: Exception) {
                status = "Cast failed: " + (e.message ?: "Check Wi-Fi and TV support.")
            }
        }
    }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) castSelectedMedia(uri, isVideo = false)
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) castSelectedMedia(uri, isVideo = true)
    }

    Column(
        modifier = modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Screen Cast", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Connect to a Cast-compatible TV to send photos and videos. Live screen mirroring is handled by your phone's system and depends on TV support.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    androidx.compose.material3.Icon(Icons.Default.Cast, contentDescription = null)
                    Text("Connect TV", style = MaterialTheme.typography.titleMedium)
                }
                Text("Tap the Cast icon and select your Chromecast / Google Cast receiver. Keep the phone and TV on the same Wi-Fi.")
                AndroidView(
                    factory = { viewContext ->
                        MediaRouteButton(viewContext).apply {
                            try {
                                CastContext.getSharedInstance(viewContext)
                                CastButtonFactory.setUpMediaRouteButton(viewContext, this)
                            } catch (_: Exception) {
                                isEnabled = false
                                contentDescription = "Google Cast is unavailable on this device"
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                )
                Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    androidx.compose.material3.Icon(Icons.Default.Image, contentDescription = null)
                    Text("Cast a photo", style = MaterialTheme.typography.titleMedium)
                }
                Text("Select a local photo. The app streams it over your local Wi-Fi to the connected Cast receiver.")
                OutlinedButton(onClick = { photoPicker.launch(arrayOf("image/*")) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Choose photo")
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    androidx.compose.material3.Icon(Icons.Default.VideoLibrary, contentDescription = null)
                    Text("Cast a video", style = MaterialTheme.typography.titleMedium)
                }
                Text("Select a local video. Supported formats depend on the Cast receiver.")
                OutlinedButton(onClick = { videoPicker.launch(arrayOf("video/*")) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Choose video")
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    androidx.compose.material3.Icon(Icons.Default.LiveTv, contentDescription = null)
                    Text("Mirror phone screen", style = MaterialTheme.typography.titleMedium)
                }
                Text("Open Android's system screen-mirroring controls. This only works if the phone and TV support a compatible mirroring protocol.")
                Button(
                    onClick = {
                        val opened = runCatching {
                            context.startActivity(Intent(Settings.ACTION_CAST_SETTINGS))
                        }.isSuccess
                        if (!opened) {
                            runCatching { context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS)) }
                                .onFailure { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open system mirroring")
                }
            }
        }
    }
}
