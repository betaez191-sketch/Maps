package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.domain.model.UiState
import com.example.location.LocationHelper
import com.example.ui.components.AstroCatPeeking
import com.example.ui.components.CosmicSkyBackdrop
import com.example.ui.components.LocationRationaleDialog
import com.example.ui.components.StatusPill
import com.example.ui.theme.WorldlyBorder
import com.example.ui.theme.WorldlyCyan
import com.example.ui.theme.WorldlyCyanDim
import com.example.ui.theme.WorldlyEmerald
import com.example.ui.theme.WorldlyLavender
import com.example.ui.theme.WorldlyObsidian
import com.example.ui.theme.WorldlyRose
import com.example.ui.theme.WorldlySlate
import com.example.ui.theme.WorldlyTextPrimary
import com.example.ui.theme.WorldlyTextSecondary
import com.example.ui.viewmodel.WorldlyViewModel

private enum class ShareDurationOption(val label: String) {
    ONE_HOUR("1h"),
    THREE_HOURS("3h"),
    TWELVE_HOURS("12h"),
    CONTINUOUS("Until Off")
}

@Composable
fun ShareLocationScreen(
    viewModel: WorldlyViewModel,
    onOpenFriends: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val profileState by viewModel.userProfile.collectAsStateWithLifecycle()
    val profile = (profileState as? UiState.Success)?.data
    val isSharing = profile?.sharingEnabled == true

    var selectedDuration by remember { mutableStateOf(ShareDurationOption.ONE_HOUR) }
    var showRationaleDialog by remember { mutableStateOf(false) }

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

    val pulseTransition = rememberInfiniteTransition(label = "shareRadar")
    val pulse by pulseTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse),
        label = "pulseVal"
    )

    Box(modifier = modifier.fillMaxSize()) {
        CosmicSkyBackdrop(
            modifier = Modifier.fillMaxSize(),
            showSkyline = false,
            showClouds = true
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Share Location",
                    style = MaterialTheme.typography.headlineMedium,
                    color = WorldlyTextPrimary
                )
                IconButton(
                    onClick = onOpenFriends,
                    modifier = Modifier.testTag("share_screen_friends_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Group,
                        contentDescription = "Manage circle",
                        tint = WorldlyLavender
                    )
                }
            }

            // Concentric Glowing Radar Core
            Box(
                modifier = Modifier.size(230.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val c = Offset(size.width / 2f, size.height / 2f)
                    val rMax = size.minDimension * 0.48f
                    val ringColor = if (isSharing) WorldlyEmerald else WorldlyCyan
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(ringColor.copy(alpha = 0.28f), Color.Transparent),
                            center = c,
                            radius = rMax * pulse
                        ),
                        radius = rMax * pulse,
                        center = c
                    )
                    listOf(0.55f, 0.78f, 1.0f).forEach { factor ->
                        drawCircle(
                            color = ringColor.copy(alpha = 0.35f),
                            radius = rMax * factor,
                            center = c,
                            style = Stroke(width = 2.5f)
                        )
                    }
                }

                Surface(
                    color = WorldlySlate,
                    shape = CircleShape,
                    border = BorderStroke(2.dp, if (isSharing) WorldlyEmerald else WorldlyCyan),
                    shadowElevation = 12.dp,
                    modifier = Modifier
                        .size(86.dp)
                        .clickable {
                            if (isSharing) {
                                viewModel.setSharingEnabled(context, false)
                            } else if (LocationHelper.hasAnyLocationPermission(context)) {
                                viewModel.setSharingEnabled(context, true)
                            } else {
                                showRationaleDialog = true
                            }
                        }
                        .testTag("share_radar_center_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.NearMe,
                            contentDescription = "Toggle sharing",
                            tint = if (isSharing) WorldlyEmerald else WorldlyLavender,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }

            // Bottom Card with Peeking Astro-Cat & Duration Selector
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                Surface(
                    color = WorldlyObsidian.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(28.dp),
                    border = BorderStroke(
                        1.5.dp,
                        if (isSharing) WorldlyEmerald.copy(alpha = 0.6f) else WorldlyCyan.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 66.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(
                            start = 18.dp,
                            end = 18.dp,
                            top = 34.dp,
                            bottom = 18.dp
                        ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        StatusPill(
                            isLive = isSharing,
                            label = if (isSharing) {
                                "SHARING ACTIVE · ${selectedDuration.label.uppercase()}"
                            } else {
                                "GHOST MODE · PAUSED"
                            }
                        )

                        Text(
                            text = if (isSharing) {
                                "Your live GPS beacon is visible to accepted friends."
                            } else {
                                "Share your location with friends in real-time!"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = WorldlyTextPrimary,
                            textAlign = TextAlign.Center
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ShareDurationOption.entries.forEach { opt ->
                                val selected = opt == selectedDuration
                                Surface(
                                    color = if (selected) WorldlyCyan else WorldlySlate,
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (selected) WorldlyLavender else WorldlyBorder
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedDuration = opt }
                                        .testTag("duration_chip_${opt.name}")
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = opt.label,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (selected) Color.White else WorldlyTextSecondary,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (isSharing) {
                                        Brush.horizontalGradient(
                                            colors = listOf(WorldlyRose, Color(0xFFBE123C))
                                        )
                                    } else {
                                        Brush.horizontalGradient(
                                            colors = listOf(WorldlyCyanDim, WorldlyCyan, WorldlyLavender)
                                        )
                                    }
                                )
                                .clickable {
                                    if (isSharing) {
                                        viewModel.setSharingEnabled(context, false)
                                    } else if (LocationHelper.hasAnyLocationPermission(context)) {
                                        viewModel.setSharingEnabled(context, true)
                                    } else {
                                        showRationaleDialog = true
                                    }
                                }
                                .testTag("share_primary_cta_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (isSharing) Icons.Rounded.StopCircle else Icons.Rounded.NearMe,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isSharing) "Stop Sharing Immediately" else "Start Sharing",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                AstroCatPeeking(
                    size = 94.dp,
                    wink = isSharing,
                    showHeartBubble = true,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 18.dp)
                        .offset(y = (-6).dp)
                )
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
}
