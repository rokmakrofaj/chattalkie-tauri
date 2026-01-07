package com.chattalkie.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chattalkie.app.data.GroupRepository
import kotlinx.coroutines.launch

@Composable
fun JoinGroupDialog(
    inviteToken: String,
    onDismiss: () -> Unit,
    onJoin: (String) -> Unit
) {
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "🔗 Grup Daveti",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (successMessage != null) {
                    Text(successMessage!!, color = Color(0xFF4CAF50))
                } else if (errorMessage != null) {
                    Text(errorMessage!!, color = Color(0xFFEF4444))
                } else {
                    Text("Bu gruba katılmak istiyor musunuz?")
                }
            }
        },
        confirmButton = {
            if (!isLoading && successMessage == null) {
                Button(
                    onClick = {
                        isLoading = true
                        scope.launch {
                            try {
                                val response = GroupRepository.joinGroupByInvite(inviteToken)
                                if (response != null) {
                                    successMessage = response.message
                                    android.util.Log.d("JoinGroup", "Successfully joined: ${response.groupName}")
                                    // Wait a bit then dismiss
                                    kotlinx.coroutines.delay(1500)
                                    onDismiss()
                                } else {
                                    errorMessage = "Gruba katılırken bir hata oluştu"
                                }
                            } catch (e: Exception) {
                                errorMessage = "Hata: ${e.message}"
                                android.util.Log.e("JoinGroup", "Error", e)
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316))
                ) {
                    Text("Katıl")
                }
            }
        },
        dismissButton = {
            if (!isLoading) {
                TextButton(onClick = onDismiss) {
                    Text("İptal")
                }
            }
        }
    )
}
