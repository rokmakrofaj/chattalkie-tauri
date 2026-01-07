package com.chattalkie.app.ui.chats.tabs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chattalkie.app.ui.model.ConversationUiState
import com.chattalkie.app.viewmodel.MessagesViewModel

@Composable
fun MessagesTab(
    onChatClick: (String, String, Boolean, Boolean) -> Unit
) {
    val viewModel: MessagesViewModel = viewModel()
    val conversations by viewModel.conversations.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.loadConversations()
    }

    if (isLoading && conversations.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (conversations.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Henüz bir sohbet yok.")
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                items = conversations,
                key = { it.id }
            ) { conversation ->
                ConversationRow(
                    conversation = conversation,
                    onClick = { 
                        onChatClick(conversation.id, conversation.partnerName, conversation.isGroup, conversation.role?.equals("admin", ignoreCase = true) == true) 
                    }
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun ConversationRow(
    conversation: ConversationUiState,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = conversation.partnerName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = if (conversation.lastMessage.isNotBlank()) conversation.lastMessage else (if (conversation.isOnline) "Çevrimiçi" else "Çevrimdışı"),
                style = MaterialTheme.typography.bodyMedium,
                color = if (conversation.isOnline && conversation.lastMessage.isBlank()) androidx.compose.ui.graphics.Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.width(16.dp))
        Text(
            text = conversation.timestamp,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
