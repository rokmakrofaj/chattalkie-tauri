package com.chattalkie.app.ui.screens.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chattalkie.app.viewmodel.FriendViewModel
import com.chattalkie.app.ui.components.WalkieTalkieTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFriendScreen(
    viewModel: FriendViewModel,
    onNavigateUp: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val searchResults by viewModel.searchResults.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val primaryBlue = Color(0xFF2196F3)

    Scaffold(
        containerColor = Color.White,
        topBar = {
            WalkieTalkieTopBar(
                title = "Arkadaş Ekle",
                subtitle = "Arkadaşlarını bul",
                showBackButton = true,
                onBackClick = onNavigateUp
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color.White)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    viewModel.searchUsers(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Kullanıcı adına göre ara...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryBlue,
                    unfocusedBorderColor = Color.LightGray,
                    focusedLabelColor = primaryBlue
                )
            )

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = primaryBlue)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(searchResults) { user ->
                        UserSearchItem(
                            username = user.username,
                            name = user.name,
                            friendshipStatus = user.friendshipStatus,
                            accentColor = primaryBlue,
                            onAddClick = { viewModel.sendFriendRequest(user.id) }
                        )
                    }
                    
                    if (searchQuery.isNotEmpty() && searchResults.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Kullanıcı bulunamadı", color = Color.Gray)
                            }
                        }
                    } else if (searchQuery.isEmpty()) {
                         item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Aramak için bir isim yazın", color = Color.Gray, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserSearchItem(
    username: String,
    name: String,
    friendshipStatus: String?,
    accentColor: Color,
    onAddClick: () -> Unit
) {
    var isSent by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar Placeholder
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.take(1).uppercase(),
                color = accentColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(text = "@$username", color = Color.Gray, fontSize = 14.sp)
        }

        if (isSent || friendshipStatus == "PENDING") {
            Text(
                text = "İstek Atıldı",
                color = accentColor,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        } else if (friendshipStatus == "ACCEPTED") {
            Text(
                text = "Zaten Arkadaş",
                color = Color.Gray,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        } else {
            IconButton(
                onClick = {
                    onAddClick()
                    isSent = true
                },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = accentColor,
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = "Arkadaş Ekle")
            }
        }
    }
}
