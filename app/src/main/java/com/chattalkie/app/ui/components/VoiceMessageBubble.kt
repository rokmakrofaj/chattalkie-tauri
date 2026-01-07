package com.chattalkie.app.ui.components

import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chattalkie.app.data.AuthRepository
import com.chattalkie.app.data.remote.NetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

@Composable
fun VoiceMessageBubble(
    mediaKey: String
) {
    var audioUrl by remember { mutableStateOf<String?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }
    
    // MediaPlayer is not a composable, so we manage its lifecycle
    val context = androidx.compose.ui.platform.LocalContext.current
    val mediaPlayer = remember { MediaPlayer() }
    
    // Fetch URL
    LaunchedEffect(mediaKey) {
        val token = AuthRepository.token
        if (token != null && audioUrl == null) {
            isLoading = true
             withContext(Dispatchers.IO) {
                try {
                    val client = NetworkModule.okHttpClient
                    val request = okhttp3.Request.Builder()
                        .url("${NetworkModule.BASE_URL}api/media/$mediaKey")
                        .addHeader("Authorization", "Bearer $token")
                        .build()
                    val response = client.newCall(request).execute()
                    val json = response.body?.string()
                    if (json != null) {
                        var url = JSONObject(json).getString("url")
                        // Android Emulator/Device Fix: Rewrite localhost to BASE_URL host
                        if (url.contains("localhost")) {
                            val host = java.net.URI(NetworkModule.BASE_URL).host
                            url = url.replace("localhost", host)
                        }
                        audioUrl = url
                    }
                } catch(e: Exception) {
                    e.printStackTrace()
                    isError = true
                } finally {
                    isLoading = false
                }
            }
        }
    }

    // Setup MediaPlayer when URL is ready
    LaunchedEffect(audioUrl) {
        if (audioUrl != null) {
            try {
                mediaPlayer.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                mediaPlayer.setDataSource(audioUrl)
                mediaPlayer.prepareAsync()
                mediaPlayer.setOnCompletionListener { 
                    isPlaying = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                isError = true
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer.release()
        }
    }

    Row(
        modifier = Modifier
            .width(200.dp)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isLoading) {
             CircularProgressIndicator(
                 modifier = Modifier.size(24.dp),
                 strokeWidth = 2.dp
             )
        } else if (isError) {
             Icon(Icons.Default.Warning, contentDescription = "Error", tint = MaterialTheme.colorScheme.error)
             Spacer(modifier = Modifier.width(8.dp))
             Text("Ses Hatası", style = MaterialTheme.typography.bodySmall)
        } else {
            IconButton(
                onClick = {
                    if (isPlaying) {
                        mediaPlayer.pause()
                        isPlaying = false
                    } else {
                        mediaPlayer.start()
                        isPlaying = true
                    }
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            // Placeholder for waveform or progress bar
            LinearProgressIndicator(
                progress = if (isPlaying) 0.5f else 0f, // TODO: Implement real progress sync
                modifier = Modifier.weight(1f)
            )
        }
    }
}
