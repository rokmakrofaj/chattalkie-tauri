package com.chattalkie.app.ui.chats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chattalkie.app.ui.components.WalkieTalkieTopBar
import com.chattalkie.app.ui.chats.tabs.ChannelsTab
import com.chattalkie.app.ui.chats.tabs.MessagesTab
import com.chattalkie.app.ui.chats.tabs.HistoryTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsScreen(
    paddingValues: PaddingValues = PaddingValues(0.dp),
    onChatClick: (String, String, Boolean, Boolean) -> Unit
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("Kanallar", "Mesajlar", "Geçmiş")

    Scaffold(
        modifier = Modifier.padding(bottom = paddingValues.calculateBottomPadding()),
        containerColor = Color.White,
        topBar = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.Top
            ) {
                WalkieTalkieTopBar(showSettingsButton = false)
                
                // TabRow
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.White,
                    contentColor = Color.Black,
                    modifier = Modifier.padding(0.dp)
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    title,
                                    fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal,
                                    fontSize = 14.sp,
                                    color = Color.Black
                                )
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.White)
        ) {
            when (selectedTab) {
                0 -> ChannelsTab()
                1 -> MessagesTab(onChatClick = onChatClick)
                2 -> HistoryTab()
            }
        }
    }
}
