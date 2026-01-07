package com.chattalkie.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import com.chattalkie.app.ui.chats.HomeChatScreen
import com.chattalkie.app.ui.chats.ChatsScreen
import com.chattalkie.app.ui.screens.HomeScreen
import com.chattalkie.app.ui.screens.LoginScreen
import com.chattalkie.app.ui.screens.RegisterScreen
import com.chattalkie.app.ui.screens.SettingsScreen
import com.chattalkie.app.viewmodel.ChatViewModel
import com.chattalkie.app.data.AuthRepository
import kotlinx.coroutines.launch
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

object AppRoutes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val CHATS = "chats"
    const val CHAT_DETAIL = "chat/{chatId}/{partnerName}/{isGroup}/{isAdmin}"
    const val SETTINGS = "settings"
    const val ADD_FRIEND = "add_friend"
    const val FRIENDS = "friends"
    const val CREATE_GROUP = "create_group"
    const val GROUP_SETTINGS = "group_settings/{groupId}"

    fun chatDetail(chatId: Int, partnerName: String, isGroup: Boolean = false, isAdmin: Boolean = false) = 
        "chat/$chatId/$partnerName/$isGroup/$isAdmin"
    
    fun groupSettings(groupId: Int) = "group_settings/$groupId"
}

@Composable
fun AppNavGraph(
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    navController: NavHostController,
    onOpenDrawer: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    NavHost(
        navController = navController,
        startDestination = AppRoutes.LOGIN,
        modifier = modifier,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None }
    ) {
        composable(AppRoutes.LOGIN) { backStackEntry ->
            val authViewModel: com.chattalkie.app.viewmodel.AuthViewModel = viewModel(backStackEntry)
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate(AppRoutes.HOME) {
                        popUpTo(AppRoutes.LOGIN) { inclusive = true }
                    }
                },
                onRegisterClick = {
                    navController.navigate(AppRoutes.REGISTER)
                }
            )
        }
        
        composable(AppRoutes.REGISTER) { backStackEntry ->
            val authViewModel: com.chattalkie.app.viewmodel.AuthViewModel = viewModel(backStackEntry)
            RegisterScreen(
                viewModel = authViewModel,
                onRegisterSuccess = {
                    navController.navigate(AppRoutes.HOME) {
                        popUpTo(AppRoutes.REGISTER) { inclusive = true }
                    }
                },
                onLoginClick = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(AppRoutes.HOME) { backStackEntry ->
            val parentEntry = androidx.compose.runtime.remember(backStackEntry) {
                navController.getBackStackEntry(AppRoutes.HOME)
            }
            val messagesViewModel: com.chattalkie.app.viewmodel.MessagesViewModel = androidx.lifecycle.viewmodel.compose.viewModel(parentEntry)
            HomeScreen(
                viewModel = messagesViewModel,
                paddingValues = paddingValues,
                onConversationClick = { id, name, isGroup, isAdmin ->
                    // id is like "p_123" or "g_456". Parse Int ID here logic or assume caller passes raw Int?
                    // Previous code passed "p_123". Now we need Int.
                    // Parse:
                    val rawId = id.substringAfter("_").toIntOrNull() ?: 0
                    navController.navigate(AppRoutes.chatDetail(rawId, name, isGroup, isAdmin))
                },
                onAddFriendClick = {
                    navController.navigate(AppRoutes.ADD_FRIEND)
                },
                onCreateGroupClick = {
                    navController.navigate(AppRoutes.CREATE_GROUP)
                },
                onStatusClick = {
                    navController.navigate(AppRoutes.FRIENDS)
                },
                onAvatarClick = onOpenDrawer,
                onJoinGroupByLink = { inviteToken ->
                    messagesViewModel.joinGroup(inviteToken) { message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        composable(AppRoutes.CREATE_GROUP) { backStackEntry ->
            val groupViewModel: com.chattalkie.app.viewmodel.GroupViewModel = viewModel(backStackEntry)
            com.chattalkie.app.ui.screens.groups.CreateGroupScreen(
                viewModel = groupViewModel,
                onGroupCreated = { groupId ->
                    // For now go back to home or to a new group chat screen if implemented
                    navController.popBackStack()
                    Toast.makeText(context, "Grup başarıyla oluşturuldu!", Toast.LENGTH_SHORT).show()
                },
                onNavigateUp = { navController.navigateUp() }
            )
        }

        composable(AppRoutes.FRIENDS) { backStackEntry ->
            val friendViewModel: com.chattalkie.app.viewmodel.FriendViewModel = viewModel(backStackEntry)
            com.chattalkie.app.ui.screens.friends.FriendsScreen(
                viewModel = friendViewModel,
                paddingValues = paddingValues,
                onNavigateUp = { navController.navigateUp() }
            )
        }

        composable(AppRoutes.CHATS) {
            ChatsScreen(
                paddingValues = paddingValues,
                onChatClick = { id, name, isGroup, isAdmin ->
                    val rawId = id.substringAfter("_").toIntOrNull() ?: 0
                    navController.navigate(AppRoutes.chatDetail(rawId, name, isGroup, isAdmin))
            })
        }

        composable(
            route = AppRoutes.CHAT_DETAIL,
            arguments = listOf(
                navArgument("chatId") { type = NavType.IntType },
                navArgument("partnerName") { type = NavType.StringType },
                navArgument("isGroup") { type = NavType.BoolType },
                navArgument("isAdmin") { type = NavType.BoolType }
            )
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getInt("chatId") ?: 0
            val partnerName = backStackEntry.arguments?.getString("partnerName") ?: "Bilinmeyen"
            val isGroup = backStackEntry.arguments?.getBoolean("isGroup") ?: false
            val isAdmin = backStackEntry.arguments?.getBoolean("isAdmin") ?: false
            
            // Reconstruct fullId for internal VM logic if needed, or pass split params
            // VM typically needs "g_123" or "123, isGroup".
            // Let's check `HomeChatScreen` and `ChatViewModel`.
            // ChatViewModel.initChat(chatId, ...) takes Int.
            // But previous code constructed fullId from route string arg?
            // "val fullId = backStackEntry.arguments?.getString("chatId")"
            // "val chatId = fullId.substringAfter..."
            // "chatViewModel.initChat(chatId, ...)"
            // So VM *expects* Int chatId.
            // We just need to pass Int directly.
            
            val chatViewModel: ChatViewModel = viewModel(backStackEntry)
            // Sohbeti ilkle
            androidx.compose.runtime.LaunchedEffect(Unit) {
                chatViewModel.initChat(chatId, partnerName, isGroup)
            }
            
            HomeChatScreen(
                viewModel = chatViewModel,
                partnerName = partnerName,
                isAdmin = isAdmin,
                isGroup = isGroup,
                onNavigateUp = { navController.navigateUp() },
                onSettingsClick = {
                    navController.navigate(AppRoutes.groupSettings(chatId))
                }
            )
        }

        composable(
            route = AppRoutes.GROUP_SETTINGS,
            arguments = listOf(navArgument("groupId") { type = NavType.IntType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getInt("groupId") ?: 0
            com.chattalkie.app.ui.screens.groups.GroupSettingsScreen(
                groupId = groupId,
                onNavigateUp = { navController.navigateUp() },
                onGroupExit = { 
                    navController.popBackStack(AppRoutes.HOME, inclusive = false)
                }
            )
        }

        composable(AppRoutes.SETTINGS) {
            SettingsScreen(
                onNavigateUp = { navController.navigateUp() },
                onLogout = {
                    navController.popBackStack(AppRoutes.HOME, inclusive = false)
                }
            )
        }

        composable(AppRoutes.ADD_FRIEND) { backStackEntry ->
            val friendViewModel: com.chattalkie.app.viewmodel.FriendViewModel = viewModel(backStackEntry)
            com.chattalkie.app.ui.screens.friends.AddFriendScreen(
                viewModel = friendViewModel,
                onNavigateUp = { navController.navigateUp() }
            )
        }
    }
}

@Composable
private fun PlaceholderScreen(screenName: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "$screenName Screen")
    }
}
