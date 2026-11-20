package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LocationOff
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.util.GeoUtils
import com.example.ui.theme.WorldlyAmber
import com.example.ui.theme.WorldlyBorder
import com.example.ui.theme.WorldlyCyan
import com.example.ui.theme.WorldlyEmerald
import com.example.ui.theme.WorldlyGlass
import com.example.ui.theme.WorldlyObsidian
import com.example.ui.theme.WorldlyRose
import com.example.ui.theme.WorldlySlate
import com.example.ui.theme.WorldlyTextPrimary
import com.example.ui.theme.WorldlyTextSecondary

@Composable
fun AvatarBadge(
    name: String,
    colorHex: String,
    size: Dp = 44.dp,
    isLive: Boolean = false
) {
    val accent = GeoUtils.parseHexColor(colorHex, WorldlyCyan)
    val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "W"
    Box(contentAlignment = Alignment.BottomEnd) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.22f))
                .border(1.5.dp, accent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = accent
            )
        }
        if (isLive) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(WorldlyEmerald)
                    .border(2.dp, WorldlyObsidian, CircleShape)
            )
        }
    }
}

@Composable
fun StatusPill(
    isLive: Boolean,
    label: String,
    modifier: Modifier = Modifier
) {
    val color = if (isLive) WorldlyEmerald else WorldlyTextSecondary
    Surface(
        modifier = modifier,
        color = if (isLive) WorldlyEmerald.copy(alpha = 0.16f) else WorldlySlate,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun BatteryChip(
    batteryLevel: Int,
    charging: Boolean?,
    modifier: Modifier = Modifier
) {
    val tint = when {
        charging == true -> WorldlyEmerald
        batteryLevel <= 20 -> WorldlyRose
        batteryLevel <= 45 -> WorldlyAmber
        else -> WorldlyCyan
    }
    Surface(
        modifier = modifier,
        color = tint.copy(alpha = 0.12f),
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (charging == true) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BatteryStd,
                contentDescription = "Battery $batteryLevel%",
                tint = tint,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = "$batteryLevel%",
                style = MaterialTheme.typography.labelSmall,
                color = tint
            )
        }
    }
}

@Composable
fun WorldlyBanner(
    message: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = !message.isNullOrBlank(),
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            color = WorldlyGlass,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, WorldlyCyan.copy(alpha = 0.5f)),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = message.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = WorldlyTextPrimary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("dismiss_banner_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Dismiss notification",
                        tint = WorldlyTextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun LocationRationaleDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = WorldlyObsidian,
        icon = {
            Icon(
                imageVector = Icons.Rounded.MyLocation,
                contentDescription = null,
                tint = WorldlyCyan,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = stringResource(R.string.location_rationale_title),
                style = MaterialTheme.typography.titleLarge,
                color = WorldlyTextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.location_rationale_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = WorldlyTextSecondary
                )
                Surface(
                    color = WorldlySlate,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, WorldlyBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocationOff,
                            contentDescription = null,
                            tint = WorldlyEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Only accepted friends you have not paused can view your coordinates.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = WorldlyTextPrimary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = WorldlyCyan),
                modifier = Modifier.testTag("confirm_location_rationale_button")
            ) {
                Text(
                    text = stringResource(R.string.location_rationale_confirm),
                    color = Color.White
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_location_rationale_button")
            ) {
                Text(
                    text = stringResource(R.string.location_rationale_cancel),
                    color = WorldlyTextSecondary
                )
            }
        }
    )
}
