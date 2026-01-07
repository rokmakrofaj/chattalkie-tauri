package com.chattalkie.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chattalkie.app.ui.components.UserAvatar
import com.chattalkie.app.ui.components.WalkieTalkieTopBar
import com.chattalkie.app.ui.components.ActionCard
import com.chattalkie.app.ui.components.UserCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: com.chattalkie.app.viewmodel.MessagesViewModel,
    onConversationClick: (String, String, Boolean, Boolean) -> Unit,
    onAddFriendClick: () -> Unit = {},
    onCreateGroupClick: () -> Unit = {},
    onStatusClick: () -> Unit = {},
    onAvatarClick: () -> Unit = {},
    onJoinGroupByLink: (String) -> Unit = {},
    paddingValues: PaddingValues = PaddingValues(0.dp)
) {
    val conversations by viewModel.conversations.collectAsState()
    val groupConversations = remember(conversations) {
        conversations.filter { it.isGroup }
    }
    val currentUser by com.chattalkie.app.data.AuthRepository.currentUser.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    var showJoinDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.padding(bottom = paddingValues.calculateBottomPadding()),
        containerColor = Color.White,
        topBar = {
            WalkieTalkieTopBar(
                showSettingsButton = false
            )
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = innerPadding,
            modifier = Modifier.fillMaxSize()
        ) {
            // Avatar Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UserAvatar(
                        userName = currentUser?.name ?: "User",
                        size = 48.dp,
                        modifier = Modifier.clickable(
                            indication = null,
                            interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                        ) { onAvatarClick() }
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onAddFriendClick) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = "Add Friend",
                                tint = Color(0xFF2196F3)
                            )
                        }
                        
                        IconButton(onClick = onStatusClick) {
                            Icon(
                                imageVector = Icons.Outlined.Circle,
                                contentDescription = "Status",
                                tint = Color(0xFF4CAF50)
                            )
                        }
                    }
                }
            }
            
            // Lobby Section
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "Lobby",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    
                    UserCard(
                        userName = currentUser?.name ?: "User",
                        userStatus = "Available",
                        buttonText = "Open",
                        onButtonClick = { /* TODO: Open lobby */ }
                    )
                    
                    ActionCard(
                        text = "Let's add friends!",
                        buttonText = "Add",
                        onButtonClick = onAddFriendClick
                    )
                }
            }
            
            // Private Section Header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 16.dp, bottom = 0.dp) // Removed bottom padding for list consistency
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Private",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showJoinDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text("Join", color = Color(0xFF2196F3))
                            }
                            
                            Button(
                                onClick = onCreateGroupClick,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF2196F3)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text("Create")
                            }
                        }
                    }
                }
            }

            // Private Groups List
            if (groupConversations.isEmpty()) {
                if (!isLoading) {
                    item {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            ActionCard(
                                text = "Henüz bir grup yok",
                                showButton = false
                            )
                        }
                    }
                }
            } else {
                items(
                    items = groupConversations,
                    key = { it.id }
                ) { group ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        ActionCard(
                            text = group.partnerName,
                            showButton = true,
                            buttonText = "Open",
                            onButtonClick = { 
                                onConversationClick(group.id, group.partnerName, true, group.role?.equals("admin", ignoreCase = true) == true)
                            }
                        )
                    }
                }
            }
            
            // Bottom Spacer
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
    
    // Join Group Dialog
    if (showJoinDialog) {
        JoinGroupByLinkDialog(
            onDismiss = { showJoinDialog = false },
            onJoin = { token ->
                showJoinDialog = false
                onJoinGroupByLink(token)
            }
        )
    }
}

@Composable
private fun JoinGroupByLinkDialog(
    onDismiss: () -> Unit,
    onJoin: (String) -> Unit
) {
    var linkText by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Gruba Katıl") },
        text = {
            Column {
                Text(
                    "Davet linkini yapıştırın:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = linkText,
                    onValueChange = { linkText = it },
                    placeholder = { Text("https://chattalkie.com/join/abc123") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onJoin(linkText) },
                enabled = linkText.isNotBlank()
            ) {
                Text("Katıl")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}

