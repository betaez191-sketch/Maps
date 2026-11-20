package com.example.ui.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddLocationAlt
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.R
import com.example.data.auth.AuthRepository
import com.example.data.prefs.OnboardingPreferences
import com.example.data.repository.WorldlyRepository
import com.example.domain.model.UiState
import com.example.ui.components.WorldlyBanner
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.FriendsScreen
import com.example.ui.screens.MapScreen
import com.example.ui.screens.MessagesScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PlacesAndHistoryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ShareLocationScreen
import com.example.ui.theme.WorldlyAmber
import com.example.ui.theme.WorldlyBorder
import com.example.ui.theme.WorldlyCyan
import com.example.ui.theme.WorldlyCyanDim
import com.example.ui.theme.WorldlyLavender
import com.example.ui.theme.WorldlyMidnight
import com.example.ui.theme.WorldlyObsidian
import com.example.ui.theme.WorldlyOnAccent
import com.example.ui.theme.WorldlyPink
import com.example.ui.theme.WorldlyRose
import com.example.ui.theme.WorldlySlate
import com.example.ui.theme.WorldlyTextMuted
import com.example.ui.theme.WorldlyTextPrimary
import com.example.ui.theme.WorldlyTextSecondary
import com.example.ui.viewmodel.WorldlyViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data object RadarRoute

@Serializable
data object ShareRoute

@Serializable
data object MessagesRoute

@Serializable
data object SettingsRoute

@Serializable
data object FriendsRoute

@Serializable
data class PlacesRoute(val initialTab: Int = 0)

private data class CosmicNavTab<T : Any>(
    val route: T,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@Composable
fun WorldlyAppRoot(
    auth: FirebaseAuth = Firebase.auth
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val onboardingPrefs = remember(context) { OnboardingPreferences(context.applicationContext) }
    val onboardingCompleted by onboardingPrefs.onboardingCompleted
        .collectAsStateWithLifecycle(initialValue = true)
    val currentUser by AuthRepository.authStateFlow(auth)
        .collectAsStateWithLifecycle(initialValue = auth.currentUser)

    if (!onboardingCompleted) {
        OnboardingScreen(
            onFinished = {
                scope.launch { onboardingPrefs.setOnboardingCompleted(true) }
            }
        )
    } else if (currentUser == null) {
        AuthScreen(onAuthSuccess = { })
    } else {
        val uid = currentUser!!.uid
        key(uid) {
            AuthenticatedWorldlyShell(currentUserId = uid)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedWorldlyShell(currentUserId: String) {
    val context = LocalContext.current
    val viewModel: WorldlyViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                WorldlyViewModel(WorldlyRepository(db), currentUserId)
            }
        }
    )

    LaunchedEffect(currentUserId) {
        viewModel.observeIncomingNotifications(context.applicationContext)
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()
    val friendshipsState by viewModel.friendships.collectAsStateWithLifecycle()
    val pendingIncomingCount = remember(friendshipsState) {
        val list = (friendshipsState as? UiState.Success)?.data.orEmpty()
        list.count { it.status == "pending" && it.recipientUid == currentUserId }
    }

    var showQuickActionSheet by remember { mutableStateOf(false) }

    val primaryTabs = remember {
        listOf(
            CosmicNavTab(RadarRoute, R.string.nav_map, Icons.Rounded.Home, Icons.Outlined.Home, "nav_radar"),
            CosmicNavTab(ShareRoute, R.string.nav_share, Icons.Rounded.NearMe, Icons.Outlined.NearMe, "nav_share"),
            CosmicNavTab(MessagesRoute, R.string.nav_messages, Icons.Rounded.ChatBubble, Icons.Outlined.ChatBubbleOutline, "nav_messages"),
            CosmicNavTab(SettingsRoute, R.string.nav_settings, Icons.Rounded.Person, Icons.Outlined.Person, "nav_settings")
        )
    }

    fun navigateToTab(route: Any) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun openFriendChat(friendUid: String) {
        viewModel.openChat(friendUid)
        navigateToTab(MessagesRoute)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = WorldlyMidnight,
            contentWindowInsets = WindowInsets(0),
            bottomBar = {
                if (!useRail) {
                    CosmicBottomBar(
                        tabs = primaryTabs,
                        currentDestination = currentDestination,
                        pendingIncomingCount = pendingIncomingCount,
                        onTabClick = ::navigateToTab,
                        onCenterPlusClick = { showQuickActionSheet = true }
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = WorldlyObsidian
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = WorldlyCyan,
                            shape = CircleShape,
                            modifier = Modifier
                                .size(48.dp)
                                .clickable { showQuickActionSheet = true }
                                .testTag("nav_quick_add_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = "Quick Actions",
                                    tint = Color.White
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        primaryTabs.forEach { item ->
                            val selected = currentDestination?.hasRoute(item.route::class) == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(item.route) },
                                icon = {
                                    if (item.route == MessagesRoute && pendingIncomingCount > 0) {
                                        BadgedBox(
                                            badge = {
                                                Badge(containerColor = WorldlyRose) {
                                                    Text("$pendingIncomingCount")
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                                contentDescription = stringResource(item.labelRes)
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = stringResource(item.labelRes)
                                        )
                                    }
                                },
                                label = {
                                    Text(
                                        text = stringResource(item.labelRes),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = WorldlyOnAccent,
                                    selectedTextColor = WorldlyLavender,
                                    indicatorColor = WorldlyCyan,
                                    unselectedIconColor = WorldlyTextSecondary,
                                    unselectedTextColor = WorldlyTextSecondary
                                ),
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.TopCenter
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = RadarRoute,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable<RadarRoute> {
                            MapScreen(
                                viewModel = viewModel,
                                onOpenChat = ::openFriendChat,
                                onOpenPlaces = { navController.navigate(PlacesRoute(0)) },
                                onOpenFriends = { navController.navigate(FriendsRoute) }
                            )
                        }
                        composable<ShareRoute> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                ShareLocationScreen(
                                    viewModel = viewModel,
                                    onOpenFriends = { navController.navigate(FriendsRoute) },
                                    modifier = Modifier
                                        .widthIn(max = 720.dp)
                                        .statusBarsPadding()
                                )
                            }
                        }
                        composable<MessagesRoute> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                MessagesScreen(
                                    viewModel = viewModel,
                                    onOpenFriends = { navController.navigate(FriendsRoute) },
                                    modifier = Modifier.widthIn(max = 720.dp)
                                )
                            }
                        }
                        composable<SettingsRoute> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                SettingsScreen(
                                    viewModel = viewModel,
                                    onOpenPlaces = { tab -> navController.navigate(PlacesRoute(tab)) },
                                    modifier = Modifier.widthIn(max = 720.dp)
                                )
                            }
                        }
                        composable<FriendsRoute> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                FriendsScreen(
                                    viewModel = viewModel,
                                    onOpenChat = ::openFriendChat,
                                    onBack = { navController.popBackStack() },
                                    modifier = Modifier.widthIn(max = 720.dp)
                                )
                            }
                        }
                        composable<PlacesRoute> { backStackEntry ->
                            val args = backStackEntry.toRoute<PlacesRoute>()
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                PlacesAndHistoryScreen(
                                    viewModel = viewModel,
                                    initialTab = args.initialTab,
                                    onBack = { navController.popBackStack() },
                                    modifier = Modifier.widthIn(max = 720.dp)
                                )
                            }
                        }
                    }

                    WorldlyBanner(
                        message = statusBanner,
                        onDismiss = { viewModel.clearBanner() },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .statusBarsPadding()
                            .widthIn(max = 600.dp)
                    )
                }
            }
        }
    }

    if (showQuickActionSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQuickActionSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = WorldlyObsidian,
            contentColor = WorldlyTextPrimary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = WorldlyTextPrimary
                )
                QuickActionTile(
                    icon = Icons.Rounded.PersonAdd,
                    title = "Add Friend or View Circle",
                    subtitle = "Search by @handle or share your Friend Code",
                    accent = WorldlyLavender,
                    testTag = "quick_action_add_friend",
                    onClick = {
                        showQuickActionSheet = false
                        navController.navigate(FriendsRoute)
                    }
                )
                QuickActionTile(
                    icon = Icons.Rounded.AddLocationAlt,
                    title = "Favorite Places & History",
                    subtitle = "Bookmark hangout spots or inspect private GPS timeline",
                    accent = WorldlyAmber,
                    testTag = "quick_action_places",
                    onClick = {
                        showQuickActionSheet = false
                        navController.navigate(PlacesRoute(0))
                    }
                )
                QuickActionTile(
                    icon = Icons.Rounded.NearMe,
                    title = "Live Location Controls",
                    subtitle = "Start, time-box, or stop live sharing immediately",
                    accent = WorldlyCyan,
                    testTag = "quick_action_share",
                    onClick = {
                        showQuickActionSheet = false
                        navigateToTab(ShareRoute)
                    }
                )
                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }
}

@Composable
private fun CosmicBottomBar(
    tabs: List<CosmicNavTab<out Any>>,
    currentDestination: androidx.navigation.NavDestination?,
    pendingIncomingCount: Int,
    onTabClick: (Any) -> Unit,
    onCenterPlusClick: () -> Unit
) {
    Surface(
        color = WorldlyObsidian,
        border = BorderStroke(1.dp, WorldlyBorder.copy(alpha = 0.7f)),
        shadowElevation = 16.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(68.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            // Left 2 tabs: Home, Share
            tabs.take(2).forEach { item ->
                CosmicBottomBarItem(
                    item = item,
                    selected = currentDestination?.hasRoute(item.route::class) == true,
                    badgeCount = 0,
                    onClick = { onTabClick(item.route) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Center Glowing Purple '+' Button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .offset(y = (-6).dp)
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(WorldlyCyanDim, WorldlyCyan, WorldlyPink)
                            )
                        )
                        .clickable(onClick = onCenterPlusClick)
                        .testTag("nav_quick_add_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Quick Actions",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Right 2 tabs: Chat, Profile
            tabs.drop(2).forEach { item ->
                val badge = if (item.route == MessagesRoute) pendingIncomingCount else 0
                CosmicBottomBarItem(
                    item = item,
                    selected = currentDestination?.hasRoute(item.route::class) == true,
                    badgeCount = badge,
                    onClick = { onTabClick(item.route) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CosmicBottomBarItem(
    item: CosmicNavTab<out Any>,
    selected: Boolean,
    badgeCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag(item.testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val tint = if (selected) WorldlyLavender else WorldlyTextMuted
        if (badgeCount > 0) {
            BadgedBox(
                badge = {
                    Badge(containerColor = WorldlyRose) {
                        Text("$badgeCount")
                    }
                }
            ) {
                Icon(
                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                    contentDescription = stringResource(item.labelRes),
                    tint = tint,
                    modifier = Modifier.size(22.dp)
                )
            }
        } else {
            Icon(
                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                contentDescription = stringResource(item.labelRes),
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = stringResource(item.labelRes),
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) WorldlyTextPrimary else WorldlyTextMuted,
            maxLines = 1
        )
    }
}

@Composable
private fun QuickActionTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        color = WorldlySlate,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, WorldlyBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = WorldlyTextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = WorldlyTextSecondary
                )
            }
        }
    }
}
