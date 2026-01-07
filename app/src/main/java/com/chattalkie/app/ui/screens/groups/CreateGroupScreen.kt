package com.chattalkie.app.ui.screens.groups

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chattalkie.app.ui.components.UserAvatar
import com.chattalkie.app.viewmodel.GroupViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(
    viewModel: GroupViewModel,
    onGroupCreated: (Int) -> Unit,
    onNavigateUp: () -> Unit
) {
    var groupName by remember { mutableStateOf("") }
    val friends by viewModel.friends.collectAsState()
    val selectedIds by viewModel.selectedFriendIds.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val groupCreatedId by viewModel.groupCreated.collectAsState()

    LaunchedEffect(groupCreatedId) {
        groupCreatedId?.let {
            onGroupCreated(it)
            viewModel.resetGroupCreated()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Grup Oluştur", fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        IconButton(
                            onClick = { viewModel.createGroup(groupName) },
                            enabled = groupName.isNotBlank()
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Tamamla", tint = if (groupName.isNotBlank()) Color(0xFF2196F3) else Color.Gray)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Group Name Input
            OutlinedTextField(
                value = groupName,
                onValueChange = { groupName = it },
                label = { Text("Grup Adı") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Group, contentDescription = null) },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Katılımcı Seç (${selectedIds.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (friends.isEmpty() && !isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Henüz arkadaşın yok.", color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(friends) { friend ->
                        FriendSelectionRow(
                            friendName = friend.name,
                            isSelected = selectedIds.contains(friend.id),
                            onToggle = { viewModel.toggleFriendSelection(friend.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FriendSelectionRow(
    friendName: String,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(userName = friendName, size = 40.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Text(friendName, modifier = Modifier.weight(1f), fontSize = 16.sp)
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF2196F3))
        )
    }
}
