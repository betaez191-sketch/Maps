package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.PauseCircleOutline
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PersonRemove
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.Friendship
import com.example.domain.model.SharedLocation
import com.example.domain.model.UiState
import com.example.domain.model.UserProfile
import com.example.domain.util.GeoUtils
import com.example.ui.components.AvatarBadge
import com.example.ui.components.BatteryChip
import com.example.ui.components.CosmicSkyBackdrop
import com.example.ui.components.StatusPill
import com.example.ui.theme.WorldlyAmber
import com.example.ui.theme.WorldlyBorder
import com.example.ui.theme.WorldlyCyan
import com.example.ui.theme.WorldlyEmerald
import com.example.ui.theme.WorldlyGlass
import com.example.ui.theme.WorldlyObsidian
import com.example.ui.theme.WorldlyOnAccent
import com.example.ui.theme.WorldlyRose
import com.example.ui.theme.WorldlySlate
import com.example.ui.theme.WorldlyTextPrimary
import com.example.ui.theme.WorldlyTextSecondary
import com.example.ui.viewmodel.WorldlyViewModel

@Composable
fun FriendsScreen(
    viewModel: WorldlyViewModel,
    onOpenChat: (String) -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val clipboard = LocalClipboardManager.current
    val profileState by viewModel.userProfile.collectAsStateWithLifecycle()
    val profile = (profileState as? UiState.Success)?.data
    val friendshipsState by viewModel.friendships.collectAsStateWithLifecycle()
    val friendLocations by viewModel.friendLocations.collectAsStateWithLifecycle()
    val myLocation by viewModel.mySharedLocation.collectAsStateWithLifecycle()
    val localFix by viewModel.localDeviceFix.collectAsStateWithLifecycle()

    val searchResult by viewModel.searchResult.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val searchFeedback by viewModel.searchFeedback.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var pendingRemoveFriendship by remember { mutableStateOf<Friendship?>(null) }

    val myLat = myLocation?.takeIf { it.sharingActive }?.latitude ?: localFix?.latitude
    val myLng = myLocation?.takeIf { it.sharingActive }?.longitude ?: localFix?.longitude
    val locationMap = remember(friendLocations) { friendLocations.associateBy { it.userId } }

    Box(modifier = modifier.fillMaxSize()) {
        CosmicSkyBackdrop(
            modifier = Modifier.fillMaxSize(),
            showSkyline = false,
            showClouds = false
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (onBack != null) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("friends_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back",
                                tint = WorldlyTextPrimary
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Friends & Circle",
                            style = MaterialTheme.typography.headlineMedium,
                            color = WorldlyTextPrimary
                        )
                        Text(
                            text = "Only mutually accepted friends can see your opt-in location.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = WorldlyTextSecondary
                        )
                    }
                }
            }

        // My Shareable Friend Code Card
        if (profile != null) {
            item {
                Surface(
                    color = WorldlySlate,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, WorldlyCyan.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "YOUR FRIEND CODE",
                                style = MaterialTheme.typography.labelSmall,
                                color = WorldlyCyan
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = profile.friendCode,
                                style = MaterialTheme.typography.headlineMedium,
                                color = WorldlyTextPrimary
                            )
                            Text(
                                text = "Handle: @${profile.handle}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = WorldlyTextSecondary
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(profile.friendCode))
                                viewModel.showBanner("Copied friend code ${profile.friendCode}")
                            },
                            border = BorderStroke(1.dp, WorldlyCyan.copy(alpha = 0.6f)),
                            modifier = Modifier.testTag("copy_friend_code_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = "Copy friend code",
                                tint = WorldlyCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy", color = WorldlyCyan)
                        }
                    }
                }
            }
        }

        // Search Bar by exact @handle or Friend Code
        item {
            Surface(
                color = WorldlyObsidian,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, WorldlyBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Add a Friend",
                        style = MaterialTheme.typography.titleMedium,
                        color = WorldlyTextPrimary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                if (it.isBlank()) viewModel.clearSearch()
                            },
                            placeholder = { Text("@handle or WLD code") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = { if (searchQuery.isNotBlank()) viewModel.searchFriend(searchQuery) }
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("friend_search_input")
                        )
                        Button(
                            onClick = { viewModel.searchFriend(searchQuery) },
                            enabled = searchQuery.isNotBlank() && !isSearching,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WorldlyCyan,
                                contentColor = WorldlyOnAccent
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .height(54.dp)
                                .testTag("friend_search_button")
                        ) {
                            if (isSearching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = WorldlyOnAccent,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Search,
                                    contentDescription = "Search explorer"
                                )
                            }
                        }
                    }

                    searchResult?.let { target ->
                        Surface(
                            color = WorldlyGlass,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, WorldlyEmerald.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                AvatarBadge(
                                    name = target.displayName,
                                    colorHex = target.avatarColorHex,
                                    size = 42.dp
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = target.displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = WorldlyTextPrimary
                                    )
                                    Text(
                                        text = "@${target.handle} · ${target.friendCode}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = WorldlyTextSecondary
                                    )
                                }
                                Button(
                                    onClick = { viewModel.sendFriendRequest(target) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = WorldlyEmerald,
                                        contentColor = WorldlyOnAccent
                                    ),
                                    modifier = Modifier.testTag("send_friend_request_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PersonAdd,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add")
                                }
                            }
                        }
                    }

                    if (!searchFeedback.isNullOrBlank()) {
                        Text(
                            text = searchFeedback.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = WorldlyAmber
                        )
                    }
                }
            }
        }

        when (val state = friendshipsState) {
            is UiState.Loading -> {
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
            }
            is UiState.Error -> {
                item {
                    Surface(
                        color = WorldlyRose.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, WorldlyRose.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = WorldlyRose,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }
            is UiState.Success -> {
                val all = state.data
                val incoming = all.filter {
                    it.status == "pending" && it.recipientUid == viewModel.currentUserId
                }
                val outgoing = all.filter {
                    it.status == "pending" && it.requesterUid == viewModel.currentUserId
                }
                val accepted = all.filter { it.status == "accepted" }

                if (incoming.isNotEmpty()) {
                    item {
                        Text(
                            text = "Incoming Requests (${incoming.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = WorldlyCyan
                        )
                    }
                    items(incoming, key = { it.id }) { req ->
                        Surface(
                            color = WorldlySlate,
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, WorldlyCyan.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                AvatarBadge(
                                    name = req.requesterName,
                                    colorHex = req.requesterColor
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = req.requesterName,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = WorldlyTextPrimary
                                    )
                                    Text(
                                        text = "@${req.requesterHandle}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = WorldlyTextSecondary
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.removeFriendship(req.id) },
                                    modifier = Modifier.testTag("decline_request_${req.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Decline request",
                                        tint = WorldlyRose
                                    )
                                }
                                Button(
                                    onClick = { viewModel.acceptFriendRequest(req.id) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = WorldlyEmerald,
                                        contentColor = WorldlyOnAccent
                                    ),
                                    modifier = Modifier.testTag("accept_request_${req.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Accept")
                                }
                            }
                        }
                    }
                }

                if (outgoing.isNotEmpty()) {
                    item {
                        Text(
                            text = "Sent Requests (${outgoing.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = WorldlyTextSecondary
                        )
                    }
                    items(outgoing, key = { it.id }) { req ->
                        Surface(
                            color = WorldlyObsidian,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, WorldlyBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                AvatarBadge(
                                    name = req.recipientName,
                                    colorHex = req.recipientColor,
                                    size = 38.dp
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = req.recipientName,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = WorldlyTextPrimary
                                    )
                                    Text(
                                        text = "@${req.recipientHandle} · Pending",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = WorldlyAmber
                                    )
                                }
                                TextButton(
                                    onClick = { viewModel.removeFriendship(req.id) },
                                    modifier = Modifier.testTag("cancel_request_${req.id}")
                                ) {
                                    Text("Cancel", color = WorldlyTextSecondary)
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Accepted Circle (${accepted.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = WorldlyTextPrimary
                    )
                }

                if (accepted.isEmpty()) {
                    item {
                        EmptyFriendsCard()
                    }
                } else {
                    items(accepted, key = { it.id }) { friendship ->
                        val friendUid = friendship.otherUid(viewModel.currentUserId)
                        val friendLoc = locationMap[friendUid]
                        val iPausedFriend = viewModel.currentUserId in friendship.pausedByUids
                        val friendPausedMe = friendUid in friendship.pausedByUids

                        FriendCard(
                            myUid = viewModel.currentUserId,
                            friendship = friendship,
                            sharedLocation = friendLoc,
                            myLat = myLat,
                            myLng = myLng,
                            iPausedFriend = iPausedFriend,
                            friendPausedMe = friendPausedMe,
                            onTogglePause = { viewModel.togglePauseForFriend(friendship) },
                            onMessage = { onOpenChat(friendUid) },
                            onRemove = { pendingRemoveFriendship = friendship }
                        )
                    }
                }
            }
        }
    }
    }

    pendingRemoveFriendship?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingRemoveFriendship = null },
            containerColor = WorldlyObsidian,
            title = {
                Text(
                    text = "Remove ${target.otherName(viewModel.currentUserId)}?",
                    color = WorldlyTextPrimary
                )
            },
            text = {
                Text(
                    text = "They will immediately lose access to your live location and private chat thread.",
                    color = WorldlyTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.removeFriendship(target.id)
                        pendingRemoveFriendship = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WorldlyRose),
                    modifier = Modifier.testTag("confirm_remove_friend_button")
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemoveFriendship = null }) {
                    Text("Cancel", color = WorldlyTextSecondary)
                }
            }
        )
    }
}

@Composable
private fun FriendCard(
    myUid: String,
    friendship: Friendship,
    sharedLocation: SharedLocation?,
    myLat: Double?,
    myLng: Double?,
    iPausedFriend: Boolean,
    friendPausedMe: Boolean,
    onTogglePause: () -> Unit,
    onMessage: () -> Unit,
    onRemove: () -> Unit
) {
    val friendName = friendship.otherName(myUid)
    val friendHandle = friendship.otherHandle(myUid)
    val friendColor = friendship.otherColor(myUid)
    val isLive = sharedLocation != null && sharedLocation.sharingActive && !friendPausedMe

    val distanceText = if (isLive && myLat != null && myLng != null) {
        GeoUtils.formatDistance(
            GeoUtils.distanceMeters(myLat, myLng, sharedLocation.latitude, sharedLocation.longitude)
        )
    } else null

    Surface(
        color = WorldlySlate,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            1.dp,
            if (isLive) WorldlyEmerald.copy(alpha = 0.45f) else WorldlyBorder
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AvatarBadge(
                    name = friendName,
                    colorHex = friendColor,
                    size = 46.dp,
                    isLive = isLive
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = friendName,
                        style = MaterialTheme.typography.titleMedium,
                        color = WorldlyTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "@$friendHandle",
                        style = MaterialTheme.typography.bodyMedium,
                        color = WorldlyTextSecondary
                    )
                }
                sharedLocation?.batteryLevel?.takeIf { isLive }?.let { lvl ->
                    BatteryChip(batteryLevel = lvl, charging = sharedLocation.charging)
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusPill(
                    isLive = isLive,
                    label = when {
                        isLive -> "LIVE · ${GeoUtils.formatRelativeTime(sharedLocation.updatedAt)}"
                        friendPausedMe -> "PAUSED"
                        else -> "NOT SHARING"
                    }
                )
                if (distanceText != null) {
                    Text(
                        text = "$distanceText away",
                        style = MaterialTheme.typography.labelLarge,
                        color = WorldlyCyan
                    )
                }
            }

            if (iPausedFriend) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.VisibilityOff,
                        contentDescription = null,
                        tint = WorldlyAmber,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Your location is paused for this friend",
                        style = MaterialTheme.typography.bodyMedium,
                        color = WorldlyAmber
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onTogglePause,
                    border = BorderStroke(1.dp, WorldlyBorder),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("toggle_pause_friend_${friendship.id}")
                ) {
                    Icon(
                        imageVector = if (iPausedFriend) Icons.Rounded.PlayCircleOutline else Icons.Rounded.PauseCircleOutline,
                        contentDescription = null,
                        tint = if (iPausedFriend) WorldlyEmerald else WorldlyAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (iPausedFriend) "Resume" else "Pause",
                        color = WorldlyTextPrimary
                    )
                }

                Button(
                    onClick = onMessage,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WorldlyCyan,
                        contentColor = WorldlyOnAccent
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_friend_${friendship.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ChatBubbleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Chat")
                }

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.testTag("remove_friend_${friendship.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PersonRemove,
                        contentDescription = "Remove friend",
                        tint = WorldlyRose
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyFriendsCard() {
    Surface(
        color = WorldlyObsidian,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, WorldlyBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.GroupAdd,
                contentDescription = null,
                tint = WorldlyCyan,
                modifier = Modifier.size(36.dp)
            )
            Text(
                text = "Your Circle is Empty",
                style = MaterialTheme.typography.titleMedium,
                color = WorldlyTextPrimary
            )
            Text(
                text = "Share your Friend Code above or search an explorer by their exact @handle.",
                style = MaterialTheme.typography.bodyMedium,
                color = WorldlyTextSecondary
            )
        }
    }
}
