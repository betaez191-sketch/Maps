package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.ChatMessage
import com.example.domain.model.Friendship
import com.example.domain.model.UiState
import com.example.domain.util.GeoUtils
import com.example.ui.components.AvatarBadge
import com.example.ui.components.BatteryChip
import com.example.ui.components.CosmicSkyBackdrop
import com.example.ui.components.StatusPill
import com.example.ui.theme.WorldlyAmber
import com.example.ui.theme.WorldlyBorder
import com.example.ui.theme.WorldlyCyan
import com.example.ui.theme.WorldlyCyanDim
import com.example.ui.theme.WorldlyEmerald
import com.example.ui.theme.WorldlyGlass
import com.example.ui.theme.WorldlyLavender
import com.example.ui.theme.WorldlyMidnight
import com.example.ui.theme.WorldlyObsidian
import com.example.ui.theme.WorldlyOnAccent
import com.example.ui.theme.WorldlyRose
import com.example.ui.theme.WorldlySlate
import com.example.ui.theme.WorldlyTextMuted
import com.example.ui.theme.WorldlyTextPrimary
import com.example.ui.theme.WorldlyTextSecondary
import com.example.ui.viewmodel.WorldlyViewModel
import java.util.Locale

private enum class ChatFilterTab(val label: String) {
    ALL("All"),
    FRIENDS("Friends"),
    REQUESTS("Requests")
}

@Composable
fun MessagesScreen(
    viewModel: WorldlyViewModel,
    onOpenFriends: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeFriendUid by viewModel.activeChatFriendUid.collectAsStateWithLifecycle()
    val friendshipsState by viewModel.friendships.collectAsStateWithLifecycle()
    val friendLocations by viewModel.friendLocations.collectAsStateWithLifecycle()
    val chatsState by viewModel.chats.collectAsStateWithLifecycle()
    val myLocation by viewModel.mySharedLocation.collectAsStateWithLifecycle()
    val localFix by viewModel.localDeviceFix.collectAsStateWithLifecycle()

    val friendships = (friendshipsState as? UiState.Success)?.data.orEmpty()
    val acceptedFriends = remember(friendships) { friendships.filter { it.status == "accepted" } }
    val incomingRequests = remember(friendships, viewModel.currentUserId) {
        friendships.filter { it.status == "pending" && it.recipientUid == viewModel.currentUserId }
    }
    val outgoingRequests = remember(friendships, viewModel.currentUserId) {
        friendships.filter { it.status == "pending" && it.requesterUid == viewModel.currentUserId }
    }
    val activeFriendship = remember(acceptedFriends, activeFriendUid) {
        acceptedFriends.firstOrNull { it.otherUid(viewModel.currentUserId) == activeFriendUid }
    }

    if (activeFriendUid != null && activeFriendship != null) {
        BackHandler { viewModel.openChat(null) }
        ChatThreadView(
            viewModel = viewModel,
            friendship = activeFriendship,
            onBack = { viewModel.openChat(null) },
            modifier = modifier
        )
    } else {
        var selectedFilter by remember { mutableStateOf(ChatFilterTab.ALL) }
        var showSearchBar by remember { mutableStateOf(false) }
        var filterQuery by remember { mutableStateOf("") }

        val chats = (chatsState as? UiState.Success)?.data.orEmpty()
        val chatByPairId = remember(chats) { chats.associateBy { it.id } }
        val locationMap = remember(friendLocations) { friendLocations.associateBy { it.userId } }
        val myLat = myLocation?.takeIf { it.sharingActive }?.latitude ?: localFix?.latitude
        val myLng = myLocation?.takeIf { it.sharingActive }?.longitude ?: localFix?.longitude

        val filteredFriends = remember(acceptedFriends, filterQuery) {
            val q = filterQuery.trim().lowercase(Locale.US)
            if (q.isBlank()) acceptedFriends
            else acceptedFriends.filter {
                it.otherName(viewModel.currentUserId).lowercase(Locale.US).contains(q) ||
                    it.otherHandle(viewModel.currentUserId).lowercase(Locale.US).contains(q)
            }
        }

        Box(modifier = modifier.fillMaxSize()) {
            CosmicSkyBackdrop(
                modifier = Modifier.fillMaxSize(),
                showSkyline = false,
                showClouds = false
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Chat",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = WorldlyTextPrimary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = {
                                showSearchBar = !showSearchBar
                                if (!showSearchBar) filterQuery = ""
                            },
                            modifier = Modifier.testTag("chat_search_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (showSearchBar) Icons.Rounded.Close else Icons.Rounded.Search,
                                contentDescription = "Search chats",
                                tint = WorldlyTextPrimary
                            )
                        }
                        IconButton(
                            onClick = onOpenFriends,
                            modifier = Modifier.testTag("chat_add_friend_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PersonAdd,
                                contentDescription = "Add friend or manage circle",
                                tint = WorldlyLavender
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = showSearchBar) {
                    OutlinedTextField(
                        value = filterQuery,
                        onValueChange = { filterQuery = it.take(40) },
                        placeholder = { Text("Filter friends by name or @handle...") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .testTag("chat_filter_input")
                    )
                }

                // Filter Pills: All | Friends | Requests
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ChatFilterTab.entries.forEach { tab ->
                        val active = tab == selectedFilter
                        val badgeCount = if (tab == ChatFilterTab.REQUESTS) incomingRequests.size else 0
                        Surface(
                            color = if (active) WorldlyCyan else WorldlyObsidian.copy(alpha = 0.78f),
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(
                                1.dp,
                                if (active) WorldlyLavender else WorldlyBorder
                            ),
                            modifier = Modifier
                                .clickable { selectedFilter = tab }
                                .testTag("chat_tab_${tab.name.lowercase(Locale.US)}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = tab.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (active) Color.White else WorldlyTextSecondary
                                )
                                if (badgeCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(WorldlyRose),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$badgeCount",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 6.dp,
                        bottom = 96.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (friendshipsState is UiState.Loading || chatsState is UiState.Loading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = WorldlyCyan)
                            }
                        }
                    } else if (selectedFilter == ChatFilterTab.REQUESTS) {
                        if (incomingRequests.isEmpty() && outgoingRequests.isEmpty()) {
                            item {
                                EmptyChatCard(
                                    title = "No Pending Requests",
                                    body = "Tap the + Friend button to search an explorer by @handle or share your Friend Code.",
                                    actionLabel = "Find Friends",
                                    onAction = onOpenFriends
                                )
                            }
                        } else {
                            if (incomingRequests.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "Incoming Requests (${incomingRequests.size})",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = WorldlyLavender
                                    )
                                }
                                items(incomingRequests, key = { it.id }) { req ->
                                    Surface(
                                        color = WorldlyObsidian.copy(alpha = 0.88f),
                                        shape = RoundedCornerShape(18.dp),
                                        border = BorderStroke(1.dp, WorldlyCyan.copy(alpha = 0.45f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            AvatarBadge(name = req.requesterName, colorHex = req.requesterColor)
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(req.requesterName, color = WorldlyTextPrimary)
                                                Text("@${req.requesterHandle}", color = WorldlyTextSecondary)
                                            }
                                            IconButton(onClick = { viewModel.removeFriendship(req.id) }) {
                                                Icon(Icons.Rounded.Close, contentDescription = "Decline", tint = WorldlyRose)
                                            }
                                            Button(
                                                onClick = { viewModel.acceptFriendRequest(req.id) },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = WorldlyEmerald,
                                                    contentColor = WorldlyMidnight
                                                )
                                            ) {
                                                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Accept")
                                            }
                                        }
                                    }
                                }
                            }
                            if (outgoingRequests.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "Sent Requests (${outgoingRequests.size})",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = WorldlyTextSecondary
                                    )
                                }
                                items(outgoingRequests, key = { it.id }) { req ->
                                    Surface(
                                        color = WorldlyObsidian.copy(alpha = 0.82f),
                                        shape = RoundedCornerShape(16.dp),
                                        border = BorderStroke(1.dp, WorldlyBorder),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            AvatarBadge(name = req.recipientName, colorHex = req.recipientColor, size = 38.dp)
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(req.recipientName, color = WorldlyTextPrimary)
                                                Text("@${req.recipientHandle} · Pending", color = WorldlyAmber)
                                            }
                                            TextButton(onClick = { viewModel.removeFriendship(req.id) }) {
                                                Text("Cancel", color = WorldlyTextSecondary)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else if (filteredFriends.isEmpty()) {
                        item {
                            EmptyChatCard(
                                title = if (acceptedFriends.isEmpty()) "No Conversations Yet" else "No Matches",
                                body = if (acceptedFriends.isEmpty()) {
                                    "Add or accept a friend to start private 1-to-1 chats and drop live location pins."
                                } else {
                                    "Try another name or @handle."
                                },
                                actionLabel = "Add Friends",
                                onAction = onOpenFriends
                            )
                        }
                    } else {
                        items(filteredFriends, key = { it.id }) { friendship ->
                            val friendUid = friendship.otherUid(viewModel.currentUserId)
                            val pairId = GeoUtils.pairId(viewModel.currentUserId, friendUid)
                            val thread = chatByPairId[pairId]
                            val friendName = friendship.otherName(viewModel.currentUserId)
                            val friendHandle = friendship.otherHandle(viewModel.currentUserId)
                            val friendColor = friendship.otherColor(viewModel.currentUserId)
                            val friendLoc = locationMap[friendUid]
                            val friendPausedMe = friendUid in friendship.pausedByUids
                            val isLive = friendLoc != null && friendLoc.sharingActive && !friendPausedMe

                            if (selectedFilter == ChatFilterTab.FRIENDS) {
                                val distText = if (isLive && myLat != null && myLng != null) {
                                    GeoUtils.formatDistance(
                                        GeoUtils.distanceMeters(myLat, myLng, friendLoc.latitude, friendLoc.longitude)
                                    )
                                } else null
                                Surface(
                                    color = WorldlyObsidian.copy(alpha = 0.88f),
                                    shape = RoundedCornerShape(18.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isLive) WorldlyEmerald.copy(alpha = 0.45f) else WorldlyBorder
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.openChat(friendUid) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        AvatarBadge(
                                            name = friendName,
                                            colorHex = friendColor,
                                            size = 48.dp,
                                            isLive = isLive
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = friendName,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = WorldlyTextPrimary
                                            )
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                StatusPill(
                                                    isLive = isLive,
                                                    label = when {
                                                        isLive -> "LIVE"
                                                        friendPausedMe -> "PAUSED"
                                                        else -> "OFFLINE"
                                                    }
                                                )
                                                if (distText != null) {
                                                    Text(
                                                        text = distText,
                                                        style = MaterialTheme.typography.labelLarge,
                                                        color = WorldlyLavender
                                                    )
                                                }
                                            }
                                        }
                                        friendLoc?.batteryLevel?.takeIf { isLive }?.let { lvl ->
                                            BatteryChip(batteryLevel = lvl, charging = friendLoc.charging)
                                        }
                                    }
                                }
                            } else {
                                Column {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(18.dp))
                                            .clickable { viewModel.openChat(friendUid) }
                                            .padding(horizontal = 6.dp, vertical = 12.dp)
                                            .testTag("chat_thread_row_$friendUid"),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        AvatarBadge(
                                            name = friendName,
                                            colorHex = friendColor,
                                            size = 50.dp,
                                            isLive = isLive
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = friendName,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = WorldlyTextPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (thread != null) {
                                                    Text(
                                                        text = GeoUtils.formatRelativeTime(thread.updatedAt),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = WorldlyTextMuted
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = thread?.lastMessageText?.ifBlank { "Tap to start chatting" }
                                                        ?: "@$friendHandle · Tap to say hello",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = WorldlyTextSecondary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                if (isLive) {
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(50))
                                                            .background(WorldlyCyan.copy(alpha = 0.25f))
                                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = "LIVE",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = WorldlyLavender
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    HorizontalDivider(
                                        color = WorldlyBorder.copy(alpha = 0.45f),
                                        modifier = Modifier.padding(start = 68.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Floating Glowing Purple Compose / Circle Action Button
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 20.dp)
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(WorldlyCyanDim, WorldlyCyan, WorldlyLavender)
                        )
                    )
                    .clickable {
                        if (acceptedFriends.isNotEmpty()) {
                            viewModel.openChat(acceptedFriends.first().otherUid(viewModel.currentUserId))
                        } else {
                            onOpenFriends()
                        }
                    }
                    .testTag("chat_compose_fab"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (acceptedFriends.isNotEmpty()) Icons.Rounded.Edit else Icons.Rounded.PersonAdd,
                    contentDescription = if (acceptedFriends.isNotEmpty()) "Open recent chat" else "Add friend",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyChatCard(
    title: String,
    body: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Surface(
        color = WorldlyObsidian.copy(alpha = 0.88f),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, WorldlyBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.ChatBubbleOutline,
                contentDescription = null,
                tint = WorldlyLavender,
                modifier = Modifier.size(38.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = WorldlyTextPrimary
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = WorldlyTextSecondary
            )
            OutlinedButton(
                onClick = onAction,
                border = BorderStroke(1.dp, WorldlyCyan.copy(alpha = 0.6f))
            ) {
                Text(actionLabel, color = WorldlyLavender)
            }
        }
    }
}

@Composable
private fun ChatThreadView(
    viewModel: WorldlyViewModel,
    friendship: Friendship,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val friendUid = friendship.otherUid(viewModel.currentUserId)
    val friendName = friendship.otherName(viewModel.currentUserId)
    val friendHandle = friendship.otherHandle(viewModel.currentUserId)
    val friendColor = friendship.otherColor(viewModel.currentUserId)

    val messagesState by viewModel.activeChatMessages.collectAsStateWithLifecycle()
    val myLocation by viewModel.mySharedLocation.collectAsStateWithLifecycle()
    val localFix by viewModel.localDeviceFix.collectAsStateWithLifecycle()
    val hasPin = (myLocation?.sharingActive == true) || localFix != null

    var draft by remember { mutableStateOf("") }
    var attachPin by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        CosmicSkyBackdrop(
            modifier = Modifier.fillMaxSize(),
            showSkyline = false,
            showClouds = false
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header
            Surface(
                color = WorldlyObsidian.copy(alpha = 0.92f),
                border = BorderStroke(1.dp, WorldlyBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("chat_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back to chats",
                            tint = WorldlyTextPrimary
                        )
                    }
                    AvatarBadge(name = friendName, colorHex = friendColor, size = 38.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = friendName,
                            style = MaterialTheme.typography.titleMedium,
                            color = WorldlyTextPrimary
                        )
                        Text(
                            text = "@$friendHandle",
                            style = MaterialTheme.typography.bodyMedium,
                            color = WorldlyTextSecondary
                        )
                    }
                }
            }

            // Messages list
            Box(modifier = Modifier.weight(1f)) {
                when (val state = messagesState) {
                    is UiState.Loading -> {
                        CircularProgressIndicator(
                            color = WorldlyCyan,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    is UiState.Error -> {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp)
                        )
                    }
                    is UiState.Success -> {
                        val messages = state.data
                        if (messages.isEmpty()) {
                            Text(
                                text = "No messages yet. Say hello or drop a pin!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = WorldlyTextSecondary,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(messages, key = { it.id }) { msg ->
                                    ChatBubble(
                                        message = msg,
                                        isMine = msg.senderUid == viewModel.currentUserId
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Composer
            Surface(
                color = WorldlyObsidian.copy(alpha = 0.94f),
                border = BorderStroke(1.dp, WorldlyBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    if (hasPin) {
                        FilterChip(
                            selected = attachPin,
                            onClick = { attachPin = !attachPin },
                            label = { Text(if (attachPin) "Current Pin Attached" else "Attach Current Pin") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.MyLocation,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = WorldlyEmerald.copy(alpha = 0.2f),
                                selectedLabelColor = WorldlyEmerald,
                                selectedLeadingIconColor = WorldlyEmerald
                            ),
                            modifier = Modifier.testTag("attach_pin_chip")
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = draft,
                            onValueChange = { draft = it.take(500) },
                            placeholder = { Text("Message $friendName...") },
                            maxLines = 3,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chat_message_input")
                        )
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    if (draft.isNotBlank()) {
                                        Brush.linearGradient(listOf(WorldlyCyanDim, WorldlyCyan))
                                    } else {
                                        Brush.linearGradient(listOf(WorldlySlate, WorldlySlate))
                                    }
                                )
                                .clickable(enabled = draft.isNotBlank()) {
                                    val textToSend = draft.trim()
                                    draft = ""
                                    val pin = attachPin
                                    attachPin = false
                                    viewModel.sendChatMessage(friendUid, textToSend, pin)
                                }
                                .testTag("send_message_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Send,
                                contentDescription = "Send message",
                                tint = if (draft.isNotBlank()) WorldlyOnAccent else WorldlyTextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessage,
    isMine: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (isMine) WorldlyCyan.copy(alpha = 0.22f) else WorldlyGlass,
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isMine) 18.dp else 4.dp,
                bottomEnd = if (isMine) 4.dp else 18.dp
            ),
            border = BorderStroke(
                1.dp,
                if (isMine) WorldlyLavender.copy(alpha = 0.55f) else WorldlyBorder
            ),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = WorldlyTextPrimary
                )
                if (message.sharedLat != null && message.sharedLng != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = WorldlyMidnight.copy(alpha = 0.72f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, WorldlyEmerald.copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Place,
                                contentDescription = null,
                                tint = WorldlyEmerald,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = String.format(
                                    Locale.US,
                                    "%s (%.4f, %.4f)",
                                    message.sharedPlaceName ?: "Shared Pin",
                                    message.sharedLat,
                                    message.sharedLng
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = WorldlyEmerald
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = GeoUtils.formatRelativeTime(message.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = WorldlyTextSecondary
                )
            }
        }
    }
}
