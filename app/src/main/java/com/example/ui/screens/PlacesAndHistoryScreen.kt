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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.PlaceCategory
import com.example.domain.model.UiState
import com.example.domain.util.GeoUtils
import com.example.ui.components.CosmicSkyBackdrop
import com.example.ui.theme.WorldlyAmber
import com.example.ui.theme.WorldlyBorder
import com.example.ui.theme.WorldlyCyan
import com.example.ui.theme.WorldlyEmerald
import com.example.ui.theme.WorldlyMidnight
import com.example.ui.theme.WorldlyObsidian
import com.example.ui.theme.WorldlyOnAccent
import com.example.ui.theme.WorldlyRose
import com.example.ui.theme.WorldlySlate
import com.example.ui.theme.WorldlyTextPrimary
import com.example.ui.theme.WorldlyTextSecondary
import com.example.ui.viewmodel.WorldlyViewModel
import java.util.Locale

@Composable
fun PlacesAndHistoryScreen(
    viewModel: WorldlyViewModel,
    initialTab: Int = 0,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 1)) } // 0 = Favorite Places, 1 = Location History
    val profileState by viewModel.userProfile.collectAsStateWithLifecycle()
    val profile = (profileState as? UiState.Success)?.data
    val placesState by viewModel.favoritePlaces.collectAsStateWithLifecycle()
    val historyState by viewModel.locationHistory.collectAsStateWithLifecycle()
    val myLocation by viewModel.mySharedLocation.collectAsStateWithLifecycle()
    val localFix by viewModel.localDeviceFix.collectAsStateWithLifecycle()

    val myLat = myLocation?.takeIf { it.sharingActive }?.latitude ?: localFix?.latitude
    val myLng = myLocation?.takeIf { it.sharingActive }?.longitude ?: localFix?.longitude

    var showAddPlaceDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }

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
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (onBack != null) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("places_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back",
                                tint = WorldlyTextPrimary
                            )
                        }
                    }
                    Text(
                        text = "Places & History",
                        style = MaterialTheme.typography.headlineMedium,
                        color = WorldlyTextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    modifier = Modifier.testTag("tab_favorite_places")
                ) {
                    Text("Favorite Places")
                }
                SegmentedButton(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    modifier = Modifier.testTag("tab_location_history")
                ) {
                    Text("Location History")
                }
            }
        }

        if (selectedTab == 0) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Button(
                        onClick = { showAddPlaceDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WorldlyCyan,
                            contentColor = WorldlyOnAccent
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("add_favorite_place_button")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Bookmark Favorite Place")
                    }
                }

                when (val state = placesState) {
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
                            Text(state.message, color = WorldlyRose)
                        }
                    }
                    is UiState.Success -> {
                        val places = state.data
                        if (places.isEmpty()) {
                            item {
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
                                            imageVector = Icons.Rounded.Place,
                                            contentDescription = null,
                                            tint = WorldlyAmber,
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Text(
                                            text = "No Favorite Places Saved",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = WorldlyTextPrimary
                                        )
                                        Text(
                                            text = "Bookmark Home, Work, or hangout spots to see them on your radar.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = WorldlyTextSecondary
                                        )
                                    }
                                }
                            }
                        } else {
                            items(places, key = { it.id }) { place ->
                                val cat = PlaceCategory.fromWire(place.category)
                                val distText = if (myLat != null && myLng != null) {
                                    GeoUtils.formatDistance(
                                        GeoUtils.distanceMeters(myLat, myLng, place.latitude, place.longitude)
                                    )
                                } else null

                                Surface(
                                    color = WorldlySlate,
                                    shape = RoundedCornerShape(18.dp),
                                    border = BorderStroke(1.dp, WorldlyBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Surface(
                                            color = WorldlyAmber.copy(alpha = 0.16f),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.size(44.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = when (cat) {
                                                        PlaceCategory.HOME -> Icons.Rounded.Home
                                                        PlaceCategory.WORK -> Icons.Rounded.Work
                                                        PlaceCategory.HANGOUT -> Icons.Rounded.LocalCafe
                                                        PlaceCategory.CUSTOM -> Icons.Rounded.Place
                                                    },
                                                    contentDescription = cat.label,
                                                    tint = WorldlyAmber
                                                )
                                            }
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = place.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = WorldlyTextPrimary
                                            )
                                            Text(
                                                text = buildString {
                                                    append(cat.label)
                                                    if (place.addressHint.isNotBlank()) {
                                                        append(" · ${place.addressHint}")
                                                    }
                                                },
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = WorldlyTextSecondary
                                            )
                                            Text(
                                                text = buildString {
                                                    append(
                                                        String.format(
                                                            Locale.US,
                                                            "%.4f, %.4f",
                                                            place.latitude,
                                                            place.longitude
                                                        )
                                                    )
                                                    if (distText != null) append(" · $distText away")
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = WorldlyCyan
                                            )
                                        }
                                        IconButton(
                                            onClick = { viewModel.deleteFavoritePlace(place.id) },
                                            modifier = Modifier.testTag("delete_place_${place.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.DeleteOutline,
                                                contentDescription = "Delete place",
                                                tint = WorldlyRose
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Location History Tab
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Surface(
                        color = WorldlySlate,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, WorldlyBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Opt-In Location History",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = WorldlyTextPrimary
                                    )
                                    Text(
                                        text = "Strictly private to your account. Never shared with friends.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = WorldlyTextSecondary
                                    )
                                }
                                Switch(
                                    checked = profile?.historyEnabled == true,
                                    onCheckedChange = { viewModel.setHistoryEnabled(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = WorldlyMidnight,
                                        checkedTrackColor = WorldlyEmerald
                                    ),
                                    modifier = Modifier.testTag("history_opt_in_switch")
                                )
                            }

                            OutlinedButton(
                                onClick = { showClearHistoryDialog = true },
                                border = BorderStroke(1.dp, WorldlyRose.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("clear_history_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = null,
                                    tint = WorldlyRose,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Clear Recorded History", color = WorldlyRose)
                            }
                        }
                    }
                }

                when (val state = historyState) {
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
                        item { Text(state.message, color = WorldlyRose) }
                    }
                    is UiState.Success -> {
                        val entries = state.data
                        if (entries.isEmpty()) {
                            item {
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
                                            imageVector = Icons.Rounded.History,
                                            contentDescription = null,
                                            tint = WorldlyCyan,
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Text(
                                            text = "No History Breadcrumbs",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = WorldlyTextPrimary
                                        )
                                        Text(
                                            text = "Turn on Opt-In Location History and Live Sharing to log your private trail.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = WorldlyTextSecondary
                                        )
                                    }
                                }
                            }
                        } else {
                            items(entries, key = { it.id }) { entry ->
                                Surface(
                                    color = WorldlyObsidian,
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, WorldlyBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Timeline,
                                            contentDescription = null,
                                            tint = WorldlyCyan
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = String.format(
                                                    Locale.US,
                                                    "%.5f, %.5f",
                                                    entry.latitude,
                                                    entry.longitude
                                                ),
                                                style = MaterialTheme.typography.labelLarge,
                                                color = WorldlyTextPrimary
                                            )
                                            Text(
                                                text = "Accuracy ±${entry.accuracyMeters.toInt()}m · ${GeoUtils.formatRelativeTime(entry.recordedAt)}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = WorldlyTextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    }

    if (showAddPlaceDialog) {
        AddFavoritePlaceDialog(
            defaultLat = myLat ?: 47.6062,
            defaultLng = myLng ?: -122.3321,
            onSave = { name, cat, lat, lng, hint ->
                viewModel.saveFavoritePlace(name, cat, lat, lng, hint) {
                    showAddPlaceDialog = false
                }
            },
            onDismiss = { showAddPlaceDialog = false }
        )
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            containerColor = WorldlyObsidian,
            title = { Text("Clear Location History?", color = WorldlyTextPrimary) },
            text = {
                Text(
                    "All recorded GPS breadcrumbs in your private timeline will be permanently deleted.",
                    color = WorldlyTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearLocationHistory()
                        showClearHistoryDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WorldlyRose),
                    modifier = Modifier.testTag("confirm_clear_history_button")
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel", color = WorldlyTextSecondary)
                }
            }
        )
    }
}

@Composable
private fun AddFavoritePlaceDialog(
    defaultLat: Double,
    defaultLng: Double,
    onSave: (String, PlaceCategory, Double, Double, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var hint by remember { mutableStateOf("") }
    var latText by remember { mutableStateOf(String.format(Locale.US, "%.5f", defaultLat)) }
    var lngText by remember { mutableStateOf(String.format(Locale.US, "%.5f", defaultLng)) }
    var category by remember { mutableStateOf(PlaceCategory.HOME) }

    val parsedLat = latText.toDoubleOrNull()
    val parsedLng = lngText.toDoubleOrNull()
    val validCoords = parsedLat != null && parsedLat in -90.0..90.0 &&
        parsedLng != null && parsedLng in -180.0..180.0

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = WorldlyObsidian,
        title = { Text("Bookmark Place", color = WorldlyTextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(50) },
                    label = { Text("Place Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = hint,
                    onValueChange = { hint = it.take(100) },
                    label = { Text("Neighborhood / Note") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = latText,
                        onValueChange = { latText = it },
                        label = { Text("Lat") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = lngText,
                        onValueChange = { lngText = it },
                        label = { Text("Lng") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PlaceCategory.entries.forEach { cat ->
                        val active = cat == category
                        OutlinedButton(
                            onClick = { category = cat },
                            border = BorderStroke(1.dp, if (active) WorldlyCyan else WorldlyBorder),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = cat.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (active) WorldlyCyan else WorldlyTextSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && parsedLat != null && parsedLng != null && validCoords) {
                        onSave(name, category, parsedLat, parsedLng, hint)
                    }
                },
                enabled = name.isNotBlank() && validCoords,
                colors = ButtonDefaults.buttonColors(
                    containerColor = WorldlyCyan,
                    contentColor = WorldlyOnAccent
                )
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
