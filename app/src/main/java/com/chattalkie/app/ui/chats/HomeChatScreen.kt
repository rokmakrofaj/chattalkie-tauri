package com.chattalkie.app.ui.chats

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Mic
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.draw.scale
import com.chattalkie.app.ui.components.MessageBubble
import com.chattalkie.app.ui.components.WalkieTalkieTopBar
import com.chattalkie.app.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun HomeChatScreen(
    viewModel: ChatViewModel,
    partnerName: String,
    isAdmin: Boolean = false,
    isGroup: Boolean = false,
    onNavigateUp: () -> Unit,
    onSettingsClick: () -> Unit = {}
) {
    val messages by viewModel.messages.collectAsState()
    val partnerStatus by viewModel.partnerStatus.collectAsState()
    val groupCreatedAt by viewModel.groupCreatedAt.collectAsState()
    val draft by viewModel.draft.collectAsState()
    val listState = rememberLazyListState()
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            if (listState.firstVisibleItemIndex <= 1) {
                listState.animateScrollToItem(0)
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.White,
        topBar = {
            WalkieTalkieTopBar(
                title = partnerName,
                subtitle = if (isGroup) "Grup Sohbeti" else when (partnerStatus) {
                    "online" -> "Çevrimiçi"
                    "offline" -> "Çevrimdışı"
                    else -> partnerStatus
                },
                showBackButton = true,
                showSettingsButton = isGroup,
                onBackClick = onNavigateUp,
                onSettingsClick = onSettingsClick
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
            ) {
                MessageInput(
                    text = draft, // Bind to draft (Source of Truth)
                    onTextChanged = viewModel::onDraftChanged,
                    onSendMessage = viewModel::sendMessage,
                    onImageSelected = { uri -> 
                        viewModel.handleImageSelection(uri, context)
                    },
                    onVoiceRecorded = { file ->
                        viewModel.sendVoice(file)
                    }
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFECE5DD))
                .padding(padding)
                .padding(horizontal = 8.dp),
            state = listState,
            reverseLayout = true
        ) {
            items(
                items = messages,
                key = { it.id ?: it.hashCode() }
            ) { message ->
                MessageBubble(
                    message = message,
                    onResend = viewModel::resendMessage
                )
                
                LaunchedEffect(message.id) {
                    viewModel.markMessageAsRead(message)
                }
            }

            if (isGroup && groupCreatedAt != null) {
                item {
                    val instant = java.time.Instant.ofEpochMilli(groupCreatedAt!!)
                    val formatter = java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy HH:mm", java.util.Locale("tr"))
                    val dateStr = instant.atZone(java.time.ZoneId.systemDefault()).format(formatter)
                    
                    Text(
                        text = "$dateStr oluşturuldu",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageInput(
    text: String,
    onTextChanged: (String) -> Unit,
    onSendMessage: (String) -> Unit,
    onImageSelected: (android.net.Uri) -> Unit,
    onVoiceRecorded: (java.io.File) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val voiceRecorder = remember { com.chattalkie.app.ui.utils.VoiceRecorder(context) }
    var isRecording by remember { mutableStateOf(false) }

    // Permission Launcher
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            if (voiceRecorder.startRecording()) {
                isRecording = true
            }
        }
    }

    Surface(
        tonalElevation = 2.dp,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji button
            IconButton(onClick = { /* TODO: Open emoji picker */ }) {
                Icon(
                    imageVector = Icons.Default.EmojiEmotions,
                    contentDescription = "Emoji",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // TextField
            TextField(
                value = text,
                onValueChange = onTextChanged,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Message") },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                maxLines = 5
            )


            // Attachment button
            val pickMedia = androidx.activity.compose.rememberLauncherForActivityResult(
                androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
            ) { uri ->
                if (uri != null) {
                    onImageSelected(uri)
                }
            }
            
            IconButton(onClick = { 
                pickMedia.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly))
            }) {
                Icon(
                    imageVector = Icons.Default.AttachFile,
                    contentDescription = "Attach",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Send or Voice button
            if (text.isNotBlank()) {
                IconButton(
                    onClick = {
                        onSendMessage(text)
                        // Text clearing is handled by ViewModel updating the state
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                IconButton(
                    onClick = { /* Handle click if needed */ },
                    modifier = Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                // Start Recording
                                if (androidx.core.content.ContextCompat.checkSelfPermission(
                                        context,
                                        android.Manifest.permission.RECORD_AUDIO
                                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                ) {
                                    if (voiceRecorder.startRecording()) {
                                        isRecording = true
                                        tryAwaitRelease()
                                        // Stop Recording
                                        isRecording = false
                                        val file = voiceRecorder.stopRecording()
                                        if (file != null) {
                                            onVoiceRecorded(file)
                                        }
                                    }
                                } else {
                                    permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        )
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice message",
                        tint = if (isRecording) Color.Red else MaterialTheme.colorScheme.primary,
                        modifier = if (isRecording) Modifier.scale(1.2f) else Modifier
                    )
                }
            }
        }
    }
}

