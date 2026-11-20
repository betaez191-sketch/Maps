package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddLocationAlt
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LocationOff
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.R
import com.example.domain.model.FavoritePlace
import com.example.domain.model.LocationHistoryEntry
import com.example.domain.model.PlaceCategory
import com.example.domain.model.SharedLocation
import com.example.domain.model.UiState
import com.example.domain.util.GeoUtils
import com.example.location.LocationHelper
import com.example.ui.components.AstroCatPeeking
import com.example.ui.components.AvatarBadge
import com.example.ui.components.BatteryChip
import com.example.ui.components.LocationRationaleDialog
import com.example.ui.components.StatusPill
import com.example.ui.components.WorldlyCrownLogo
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
import com.example.ui.theme.WorldlyPink
import com.example.ui.theme.WorldlyRose
import com.example.ui.theme.WorldlySlate
import com.example.ui.theme.WorldlyTextMuted
import com.example.ui.theme.WorldlyTextPrimary
import com.example.ui.theme.WorldlyTextSecondary
import com.example.ui.viewmodel.WorldlyViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.cos
import kotlin.math.hypot

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: WorldlyViewModel,
    onOpenChat: (String) -> Unit,
    onOpenPlaces: () -> Unit,
    onOpenFriends: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val profileState by viewModel.userProfile.collectAsStateWithLifecycle()
    val profile = (profileState as? UiState.Success)?.data
    val myLocation by viewModel.mySharedLocation.collectAsStateWithLifecycle()
    val localFix by viewModel.localDeviceFix.collectAsStateWithLifecycle()
    val friendLocations by viewModel.friendLocations.collectAsStateWithLifecycle()
    val friendshipsState by viewModel.friendships.collectAsStateWithLifecycle()
    val placesState by viewModel.favoritePlaces.collectAsStateWithLifecycle()
    val places = (placesState as? UiState.Success)?.data.orEmpty()
    val historyState by viewModel.locationHistory.collectAsStateWithLifecycle()
    val historyEntries = (historyState as? UiState.Success)?.data.orEmpty()

    val friendships = (friendshipsState as? UiState.Success)?.data.orEmpty()
    val pendingIncoming = remember(friendships, viewModel.currentUserId) {
        friendships.filter { it.status == "pending" && it.recipientUid == viewModel.currentUserId }
    }

    var selectedFriend by remember { mutableStateOf<SharedLocation?>(null) }
    var showRationaleDialog by remember { mutableStateOf(false) }
    var showSavePlaceDialog by remember { mutableStateOf(false) }
    var showNotificationsSheet by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val hasMapsApiKey = remember {
        BuildConfig.MAPS_API_KEY.isNotBlank() && BuildConfig.MAPS_API_KEY != "MY_MAPS_API_KEY"
    }

    val myLat = myLocation?.takeIf { it.sharingActive }?.latitude
        ?: localFix?.latitude
        ?: friendLocations.firstOrNull()?.latitude
        ?: places.firstOrNull()?.latitude
        ?: 47.6062
    val myLng = myLocation?.takeIf { it.sharingActive }?.longitude
        ?: localFix?.longitude
        ?: friendLocations.firstOrNull()?.longitude
        ?: places.firstOrNull()?.longitude
        ?: -122.3321

    var focusLat by remember { mutableDoubleStateOf(myLat) }
    var focusLng by remember { mutableDoubleStateOf(myLng) }

    LaunchedEffect(myLat, myLng) {
        if (selectedFriend == null && searchQuery.isBlank()) {
            focusLat = myLat
            focusLng = myLng
        }
    }

    val hasMyFix = (myLocation?.sharingActive == true) || localFix != null
    val sharingEnabled = profile?.sharingEnabled == true

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = (results[Manifest.permission.ACCESS_FINE_LOCATION] == true) ||
            (results[Manifest.permission.ACCESS_COARSE_LOCATION] == true)
        if (granted) {
            viewModel.setSharingEnabled(context, true)
        } else {
            viewModel.showBanner(context.getString(R.string.location_denied_msg))
        }
    }

    fun requestPermissionsAndShare() {
        val perms = buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
        permissionLauncher.launch(perms)
    }

    LaunchedEffect(profile?.sharingEnabled) {
        if (profile?.sharingEnabled == true && LocationHelper.hasAnyLocationPermission(context)) {
            LocationHelper.startSharingService(context)
            viewModel.refreshLocalFixIfPermitted(context)
        }
    }

    var radarZoom by remember { mutableFloatStateOf(1f) }
    var radarPanX by remember { mutableFloatStateOf(0f) }
    var radarPanY by remember { mutableFloatStateOf(0f) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(myLat, myLng), 13.5f)
    }

    val searchMatches = remember(searchQuery, friendLocations, places) {
        val q = searchQuery.trim().lowercase(Locale.US)
        if (q.isBlank()) emptyList()
        else {
            val friendResults = friendLocations.filter {
                it.displayName.lowercase(Locale.US).contains(q) ||
                    it.handle.lowercase(Locale.US).contains(q)
            }.map { SearchTarget.FriendTarget(it) }
            val placeResults = places.filter {
                it.name.lowercase(Locale.US).contains(q) ||
                    it.addressHint.lowercase(Locale.US).contains(q)
            }.map { SearchTarget.PlaceTarget(it) }
            (friendResults + placeResults).take(5)
        }
    }

    val nearestPlaceName = remember(myLat, myLng, places, hasMyFix) {
        if (!hasMyFix || places.isEmpty()) null
        else places.minByOrNull { GeoUtils.distanceMeters(myLat, myLng, it.latitude, it.longitude) }
            ?.takeIf { GeoUtils.distanceMeters(myLat, myLng, it.latitude, it.longitude) <= 500.0 }
            ?.name
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (hasMapsApiKey) {
            GoogleMapsLiveLayer(
                myLat = myLat,
                myLng = myLng,
                hasMyFix = hasMyFix,
                sharingEnabled = sharingEnabled,
                friendLocations = friendLocations,
                places = places,
                historyEntries = if (profile?.historyEnabled == true) historyEntries else emptyList(),
                cameraPositionState = cameraPositionState,
                onFriendClick = { selectedFriend = it }
            )
        } else {
            CosmicRadarCanvas(
                centerLat = focusLat,
                centerLng = focusLng,
                myLat = myLat,
                myLng = myLng,
                hasMyFix = hasMyFix,
                sharingEnabled = sharingEnabled,
                friendLocations = friendLocations,
                places = places,
                historyEntries = if (profile?.historyEnabled == true) historyEntries else emptyList(),
                selectedFriendId = selectedFriend?.userId,
                zoom = radarZoom,
                panX = radarPanX,
                panY = radarPanY,
                onTransform = { zoomChange, panChange ->
                    radarZoom = (radarZoom * zoomChange).coerceIn(0.45f, 5.0f)
                    radarPanX += panChange.x
                    radarPanY += panChange.y
                },
                onFriendSelected = { selectedFriend = it }
            )
        }

        // Top Bar: WORLDLY Crown Logo + Notification Bell + Search Pill
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                WorldlyCrownLogo(fontSize = 23.sp)

                Surface(
                    color = WorldlyObsidian.copy(alpha = 0.86f),
                    shape = CircleShape,
                    border = BorderStroke(
                        1.dp,
                        if (pendingIncoming.isNotEmpty()) WorldlyPink else WorldlyBorder
                    ),
                    shadowElevation = 8.dp
                ) {
                    IconButton(
                        onClick = { showNotificationsSheet = true },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("home_notifications_button")
                    ) {
                        if (pendingIncoming.isNotEmpty()) {
                            BadgedBox(
                                badge = {
                                    Badge(containerColor = WorldlyRose) {
                                        Text("${pendingIncoming.size}")
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Notifications,
                                    contentDescription = "Alerts and friend requests",
                                    tint = WorldlyTextPrimary
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Notifications,
                                contentDescription = "Alerts and friend requests",
                                tint = WorldlyTextPrimary
                            )
                        }
                    }
                }
            }

            // Glassy Search Bar
            Surface(
                color = WorldlyObsidian.copy(alpha = 0.88f),
                shape = RoundedCornerShape(26.dp),
                border = BorderStroke(1.dp, WorldlyBorder),
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = WorldlyLavender,
                        modifier = Modifier.size(20.dp)
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search friend or saved place...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = WorldlyTextMuted
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it.take(40) },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = WorldlyTextPrimary
                            ),
                            cursorBrush = SolidColor(WorldlyCyan),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("home_search_input")
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Clear search",
                            tint = WorldlyTextSecondary,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable { searchQuery = "" }
                        )
                    }
                }
            }

            // Live Search Results Dropdown
            AnimatedVisibility(visible = searchMatches.isNotEmpty()) {
                Surface(
                    color = WorldlyObsidian.copy(alpha = 0.96f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, WorldlyCyan.copy(alpha = 0.45f)),
                    shadowElevation = 10.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        searchMatches.forEach { target ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        when (target) {
                                            is SearchTarget.FriendTarget -> {
                                                selectedFriend = target.friend
                                                focusLat = target.friend.latitude
                                                focusLng = target.friend.longitude
                                                radarPanX = 0f
                                                radarPanY = 0f
                                                if (hasMapsApiKey) {
                                                    scope.launch {
                                                        cameraPositionState.animate(
                                                            CameraUpdateFactory.newLatLngZoom(
                                                                LatLng(target.friend.latitude, target.friend.longitude),
                                                                15f
                                                            )
                                                        )
                                                    }
                                                }
                                            }
                                            is SearchTarget.PlaceTarget -> {
                                                selectedFriend = null
                                                focusLat = target.place.latitude
                                                focusLng = target.place.longitude
                                                radarPanX = 0f
                                                radarPanY = 0f
                                                if (hasMapsApiKey) {
                                                    scope.launch {
                                                        cameraPositionState.animate(
                                                            CameraUpdateFactory.newLatLngZoom(
                                                                LatLng(target.place.latitude, target.place.longitude),
                                                                15f
                                                            )
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        searchQuery = ""
                                    }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = when (target) {
                                        is SearchTarget.FriendTarget -> Icons.Rounded.MyLocation
                                        is SearchTarget.PlaceTarget -> Icons.Rounded.Place
                                    },
                                    contentDescription = null,
                                    tint = when (target) {
                                        is SearchTarget.FriendTarget -> WorldlyCyan
                                        is SearchTarget.PlaceTarget -> WorldlyAmber
                                    },
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = when (target) {
                                        is SearchTarget.FriendTarget -> "${target.friend.displayName} (@${target.friend.handle})"
                                        is SearchTarget.PlaceTarget -> target.place.name
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = WorldlyTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // Right-side Floating Circular Controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = 16.dp,
                    bottom = if (selectedFriend != null) 286.dp else 184.dp
                ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.End
        ) {
            CosmicMapCircleButton(
                icon = Icons.Rounded.MyLocation,
                contentDescription = "Re-center map on current location",
                accent = WorldlyLavender,
                testTag = "recenter_map_button",
                onClick = {
                    focusLat = myLat
                    focusLng = myLng
                    selectedFriend = null
                    if (hasMapsApiKey) {
                        scope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(LatLng(myLat, myLng), 14f)
                            )
                        }
                    } else {
                        radarPanX = 0f
                        radarPanY = 0f
                        radarZoom = 1f
                    }
                    viewModel.refreshLocalFixIfPermitted(context)
                }
            )

            CosmicMapCircleButton(
                icon = Icons.Rounded.Map,
                contentDescription = "Open Favorite Places and Location History",
                accent = WorldlyTextPrimary,
                testTag = "open_places_layer_button",
                onClick = onOpenPlaces
            )

            CosmicMapCircleButton(
                icon = Icons.Rounded.AddLocationAlt,
                contentDescription = "Save current spot to Favorite Places",
                accent = WorldlyAmber,
                testTag = "save_current_pin_button",
                onClick = { showSavePlaceDialog = true }
            )
        }

        // Bottom HUD: Friend Chips + Selected Friend Detail + Peeking Astro-Cat Telemetry Card
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AnimatedVisibility(visible = selectedFriend != null) {
                selectedFriend?.let { friend ->
                    FriendRadarDetailCard(
                        friend = friend,
                        myLat = myLat,
                        myLng = myLng,
                        hasMyFix = hasMyFix,
                        onDismiss = { selectedFriend = null },
                        onMessageClick = { onOpenChat(friend.userId) }
                    )
                }
            }

            if (friendLocations.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(friendLocations, key = { it.userId }) { friend ->
                        val distLabel = if (hasMyFix) {
                            GeoUtils.formatDistance(
                                GeoUtils.distanceMeters(myLat, myLng, friend.latitude, friend.longitude)
                            )
                        } else {
                            GeoUtils.formatRelativeTime(friend.updatedAt)
                        }
                        val isSelected = selectedFriend?.userId == friend.userId
                        Surface(
                            color = if (isSelected) WorldlyGlass else WorldlyObsidian.copy(alpha = 0.90f),
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) WorldlyCyan else WorldlyBorder
                            ),
                            modifier = Modifier
                                .clickable {
                                    selectedFriend = friend
                                    focusLat = friend.latitude
                                    focusLng = friend.longitude
                                    if (hasMapsApiKey) {
                                        scope.launch {
                                            cameraPositionState.animate(
                                                CameraUpdateFactory.newLatLngZoom(
                                                    LatLng(friend.latitude, friend.longitude),
                                                    15f
                                                )
                                            )
                                        }
                                    } else {
                                        radarPanX = 0f
                                        radarPanY = 0f
                                    }
                                }
                                .testTag("radar_friend_chip_${friend.userId}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AvatarBadge(
                                    name = friend.displayName,
                                    colorHex = friend.avatarColorHex,
                                    size = 26.dp,
                                    isLive = true
                                )
                                Column {
                                    Text(
                                        text = friend.displayName,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = WorldlyTextPrimary,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = distLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = WorldlyLavender
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom-Left Peeking Astro-Cat + Dark Glass Telemetry Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Surface(
                    color = WorldlyObsidian.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(
                        1.dp,
                        if (sharingEnabled) WorldlyEmerald.copy(alpha = 0.6f) else WorldlyCyan.copy(alpha = 0.45f)
                    ),
                    shadowElevation = 12.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 42.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StatusPill(
                                    isLive = sharingEnabled,
                                    label = if (sharingEnabled) "SHARING LIVE" else "GHOST MODE"
                                )
                                val batt = myLocation?.batteryLevel
                                if (sharingEnabled && batt != null) {
                                    BatteryChip(
                                        batteryLevel = batt,
                                        charging = myLocation?.charging
                                    )
                                }
                            }
                            Text(
                                text = when {
                                    nearestPlaceName != null -> "Near $nearestPlaceName"
                                    hasMyFix -> String.format(Locale.US, "%.4f, %.4f", myLat, myLng)
                                    else -> "GPS paused · Tap pill to go live"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = WorldlyTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = buildString {
                                    append("${friendLocations.size} friend${if (friendLocations.size == 1) "" else "s"} live")
                                    if (!hasMapsApiKey) append(" · Cosmic Radar")
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = WorldlyTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                color = if (sharingEnabled) WorldlyEmerald else WorldlyCyan,
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .clickable {
                                        if (sharingEnabled) {
                                            viewModel.setSharingEnabled(context, false)
                                        } else if (LocationHelper.hasAnyLocationPermission(context)) {
                                            viewModel.setSharingEnabled(context, true)
                                        } else {
                                            showRationaleDialog = true
                                        }
                                    }
                                    .testTag("master_sharing_toggle")
                            ) {
                                Text(
                                    text = if (sharingEnabled) "Pause" else "Go Live",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (sharingEnabled) WorldlyMidnight else Color.White,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                            val updatedText = myLocation?.updatedAt?.let {
                                "Updated ${GeoUtils.formatRelativeTime(it)}"
                            } ?: if (localFix != null) "Fix ready" else "Standby"
                            Text(
                                text = updatedText,
                                style = MaterialTheme.typography.labelSmall,
                                color = WorldlyTextMuted
                            )
                        }
                    }
                }

                AstroCatPeeking(
                    size = 78.dp,
                    wink = sharingEnabled,
                    showHeartBubble = false,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 12.dp)
                        .offset(y = (-8).dp)
                )
            }
        }
    }

    if (showNotificationsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showNotificationsSheet = false },
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
                    text = "Circle Alerts & Requests",
                    style = MaterialTheme.typography.titleLarge,
                    color = WorldlyTextPrimary
                )
                if (pendingIncoming.isEmpty()) {
                    Text(
                        text = "No pending friend requests right now.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = WorldlyTextSecondary
                    )
                } else {
                    pendingIncoming.forEach { req ->
                        Surface(
                            color = WorldlySlate,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, WorldlyCyan.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                AvatarBadge(name = req.requesterName, colorHex = req.requesterColor)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(req.requesterName, color = WorldlyTextPrimary)
                                    Text(
                                        "@${req.requesterHandle}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = WorldlyTextSecondary
                                    )
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
                OutlinedButton(
                    onClick = {
                        showNotificationsSheet = false
                        onOpenFriends()
                    },
                    border = BorderStroke(1.dp, WorldlyCyan.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open Friends & Circle", color = WorldlyLavender)
                }
                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }

    if (showRationaleDialog) {
        LocationRationaleDialog(
            onConfirm = {
                showRationaleDialog = false
                requestPermissionsAndShare()
            },
            onDismiss = { showRationaleDialog = false }
        )
    }

    if (showSavePlaceDialog) {
        SavePlaceQuickDialog(
            latitude = myLat,
            longitude = myLng,
            onSave = { name, category, hint ->
                viewModel.saveFavoritePlace(name, category, myLat, myLng, hint) {
                    showSavePlaceDialog = false
                }
            },
            onDismiss = { showSavePlaceDialog = false }
        )
    }
}

private sealed interface SearchTarget {
    data class FriendTarget(val friend: SharedLocation) : SearchTarget
    data class PlaceTarget(val place: FavoritePlace) : SearchTarget
}

@Composable
private fun CosmicMapCircleButton(
    icon: ImageVector,
    contentDescription: String,
    accent: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        color = WorldlyObsidian.copy(alpha = 0.90f),
        shape = CircleShape,
        border = BorderStroke(1.2.dp, WorldlyBorder),
        shadowElevation = 10.dp,
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = accent,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun GoogleMapsLiveLayer(
    myLat: Double,
    myLng: Double,
    hasMyFix: Boolean,
    sharingEnabled: Boolean,
    friendLocations: List<SharedLocation>,
    places: List<FavoritePlace>,
    historyEntries: List<LocationHistoryEntry>,
    cameraPositionState: CameraPositionState,
    onFriendClick: (SharedLocation) -> Unit
) {
    val darkMapProperties = remember(sharingEnabled, hasMyFix) {
        MapProperties(
            isMyLocationEnabled = false,
            mapStyleOptions = MapStyleOptions(WORLDLY_DARK_MAP_STYLE_JSON)
        )
    }
    val uiSettings = remember {
        MapUiSettings(
            zoomControlsEnabled = false,
            compassEnabled = true,
            myLocationButtonEnabled = false
        )
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = darkMapProperties,
        uiSettings = uiSettings
    ) {
        if (hasMyFix) {
            Marker(
                state = MarkerState(position = LatLng(myLat, myLng)),
                title = if (sharingEnabled) "You (Live)" else "You (Private)",
                icon = BitmapDescriptorFactory.defaultMarker(
                    if (sharingEnabled) BitmapDescriptorFactory.HUE_VIOLET else BitmapDescriptorFactory.HUE_AZURE
                )
            )
        }

        friendLocations.forEach { friend ->
            Marker(
                state = MarkerState(position = LatLng(friend.latitude, friend.longitude)),
                title = friend.displayName,
                snippet = "@${friend.handle} · ${GeoUtils.formatRelativeTime(friend.updatedAt)}",
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA),
                onClick = {
                    onFriendClick(friend)
                    false
                }
            )
        }

        places.forEach { place ->
            Marker(
                state = MarkerState(position = LatLng(place.latitude, place.longitude)),
                title = place.name,
                snippet = PlaceCategory.fromWire(place.category).label,
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
            )
        }

        if (historyEntries.size >= 2) {
            Polyline(
                points = historyEntries.map { LatLng(it.latitude, it.longitude) },
                color = WorldlyCyan.copy(alpha = 0.8f),
                width = 6f
            )
        }
    }
}

@Composable
private fun CosmicRadarCanvas(
    centerLat: Double,
    centerLng: Double,
    myLat: Double,
    myLng: Double,
    hasMyFix: Boolean,
    sharingEnabled: Boolean,
    friendLocations: List<SharedLocation>,
    places: List<FavoritePlace>,
    historyEntries: List<LocationHistoryEntry>,
    selectedFriendId: String?,
    zoom: Float,
    panX: Float,
    panY: Float,
    onTransform: (Float, Offset) -> Unit,
    onFriendSelected: (SharedLocation?) -> Unit
) {
    val textMeasurer: TextMeasurer = rememberTextMeasurer()
    val pulseTransition = rememberInfiniteTransition(label = "cosmicBeacon")
    val pulseRadius by pulseTransition.animateFloat(
        initialValue = 22f,
        targetValue = 74f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Restart),
        label = "beaconRadius"
    )
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.62f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Restart),
        label = "beaconAlpha"
    )

    val cosLat = cos(Math.toRadians(centerLat)).coerceAtLeast(0.2)
    val baseScalePxPerDeg = 12_000f * zoom

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF1E0F42), Color(0xFF0E0720), WorldlyMidnight)
                )
            )
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoomChange, _ ->
                    onTransform(zoomChange, pan)
                }
            }
            .pointerInput(friendLocations, centerLat, centerLng, zoom, panX, panY) {
                detectTapGestures { tapOffset ->
                    val w = size.width.toFloat()
                    val h = size.height.toFloat()
                    val origin = Offset(w / 2f + panX, h / 2f + panY)
                    val hit = friendLocations.firstOrNull { friend ->
                        val dx = ((friend.longitude - centerLng) * cosLat * baseScalePxPerDeg).toFloat()
                        val dy = (-(friend.latitude - centerLat) * baseScalePxPerDeg).toFloat()
                        val pos = Offset(origin.x + dx, origin.y + dy)
                        hypot(tapOffset.x - pos.x, tapOffset.y - pos.y) <= 58f
                    }
                    onFriendSelected(hit)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val origin = Offset(size.width / 2f + panX, size.height / 2f + panY)

            fun project(lat: Double, lng: Double): Offset {
                val dx = ((lng - centerLng) * cosLat * baseScalePxPerDeg).toFloat()
                val dy = (-(lat - centerLat) * baseScalePxPerDeg).toFloat()
                return Offset(origin.x + dx, origin.y + dy)
            }

            // Cosmic purple street-like grid & arterial veins
            val gridStep = 140f
            var gx = origin.x % gridStep
            while (gx < size.width) {
                drawLine(Color(0xFF331D63).copy(alpha = 0.42f), Offset(gx, 0f), Offset(gx, size.height), 2f)
                gx += gridStep
            }
            var gy = origin.y % gridStep
            while (gy < size.height) {
                drawLine(Color(0xFF331D63).copy(alpha = 0.42f), Offset(0f, gy), Offset(size.width, gy), 2f)
                gy += gridStep
            }
            drawLine(
                color = WorldlyCyanDim.copy(alpha = 0.35f),
                start = Offset(0f, size.height * 0.25f),
                end = Offset(size.width, size.height * 0.72f),
                strokeWidth = 6f
            )
            drawLine(
                color = WorldlyCyanDim.copy(alpha = 0.28f),
                start = Offset(size.width * 0.78f, 0f),
                end = Offset(size.width * 0.22f, size.height),
                strokeWidth = 5f
            )

            // Concentric cosmic violet rings
            listOf(115f, 225f, 345f).forEach { r ->
                drawCircle(
                    color = WorldlyCyan.copy(alpha = 0.22f),
                    radius = r * zoom.coerceIn(0.6f, 2.2f),
                    center = origin,
                    style = Stroke(
                        width = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f))
                    )
                )
            }

            // Opt-in Location History trail
            if (historyEntries.size >= 2) {
                val path = Path()
                historyEntries.forEachIndexed { idx, entry ->
                    val pt = project(entry.latitude, entry.longitude)
                    if (idx == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
                }
                drawPath(
                    path = path,
                    color = WorldlyLavender.copy(alpha = 0.7f),
                    style = Stroke(width = 4.5f)
                )
            }

            // Favorite Places pins
            places.forEach { place ->
                val pt = project(place.latitude, place.longitude)
                drawCircle(WorldlyAmber.copy(alpha = 0.24f), radius = 24f, center = pt)
                drawCircle(WorldlyAmber, radius = 9.5f, center = pt)
                drawText(
                    textMeasurer = textMeasurer,
                    text = place.name,
                    topLeft = Offset(pt.x + 15f, pt.y - 10f),
                    style = TextStyle(color = WorldlyAmber, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                )
            }

            // My Cosmic Beacon
            val myPos = project(myLat, myLng)
            val myColor = if (sharingEnabled) WorldlyEmerald else WorldlyCyan
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(WorldlyCyan.copy(alpha = 0.42f), Color.Transparent),
                    center = myPos,
                    radius = 90f
                ),
                radius = 90f,
                center = myPos
            )
            if (hasMyFix) {
                drawCircle(
                    color = myColor.copy(alpha = pulseAlpha),
                    radius = pulseRadius,
                    center = myPos
                )
                drawCircle(Color.White, radius = 17f, center = myPos)
                drawCircle(myColor, radius = 12f, center = myPos)
            } else {
                drawCircle(WorldlyLavender.copy(alpha = 0.45f), radius = 14f, center = myPos)
            }

            // Kawaii Cat-Ear Friend Beacons
            friendLocations.forEach { friend ->
                val pt = project(friend.latitude, friend.longitude)
                val accent = GeoUtils.parseHexColor(friend.avatarColorHex, WorldlyPink)
                val isSelected = friend.userId == selectedFriendId
                val r = if (isSelected) 22f else 17f

                drawCircle(
                    color = accent.copy(alpha = if (isSelected) 0.4f else 0.22f),
                    radius = r * 1.9f,
                    center = pt
                )
                // Little cat ears on top of the friend pin
                val leftEar = Path().apply {
                    moveTo(pt.x - r * 0.85f, pt.y - r * 0.35f)
                    lineTo(pt.x - r * 0.95f, pt.y - r * 1.30f)
                    lineTo(pt.x - r * 0.15f, pt.y - r * 0.85f)
                    close()
                }
                val rightEar = Path().apply {
                    moveTo(pt.x + r * 0.85f, pt.y - r * 0.35f)
                    lineTo(pt.x + r * 0.95f, pt.y - r * 1.30f)
                    lineTo(pt.x + r * 0.15f, pt.y - r * 0.85f)
                    close()
                }
                drawPath(leftEar, color = WorldlyLavender)
                drawPath(rightEar, color = WorldlyLavender)
                drawCircle(WorldlyObsidian, radius = r + 3f, center = pt)
                drawCircle(accent, radius = r, center = pt)
                drawText(
                    textMeasurer = textMeasurer,
                    text = friend.displayName,
                    topLeft = Offset(pt.x + r + 8f, pt.y - 10f),
                    style = TextStyle(
                        color = WorldlyTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}

@Composable
private fun FriendRadarDetailCard(
    friend: SharedLocation,
    myLat: Double,
    myLng: Double,
    hasMyFix: Boolean,
    onDismiss: () -> Unit,
    onMessageClick: () -> Unit
) {
    val distanceText = if (hasMyFix) {
        GeoUtils.formatDistance(GeoUtils.distanceMeters(myLat, myLng, friend.latitude, friend.longitude))
    } else {
        "Distance hidden (your sharing is off)"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        color = WorldlyObsidian.copy(alpha = 0.96f),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, WorldlyCyan.copy(alpha = 0.55f)),
        shadowElevation = 12.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AvatarBadge(
                    name = friend.displayName,
                    colorHex = friend.avatarColorHex,
                    size = 46.dp,
                    isLive = true
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = friend.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = WorldlyTextPrimary
                    )
                    Text(
                        text = "@${friend.handle} · ${GeoUtils.formatRelativeTime(friend.updatedAt)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = WorldlyTextSecondary
                    )
                }
                friend.batteryLevel?.let { lvl ->
                    BatteryChip(batteryLevel = lvl, charging = friend.charging)
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close friend detail",
                        tint = WorldlyTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (hasMyFix) Icons.Rounded.Place else Icons.Rounded.LocationOff,
                        contentDescription = null,
                        tint = WorldlyLavender,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = distanceText,
                        style = MaterialTheme.typography.labelLarge,
                        color = WorldlyLavender
                    )
                }

                Button(
                    onClick = onMessageClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WorldlyCyan,
                        contentColor = WorldlyOnAccent
                    ),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.testTag("radar_message_friend_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ChatBubbleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Message")
                }
            }
        }
    }
}

@Composable
private fun SavePlaceQuickDialog(
    latitude: Double,
    longitude: Double,
    onSave: (String, PlaceCategory, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var hint by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(PlaceCategory.HANGOUT) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = WorldlyObsidian,
        title = {
            Text("Save Favorite Place", color = WorldlyTextPrimary)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = String.format(Locale.US, "Pin: %.4f, %.4f", latitude, longitude),
                    style = MaterialTheme.typography.labelSmall,
                    color = WorldlyLavender
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(50) },
                    label = { Text("Place Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("place_name_input")
                )
                OutlinedTextField(
                    value = hint,
                    onValueChange = { hint = it.take(100) },
                    label = { Text("Neighborhood or Note (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PlaceCategory.entries.forEach { cat ->
                        val active = cat == selectedCategory
                        OutlinedButton(
                            onClick = { selectedCategory = cat },
                            border = BorderStroke(
                                1.dp,
                                if (active) WorldlyCyan else WorldlyBorder
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = cat.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (active) WorldlyLavender else WorldlyTextSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onSave(name, selectedCategory, hint) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WorldlyCyan,
                    contentColor = WorldlyOnAccent
                ),
                modifier = Modifier.testTag("confirm_save_place_button")
            ) {
                Text("Save Place")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = WorldlyTextSecondary)
            }
        }
    )
}

private const val WORLDLY_DARK_MAP_STYLE_JSON = """
[
  {"elementType":"geometry","stylers":[{"color":"#120929"}]},
  {"elementType":"labels.text.fill","stylers":[{"color":"#c4b5fd"}]},
  {"elementType":"labels.text.stroke","stylers":[{"color":"#070512"}]},
  {"featureType":"road","elementType":"geometry","stylers":[{"color":"#281652"}]},
  {"featureType":"road.highway","elementType":"geometry","stylers":[{"color":"#422387"}]},
  {"featureType":"poi","elementType":"geometry","stylers":[{"color":"#1a0e38"}]},
  {"featureType":"water","elementType":"geometry","stylers":[{"color":"#070412"}]}
]
"""
