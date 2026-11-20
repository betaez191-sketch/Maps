package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.auth.AuthRepository
import com.example.domain.model.UiState
import com.example.domain.model.UserProfile
import com.example.domain.util.GeoUtils
import com.example.location.LocationHelper
import com.example.location.LocationSharingService
import com.example.ui.components.AstroCatPeeking
import com.example.ui.components.CosmicSkyBackdrop
import com.example.ui.components.LocationRationaleDialog
import com.example.ui.components.StatusPill
import com.example.ui.theme.WorldlyAmber
import com.example.ui.theme.WorldlyBorder
import com.example.ui.theme.WorldlyCyan
import com.example.ui.theme.WorldlyCyanDim
import com.example.ui.theme.WorldlyEmerald
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: WorldlyViewModel,
    onOpenPlaces: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    val profileState by viewModel.userProfile.collectAsStateWithLifecycle()
    val profile = (profileState as? UiState.Success)?.data
    val friendshipsState by viewModel.friendships.collectAsStateWithLifecycle()
    val friendLocations by viewModel.friendLocations.collectAsStateWithLifecycle()
    val placesState by viewModel.favoritePlaces.collectAsStateWithLifecycle()

    val acceptedFriendsCount = remember(friendshipsState) {
        (friendshipsState as? UiState.Success)?.data?.count { it.status == "accepted" } ?: 0
    }
    val liveFriendsCount = friendLocations.size
    val savedPlacesCount = remember(placesState) {
        (placesState as? UiState.Success)?.data?.size ?: 0
    }

    val isServiceRunning by LocationSharingService.isServiceRunning.collectAsStateWithLifecycle()
    val isDeletingAccount by viewModel.isDeletingAccount.collectAsStateWithLifecycle()
    val requiresRecentLogin by viewModel.requiresRecentLoginForDelete.collectAsStateWithLifecycle()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showPrivacySheet by remember { mutableStateOf(false) }
    var showPermissionsSheet by remember { mutableStateOf(false) }
    var showRationaleDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }

    val hasLocationPerm = LocationHelper.hasAnyLocationPermission(context)
    val hasFinePerm = LocationHelper.hasFineLocationPermission(context)
    val hasNotifPerm = LocationHelper.hasNotificationPermission(context)

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = (results[Manifest.permission.ACCESS_FINE_LOCATION] == true) ||
            (results[Manifest.permission.ACCESS_COARSE_LOCATION] == true)
        if (granted) {
            viewModel.setSharingEnabled(context, true)
        } else {
            viewModel.showBanner("Location permission denied. Sharing remains off.")
        }
    }

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
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top bar with quick permissions gear
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Profile",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = WorldlyTextPrimary
                    )
                    IconButton(
                        onClick = { showPermissionsSheet = true },
                        modifier = Modifier.testTag("profile_permissions_gear_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Android Permissions and Settings",
                            tint = WorldlyTextPrimary
                        )
                    }
                }
            }

            if (profile != null) {
                val accentColor = GeoUtils.parseHexColor(profile.avatarColorHex, WorldlyCyan)

                // Hero Cosmic Avatar Ring + Display Name + Gold Crown + ID
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier.size(106.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(accentColor.copy(alpha = 0.35f), WorldlySlate)
                                        )
                                    )
                                    .border(2.5.dp, WorldlyLavender, CircleShape)
                                    .clickable { showEditProfileDialog = true },
                                contentAlignment = Alignment.Center
                            ) {
                                AstroCatPeeking(
                                    size = 76.dp,
                                    wink = profile.sharingEnabled,
                                    showHeartBubble = false
                                )
                            }
                            Surface(
                                color = WorldlyCyan,
                                shape = CircleShape,
                                border = BorderStroke(2.dp, WorldlyMidnight),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(32.dp)
                                    .clickable { showEditProfileDialog = true }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.CameraAlt,
                                        contentDescription = "Edit avatar and beacon color",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = profile.displayName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = WorldlyTextPrimary
                            )
                            Canvas(modifier = Modifier.size(20.dp)) {
                                val w = size.width
                                val h = size.height
                                val crown = Path().apply {
                                    moveTo(w * 0.1f, h * 0.85f)
                                    lineTo(w * 0.05f, h * 0.25f)
                                    lineTo(w * 0.34f, h * 0.56f)
                                    lineTo(w * 0.50f, h * 0.15f)
                                    lineTo(w * 0.66f, h * 0.56f)
                                    lineTo(w * 0.95f, h * 0.25f)
                                    lineTo(w * 0.9f, h * 0.85f)
                                    close()
                                }
                                drawPath(crown, color = WorldlyAmber)
                                drawPath(crown, color = Color.White, style = Stroke(width = 1.4f))
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable {
                                    clipboard.setText(AnnotatedString(profile.friendCode))
                                    viewModel.showBanner("Copied ID ${profile.friendCode}")
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "ID: ${profile.friendCode} · @${profile.handle}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = WorldlyTextSecondary
                            )
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = "Copy friend code",
                                tint = WorldlyLavender,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                // 3-Column Real Stats Pill Card
                item {
                    Surface(
                        color = WorldlyObsidian.copy(alpha = 0.88f),
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(1.dp, WorldlyBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ProfileStatCell(value = "$acceptedFriendsCount", label = "Friends")
                            ProfileStatCell(value = "$liveFriendsCount", label = "Live Now")
                            ProfileStatCell(value = "$savedPlacesCount", label = "Places")
                        }
                    }
                }

                // "Your Status" Interactive Card
                item {
                    Surface(
                        color = WorldlyObsidian.copy(alpha = 0.90f),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(
                            1.dp,
                            if (profile.sharingEnabled) WorldlyEmerald.copy(alpha = 0.55f) else WorldlyBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showPrivacySheet = true }
                            .testTag("profile_status_card")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (profile.sharingEnabled) WorldlyEmerald.copy(alpha = 0.16f)
                                        else WorldlyCyan.copy(alpha = 0.16f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Radar,
                                    contentDescription = null,
                                    tint = if (profile.sharingEnabled) WorldlyEmerald else WorldlyLavender
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Your Status",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = WorldlyTextPrimary
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (profile.sharingEnabled) WorldlyEmerald else WorldlyTextMuted)
                                    )
                                    Text(
                                        text = if (profile.sharingEnabled) {
                                            "Online · ${profile.statusMessage.ifBlank { "Sharing live GPS" }}"
                                        } else {
                                            "Ghost Mode · ${profile.statusMessage.ifBlank { "Location hidden" }}"
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (profile.sharingEnabled) WorldlyEmerald else WorldlyTextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = null,
                                tint = WorldlyTextSecondary
                            )
                        }
                    }
                }
            }

            // Cosmic Menu List
            item {
                Surface(
                    color = WorldlyObsidian.copy(alpha = 0.88f),
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, WorldlyBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        ProfileMenuRow(
                            icon = Icons.Rounded.Edit,
                            title = "Edit Profile",
                            tint = WorldlyLavender,
                            testTag = "edit_profile_button",
                            onClick = { showEditProfileDialog = true }
                        )
                        HorizontalDivider(color = WorldlyBorder.copy(alpha = 0.45f))
                        ProfileMenuRow(
                            icon = Icons.Rounded.Shield,
                            title = "Privacy & Security",
                            tint = WorldlyEmerald,
                            testTag = "privacy_security_menu_button",
                            onClick = { showPrivacySheet = true }
                        )
                        HorizontalDivider(color = WorldlyBorder.copy(alpha = 0.45f))
                        ProfileMenuRow(
                            icon = Icons.Rounded.Notifications,
                            title = "Notifications & Permissions",
                            tint = WorldlyAmber,
                            testTag = "notifications_permissions_menu_button",
                            onClick = { showPermissionsSheet = true }
                        )
                        HorizontalDivider(color = WorldlyBorder.copy(alpha = 0.45f))
                        ProfileMenuRow(
                            icon = Icons.Rounded.Place,
                            title = "Favorite Places",
                            tint = WorldlyCyan,
                            testTag = "favorite_places_menu_button",
                            onClick = { onOpenPlaces(0) }
                        )
                        HorizontalDivider(color = WorldlyBorder.copy(alpha = 0.45f))
                        ProfileMenuRow(
                            icon = Icons.Rounded.History,
                            title = "Location History",
                            tint = WorldlyLavender,
                            testTag = "location_history_menu_button",
                            onClick = { onOpenPlaces(1) }
                        )
                    }
                }
            }

            // Account Sign-Out & Delete Account
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = {
                            LocationHelper.stopSharingService(context)
                            scope.launch { AuthRepository.signOut(context) }
                        },
                        border = BorderStroke(1.dp, WorldlyBorder),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Logout,
                            contentDescription = null,
                            tint = WorldlyTextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign Out", color = WorldlyTextPrimary)
                    }

                    Button(
                        onClick = { showDeleteAccountDialog = true },
                        enabled = !isDeletingAccount,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WorldlyRose.copy(alpha = 0.16f),
                            contentColor = WorldlyRose
                        ),
                        border = BorderStroke(1.dp, WorldlyRose.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("delete_account_button")
                    ) {
                        if (isDeletingAccount) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = WorldlyRose,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Deleting Account...")
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.DeleteForever,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Delete Account & All Data")
                        }
                    }
                }
            }
        }
    }

    if (showPrivacySheet && profile != null) {
        ModalBottomSheet(
            onDismissRequest = { showPrivacySheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = WorldlyObsidian,
            contentColor = WorldlyTextPrimary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Privacy & Security",
                    style = MaterialTheme.typography.titleLarge,
                    color = WorldlyTextPrimary
                )
                PrivacyToggleRow(
                    icon = Icons.Rounded.MyLocation,
                    title = "Opt-In Live Location Sharing",
                    subtitle = if (profile.sharingEnabled) {
                        if (isServiceRunning) "Foreground service active with persistent notification."
                        else "Enabled for accepted unpaused friends."
                    } else {
                        "Off by default. Zero GPS collection while disabled."
                    },
                    checked = profile.sharingEnabled,
                    onCheckedChange = { enable ->
                        if (enable) {
                            if (LocationHelper.hasAnyLocationPermission(context)) {
                                viewModel.setSharingEnabled(context, true)
                            } else {
                                showRationaleDialog = true
                            }
                        } else {
                            viewModel.setSharingEnabled(context, false)
                        }
                    },
                    testTag = "privacy_sharing_switch"
                )
                PrivacyToggleRow(
                    icon = Icons.Rounded.BatteryStd,
                    title = "Share Battery Status",
                    subtitle = "Optionally share battery % & charging state alongside live location.",
                    checked = profile.batterySharingEnabled,
                    onCheckedChange = { viewModel.setBatterySharingEnabled(context, it) },
                    testTag = "privacy_battery_switch"
                )
                PrivacyToggleRow(
                    icon = Icons.Rounded.History,
                    title = "Optional Location History",
                    subtitle = "Save private GPS breadcrumbs strictly for your own timeline.",
                    checked = profile.historyEnabled,
                    onCheckedChange = { viewModel.setHistoryEnabled(it) },
                    testTag = "privacy_history_switch"
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    if (showPermissionsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPermissionsSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = WorldlyObsidian,
            contentColor = WorldlyTextPrimary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Android Permissions",
                    style = MaterialTheme.typography.titleLarge,
                    color = WorldlyTextPrimary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Location Access", color = WorldlyTextPrimary)
                    StatusPill(
                        isLive = hasLocationPerm,
                        label = when {
                            hasFinePerm -> "PRECISE"
                            hasLocationPerm -> "APPROXIMATE"
                            else -> "DENIED"
                        }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Notifications", color = WorldlyTextPrimary)
                    StatusPill(
                        isLive = hasNotifPerm,
                        label = if (hasNotifPerm) "ALLOWED" else "OFF"
                    )
                }
                OutlinedButton(
                    onClick = {
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null)
                        )
                        context.startActivity(intent)
                    },
                    border = BorderStroke(1.dp, WorldlyBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_app_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = null,
                        tint = WorldlyLavender,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open Android App Settings", color = WorldlyTextPrimary)
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    if (showRationaleDialog) {
        LocationRationaleDialog(
            onConfirm = {
                showRationaleDialog = false
                val perms = buildList {
                    add(Manifest.permission.ACCESS_FINE_LOCATION)
                    add(Manifest.permission.ACCESS_COARSE_LOCATION)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }.toTypedArray()
                permissionLauncher.launch(perms)
            },
            onDismiss = { showRationaleDialog = false }
        )
    }

    if (showEditProfileDialog && profile != null) {
        EditProfileDialog(
            profile = profile,
            onSave = { name, handle, status, color ->
                viewModel.updateProfile(name, handle, status, color) {
                    showEditProfileDialog = false
                }
            },
            onDismiss = { showEditProfileDialog = false }
        )
    }

    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            containerColor = WorldlyObsidian,
            title = { Text("Delete Account Permanently?", color = WorldlyRose) },
            text = {
                Text(
                    "This deletes your profile, shared location, friendships, favorite places, and location history, and removes your Firebase Auth account.",
                    color = WorldlyTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAccountDialog = false
                        viewModel.deleteAccount(context)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WorldlyRose),
                    modifier = Modifier.testTag("confirm_delete_account_button")
                ) {
                    Text("Delete Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) {
                    Text("Cancel", color = WorldlyTextSecondary)
                }
            }
        )
    }

    if (requiresRecentLogin) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissRecentLoginNotice() },
            containerColor = WorldlyObsidian,
            title = { Text("Re-Authentication Needed", color = WorldlyTextPrimary) },
            text = {
                Text(
                    "Your WORLDLY data has been erased. Please re-verify your Google session to permanently close your Firebase Auth account.",
                    color = WorldlyTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.dismissRecentLoginNotice()
                        scope.launch {
                            val outcome = AuthRepository.signInWithGoogle(context)
                            if (outcome is AuthRepository.SignInOutcome.Success) {
                                AuthRepository.deleteAuthAccount(context)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WorldlyCyan,
                        contentColor = WorldlyOnAccent
                    )
                ) {
                    Text("Re-Verify & Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissRecentLoginNotice() }) {
                    Text("Later", color = WorldlyTextSecondary)
                }
            }
        )
    }
}

@Composable
private fun ProfileStatCell(
    value: String,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = WorldlyTextPrimary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = WorldlyTextSecondary
        )
    }
}

@Composable
private fun ProfileMenuRow(
    icon: ImageVector,
    title: String,
    tint: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = WorldlyTextPrimary,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = WorldlyTextMuted
        )
    }
}

@Composable
private fun PrivacyToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (checked) WorldlyEmerald else WorldlyTextSecondary,
            modifier = Modifier.size(22.dp)
        )
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
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = WorldlyMidnight,
                checkedTrackColor = WorldlyEmerald
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun EditProfileDialog(
    profile: UserProfile,
    onSave: (String, String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(profile.displayName) }
    var handle by remember { mutableStateOf(profile.handle) }
    var status by remember { mutableStateOf(profile.statusMessage) }
    var selectedColor by remember { mutableStateOf(profile.avatarColorHex) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = WorldlyObsidian,
        title = { Text("Edit Profile", color = WorldlyTextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(50) },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = handle,
                    onValueChange = { handle = it.take(24) },
                    label = { Text("Handle (letters, numbers, _)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = status,
                    onValueChange = { status = it.take(120) },
                    label = { Text("Status Message") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Beacon Color",
                    style = MaterialTheme.typography.labelLarge,
                    color = WorldlyTextSecondary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GeoUtils.AvatarPaletteHex.forEach { hex ->
                        val color = GeoUtils.parseHexColor(hex)
                        val active = hex == selectedColor
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (active) 3.dp else 1.dp,
                                    color = if (active) WorldlyTextPrimary else WorldlyBorder,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = hex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, handle, status, selectedColor) },
                enabled = name.isNotBlank() && handle.trim().length >= 3,
                colors = ButtonDefaults.buttonColors(
                    containerColor = WorldlyCyan,
                    contentColor = WorldlyOnAccent
                ),
                modifier = Modifier.testTag("save_profile_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = WorldlyTextSecondary)
            }
        }
    )
}
