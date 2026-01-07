package com.chattalkie.app.ui.screens.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
fun FriendsScreen(
    viewModel: FriendViewModel,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    onNavigateUp: () -> Unit
) {
    val friends by viewModel.friends.collectAsState()
    val pendingRequests by viewModel.pendingRequests.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val primaryBlue = Color(0xFF2196F3)

    LaunchedEffect(Unit) {
        viewModel.loadFriends()
        viewModel.loadPendingRequests()
    }

    Scaffold(
        modifier = Modifier.padding(bottom = paddingValues.calculateBottomPadding()),
        containerColor = Color.White,
        topBar = {
            WalkieTalkieTopBar(
                title = "Arkadaşlar & Durum",
                subtitle = "Bağlantılarını yönet",
                showBackButton = true,
                onBackClick = onNavigateUp
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Pending Requests Section
            if (pendingRequests.isNotEmpty()) {
                item {
                    SectionHeader("Arkadaşlık İstekleri (${pendingRequests.size})")
                }
                items(pendingRequests) { request ->
                    RequestItem(
                        name = request.name,
                        username = request.username,
                        accentColor = primaryBlue,
                        onAccept = { viewModel.respondToRequest(request.id, "ACCEPT") },
                        onReject = { viewModel.respondToRequest(request.id, "REJECT") }
                    )
                }
            }

            // Friends Section
            item {
                SectionHeader("Arkadaşlarım (${friends.size})")
            }

            if (friends.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Henüz arkadaşın yok. Birilerini ekle!", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            } else {
                items(friends) { friend ->
                    FriendItem(
                        name = friend.name,
                        username = friend.username,
                        isOnline = friend.status == "online",
                        accentColor = primaryBlue
                    )
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Surface(
        color = Color(0xFFF5F5F5),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )
    }
}

@Composable
fun RequestItem(
    name: String,
    username: String,
    accentColor: Color,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.take(1).uppercase(),
                color = accentColor,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(text = "@$username", color = Color.Gray, fontSize = 13.sp)
        }

        Row {
            IconButton(
                onClick = onReject,
                colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFFFFEBEE))
            ) {
                Icon(Icons.Default.Close, contentDescription = "Reddet", tint = Color.Red, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onAccept,
                colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFFE8F5E9))
            ) {
                Icon(Icons.Default.Check, contentDescription = "Onayla", tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun FriendItem(
    name: String,
    username: String,
    isOnline: Boolean,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEEEEEE)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name.take(1).uppercase(),
                    color = Color.Gray,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            // Status dot
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(if (isOnline) Color(0xFF4CAF50) else Color.LightGray)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(text = name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text(
                text = if (isOnline) "Çevrimiçi" else "Çevrimdışı",
                color = if (isOnline) Color(0xFF4CAF50) else Color.Gray,
                fontSize = 12.sp
            )
        }
    }
}
