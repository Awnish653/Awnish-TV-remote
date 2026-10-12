package com.example.ui.screens

import android.content.ClipData
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun CastScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    fun shareMedia(uri: Uri, mimeType: String, chooserTitle: String) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newUri(context.contentResolver, "Media to cast", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { context.startActivity(Intent.createChooser(sendIntent, chooserTitle)) }
    }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) shareMedia(uri, "image/*", "Send photo to a casting app")
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) shareMedia(uri, "video/*", "Send video to a casting app")
    }

    Column(
        modifier = modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Screen Cast", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Cast photos and videos with a compatible TV/casting app, or open Android's system screen-mirroring controls.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    androidx.compose.material3.Icon(Icons.Default.LiveTv, contentDescription = null)
                    Text("Live phone screen", style = MaterialTheme.typography.titleMedium)
                }
                Text(
                    "Opens your phone's system Cast settings. Choose your TV and enable screen mirroring if the phone and TV support it.",
                    style = MaterialTheme.typography.bodySmall
                )
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
                    androidx.compose.material3.Icon(Icons.Default.Cast, contentDescription = null)
                    Spacer(Modifier.height(0.dp))
                    Text("Open screen-mirroring settings")
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    androidx.compose.material3.Icon(Icons.Default.Image, contentDescription = null)
                    Text("Cast a photo", style = MaterialTheme.typography.titleMedium)
                }
                Text("Choose an image, then select a compatible casting app or receiver from the Android share sheet.")
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
                Text("Choose a video, then select a compatible casting app or receiver. Some apps and TVs may not support every format.")
                OutlinedButton(onClick = { videoPicker.launch(arrayOf("video/*")) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Choose video")
                }
            }
        }

        Text(
            "Note: screen mirroring depends on the phone's system and TV support. Photo/video sharing needs a compatible casting receiver app; this screen does not bypass those requirements.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
