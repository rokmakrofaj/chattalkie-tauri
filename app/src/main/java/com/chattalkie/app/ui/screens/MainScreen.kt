package com.chattalkie.app.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.chattalkie.app.navigation.AppNavGraph
import com.chattalkie.app.navigation.AppRoutes
import com.chattalkie.app.ui.components.AppDrawer
import androidx.compose.runtime.collectAsState
import com.chattalkie.app.data.AuthRepository

private sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    object Home : BottomNavItem(AppRoutes.HOME, Icons.Filled.Home, "Home")
    object Chats : BottomNavItem(AppRoutes.CHATS, Icons.Filled.Message, "Chats")
    object Settings : BottomNavItem(AppRoutes.SETTINGS, Icons.Filled.Settings, "Settings")
}

@Composable
fun MainScreen(
    inviteToken: String? = null
) {
    val navController = rememberNavController()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // 1. Join Dialog with consumed token logic (Prevents reappearing on rotation/recomposition)
    var consumedToken by rememberSaveable { mutableStateOf<String?>(null) }
    val shouldShowJoinDialog = inviteToken != null && inviteToken != consumedToken

    if (shouldShowJoinDialog) {
        JoinGroupDialog(
            inviteToken = inviteToken!!,
            onDismiss = { consumedToken = inviteToken },
            onJoin = { token ->
                consumedToken = inviteToken
            }
        )
    }
    
    val currentUser by AuthRepository.currentUser.collectAsState()
    val userId = currentUser?.id
    
    // 2. Optimized Socket Connection (only reconnect on userId change)
    androidx.compose.runtime.LaunchedEffect(userId) {
        if (userId != null) {
            com.chattalkie.app.data.ChatRepository.connect()
        } else {
            com.chattalkie.app.data.ChatRepository.disconnect()
        }
    }

    val bottomNavItems = listOf(
        BottomNavItem.Home,
        BottomNavItem.Chats,
        BottomNavItem.Settings
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    // 3. Robust Bottom Bar Visibility (Handles nested graphs/arguments)
    val showBottomBar = currentDestination?.hierarchy?.any { destination ->
        destination.route == AppRoutes.HOME || 
        destination.route == AppRoutes.CHATS || 
        destination.route == AppRoutes.SETTINGS
    } == true
    
    // Only allow drawer gestures on HOME screen
    val enableDrawerGestures = currentRoute == AppRoutes.HOME

    ModalNavigationDrawer(
        drawerState = drawerState,
        scrimColor = Color.Transparent,
        gesturesEnabled = enableDrawerGestures,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxWidth(0.7f),
                drawerContainerColor = Color.White,
                drawerShape = androidx.compose.ui.graphics.RectangleShape,
                windowInsets = WindowInsets(0, 0, 0, 0)
            ) {
                AppDrawer(
                    user = currentUser,
                    onProfileClick = {
                        scope.launch {
                            drawerState.close()
                        }
                        // TODO: Navigate to profile
                    },
                    onLogoutClick = {
                        scope.launch {
                            drawerState.close()
                            AuthRepository.logout()
                            navController.navigate(AppRoutes.LOGIN) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                )
            }
        }
    ) {
        Scaffold(
            containerColor = Color.White,
            contentWindowInsets = WindowInsets(0), // Disable default status bar padding - each screen handles it
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ) {
                        bottomNavItems.forEach { screen ->
                            NavigationBarItem(
                                icon = { Icon(screen.icon, contentDescription = screen.label) },
                                label = { Text(screen.label) },
                                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            AppNavGraph(
                modifier = Modifier,
                paddingValues = innerPadding,
                navController = navController,
                onOpenDrawer = {
                    scope.launch {
                        drawerState.open()
                    }
                }
            )
        }
    }
}
