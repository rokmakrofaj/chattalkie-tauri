package com.chattalkie.app.ui.screens.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chattalkie.app.ui.components.WalkieTalkieTopBar
import com.chattalkie.app.viewmodel.GroupSettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupSettingsScreen(
    groupId: Int,
    onNavigateUp: () -> Unit,
    onGroupExit: () -> Unit
) {
    // 🔥 CRITICAL FIX: ViewModel KEY
    val viewModel: GroupSettingsViewModel = viewModel(
        key = "GroupSettingsViewModel_$groupId"
    )
    val members by viewModel.members.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val inviteLink by viewModel.inviteLink.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    // 🔥 Only initial load
    LaunchedEffect(groupId) {
        viewModel.loadMembers(groupId)
        viewModel.loadInviteLink(groupId)
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            WalkieTalkieTopBar(
                title = "Grup Ayarları",
                subtitle = "Üyeleri Yönet",
                showBackButton = true,
                showSettingsButton = false,
                onBackClick = onNavigateUp
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.White)
        ) {

            if (isLoading && members.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            val currentUser by com.chattalkie.app.data.AuthRepository.currentUser.collectAsState()
            val currentUserId = currentUser?.id
            val isCurrentUserAdmin = members.find { it.userId == currentUserId }?.role == "admin"



            // 🔗 Invite Link Card (Admins Only)
            if (isCurrentUserAdmin) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🔗 Davet Linki", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))

                    inviteLink?.let {
                        val link = "https://chattalkie.com/join/${it.token}"
                        val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = link,
                                color = Color.Gray,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = {
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(link))
                                android.widget.Toast.makeText(context, "Link kopyalandı", android.widget.Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Kopyala", tint = Color.Gray)
                            }
                        }
                        Spacer(Modifier.height(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    viewModel.shareInviteLink(context, it.token)
                                }
                            ) {
                                Icon(Icons.Default.Share, null)
                                Spacer(Modifier.width(6.dp))
                                Text("Paylaş")
                            }

                            OutlinedButton(
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.createInviteLink(groupId) }
                            ) {
                                Icon(Icons.Default.Refresh, null)
                                Spacer(Modifier.width(6.dp))
                                Text("Yenile")
                            }
                        }
                    } ?: run {
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { viewModel.createInviteLink(groupId) }
                        ) {
                            Icon(Icons.Default.Add, null)
                            Spacer(Modifier.width(6.dp))
                            Text("Davet Linki Oluştur")
                        }
                    }
                }
                }
            }

            Text(
                text = "Üyeler (${members.size})",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(
                    items = members,
                    key = { it.userId }
                ) { member ->
                    MemberRow(
                        member = member,
                        canKick = isCurrentUserAdmin,
                        onKick = {
                            viewModel.kickMember(groupId, member.userId)
                        }
                    )
                    HorizontalDivider()
                }
            }

            // Actions Section
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                var showLeaveDialog by remember { mutableStateOf(false) }
                var showDeleteDialog by remember { mutableStateOf(false) }
                
                // Leave Group Button (Visible to ALL members)
                OutlinedButton(
                    onClick = { showLeaveDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFF97316)
                    )
                ) {
                    Text("Gruptan Çık")
                }
                
                if (showLeaveDialog) {
                    AlertDialog(
                        onDismissRequest = { showLeaveDialog = false },
                        title = { Text("Gruptan Çık") },
                        text = { Text("Gruptan çıkmak istediğinize emin misiniz?") },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showLeaveDialog = false
                                    viewModel.leaveGroup(groupId) { onGroupExit() }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316))
                            ) {
                                Text("Çık")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showLeaveDialog = false }) {
                                Text("İptal")
                            }
                        }
                    )
                }
                
                // Delete Group Button (Visible ONLY to Admins)
                if (isCurrentUserAdmin) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEF4444)
                        )
                    ) {
                        Text("Grubu Sil")
                    }
                    
                    if (showDeleteDialog) {
                        AlertDialog(
                            onDismissRequest = { showDeleteDialog = false },
                            title = { Text("Grubu Sil") },
                            text = { Text("Bu grubu silmek istediğinizden emin misiniz? Bu işlem geri alınamaz.") },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showDeleteDialog = false
                                        viewModel.deleteGroup(groupId) { onGroupExit() }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                                ) {
                                    Text("Sil")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteDialog = false }) {
                                    Text("İptal")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MemberRow(
    member: com.chattalkie.app.data.remote.model.GroupMemberResponse,
    canKick: Boolean,
    onKick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(member.name, fontWeight = FontWeight.SemiBold)
            Text(
                if (member.role == "admin") "👑 Yönetici" else "Üye",
                color = if (member.role == "admin") Color(0xFFF97316) else Color.Gray
            )
        }

        if (canKick && member.role != "admin") {
            IconButton(onClick = onKick) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Çıkar",
                    tint = Color(0xFFEF4444)
                )
            }
        }
    }
}
