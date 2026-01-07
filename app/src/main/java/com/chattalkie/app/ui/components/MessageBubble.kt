package com.chattalkie.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.chattalkie.app.domain.model.Message
import com.chattalkie.app.domain.model.MessageStatus
import com.chattalkie.app.domain.model.MessageType

@Composable
fun MessageBubble(
    message: Message,
    onResend: ((String) -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp, horizontal = 8.dp),
        horizontalArrangement = if (message.isMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isMe) 16.dp else 4.dp,
                bottomEnd = if (message.isMe) 4.dp else 16.dp
            ),
            color = if (message.isMe) {
                Color(0xFFDCF8C6) // WhatsApp green
            } else {
                Color.White
            },
            shadowElevation = 1.dp,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                if (!message.isMe && !message.senderName.isNullOrBlank()) {
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = Color(0xFF2196F3), // Primary Blue
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                if (message.messageType == MessageType.VOICE && message.mediaKey != null) {
                    VoiceMessageBubble(mediaKey = message.mediaKey)
                } else if (message.mediaKey != null) {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val token = com.chattalkie.app.data.AuthRepository.token
                    
                    var imageUrl by androidx.compose.runtime.remember(message.mediaKey) { androidx.compose.runtime.mutableStateOf<String?>(null) }
                    
                     val client = com.chattalkie.app.data.remote.NetworkModule.okHttpClient
                     androidx.compose.runtime.LaunchedEffect(message.mediaKey) {
                        if (imageUrl == null && token != null) {
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                try {
                                    val request = okhttp3.Request.Builder()
                                        .url("${com.chattalkie.app.data.remote.NetworkModule.BASE_URL}api/media/${message.mediaKey}")
                                        .addHeader("Authorization", "Bearer $token")
                                        .build()
                                    val response = client.newCall(request).execute()
                                    val json = response.body?.string()
                                    if (json != null) {
                                         var url = org.json.JSONObject(json).getString("url")
                                         // Android Emulator/Device Fix: Rewrite localhost to BASE_URL host
                                         if (url.contains("localhost")) {
                                             val host = java.net.URI(com.chattalkie.app.data.remote.NetworkModule.BASE_URL).host
                                             url = url.replace("localhost", host)
                                         }
                                         imageUrl = url
                                    }
                                } catch(e: Exception) { e.printStackTrace() }
                            }
                        }
                     }
                     
                     if (imageUrl != null) {
                         coil.compose.AsyncImage(
                             model = imageUrl,
                             contentDescription = "Image",
                             modifier = Modifier
                                 .widthIn(max = 240.dp)
                                 .heightIn(max = 300.dp)
                                 .padding(bottom = 4.dp)
                                 .fillMaxWidth(),
                             contentScale = androidx.compose.ui.layout.ContentScale.Crop
                         )
                     }
                }

                if (message.text.isNotBlank()) {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Timestamp and status row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = message.timestamp,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    
                    if (message.isMe) {
                        Spacer(modifier = Modifier.width(4.dp))
                        // Status icon
                        val icon = when (message.status) {
                            MessageStatus.SENDING -> Icons.Default.AccessTime
                            MessageStatus.SENT, MessageStatus.SYNCED -> Icons.Default.Check
                            MessageStatus.DELIVERED, MessageStatus.READ -> Icons.Default.DoneAll
                            MessageStatus.FAILED -> Icons.Default.Error
                        }
                        
                        val iconModifier = if (message.status == MessageStatus.FAILED && onResend != null && message.cid != null) {
                            Modifier
                                .size(14.dp)
                                .clickable { onResend(message.cid) }
                        } else {
                            Modifier.size(14.dp)
                        }

                        Icon(
                            imageVector = icon,
                            contentDescription = message.status.name,
                            modifier = iconModifier,
                            tint = when (message.status) {
                                MessageStatus.READ -> Color(0xFF4FC3F7) // Blue for read
                                MessageStatus.FAILED -> Color.Red
                                MessageStatus.SENDING -> Color.LightGray
                                else -> Color.Gray
                            }
                        )
                        
                        if (message.status == MessageStatus.FAILED) {
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(text = "Tekrarla", color = Color.Red, fontSize = 10.sp, 
                                modifier = Modifier.clickable { 
                                    if (message.cid != null) onResend?.invoke(message.cid) 
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
