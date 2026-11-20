package com.example.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.auth.AuthRepository
import com.example.data.repository.WorldlyRepository
import com.example.domain.model.ChatMessage
import com.example.domain.model.ChatThread
import com.example.domain.model.FavoritePlace
import com.example.domain.model.Friendship
import com.example.domain.model.LocationHistoryEntry
import com.example.domain.model.PlaceCategory
import com.example.domain.model.PublicProfile
import com.example.domain.model.SharedLocation
import com.example.domain.model.UiState
import com.example.domain.model.UserProfile
import com.example.domain.util.GeoUtils
import com.example.location.LocationFix
import com.example.location.LocationHelper
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WorldlyViewModel(
    private val repository: WorldlyRepository,
    val currentUserId: String
) : ViewModel() {

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _searchResult = MutableStateFlow<PublicProfile?>(null)
    val searchResult: StateFlow<PublicProfile?> = _searchResult.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _searchFeedback = MutableStateFlow<String?>(null)
    val searchFeedback: StateFlow<String?> = _searchFeedback.asStateFlow()

    private val _localDeviceFix = MutableStateFlow<LocationFix?>(null)
    val localDeviceFix: StateFlow<LocationFix?> = _localDeviceFix.asStateFlow()

    private val _activeChatFriendUid = MutableStateFlow<String?>(null)
    val activeChatFriendUid: StateFlow<String?> = _activeChatFriendUid.asStateFlow()

    private val _isDeletingAccount = MutableStateFlow(false)
    val isDeletingAccount: StateFlow<Boolean> = _isDeletingAccount.asStateFlow()

    private val _requiresRecentLoginForDelete = MutableStateFlow(false)
    val requiresRecentLoginForDelete: StateFlow<Boolean> = _requiresRecentLoginForDelete.asStateFlow()

    private var seenPendingRequestIds = emptySet<String>()
    private var initialFriendshipsLoaded = false

    val userProfile: StateFlow<UiState<UserProfile?>> =
        repository.observeUserProfile(currentUserId)
            .toUiState("Failed to load profile")
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val mySharedLocation: StateFlow<SharedLocation?> =
        repository.observeMySharedLocation(currentUserId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = null
            )

    val friendships: StateFlow<UiState<List<Friendship>>> =
        repository.observeFriendships(currentUserId)
            .toUiState("Failed to load friendships")
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val friendLocations: StateFlow<List<SharedLocation>> =
        repository.observeAcceptedFriendsLocations(currentUserId)
            .catch { error ->
                val fsErr = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsErr == null && error is CancellationException) throw error
                Log.w(TAG, "Friend location stream error", fsErr ?: error)
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = emptyList()
            )

    val chats: StateFlow<UiState<List<ChatThread>>> =
        repository.observeChats(currentUserId)
            .toUiState("Failed to load messages")
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeChatMessages: StateFlow<UiState<List<ChatMessage>>> =
        _activeChatFriendUid.flatMapLatest { friendUid ->
            if (friendUid.isNullOrBlank()) {
                flowOf(UiState.Success(emptyList()))
            } else {
                val chatId = GeoUtils.pairId(currentUserId, friendUid)
                repository.observeMessages(chatId, currentUserId)
                    .toUiState("Failed to load chat thread")
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val locationHistory: StateFlow<UiState<List<LocationHistoryEntry>>> =
        repository.observeLocationHistory(currentUserId)
            .toUiState("Failed to load location history")
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val favoritePlaces: StateFlow<UiState<List<FavoritePlace>>> =
        repository.observeFavoritePlaces(currentUserId)
            .toUiState("Failed to load favorite places")
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    init {
        viewModelScope.launch {
            val currentUser = Firebase.auth.currentUser
            repository.ensureUserProfile(
                uid = currentUserId,
                defaultName = currentUser?.displayName,
                email = currentUser?.email
            ).onFailure { err ->
                _statusBanner.value = err.localizedMessage ?: "Failed to initialize profile"
            }
        }
    }

    fun observeIncomingNotifications(context: Context) {
        viewModelScope.launch {
            friendships.collect { state ->
                if (state is UiState.Success) {
                    val incomingPending = state.data.filter {
                        it.status == "pending" && it.recipientUid == currentUserId
                    }
                    val currentIds = incomingPending.map { it.id }.toSet()
                    if (initialFriendshipsLoaded) {
                        val newlyArrived = incomingPending.filter { it.id !in seenPendingRequestIds }
                        newlyArrived.firstOrNull()?.let { req ->
                            LocationHelper.notifySocialAlert(
                                context = context,
                                title = "New WORLDLY Friend Request",
                                body = "${req.requesterName} (@${req.requesterHandle}) wants to connect.",
                                notificationId = req.id.hashCode()
                            )
                        }
                    } else {
                        initialFriendshipsLoaded = true
                    }
                    seenPendingRequestIds = currentIds
                }
            }
        }
    }

    fun clearBanner() {
        _statusBanner.value = null
    }

    fun showBanner(message: String) {
        _statusBanner.value = message
    }

    fun refreshLocalFixIfPermitted(context: Context) {
        val profile = (userProfile.value as? UiState.Success)?.data
        if (profile?.sharingEnabled != true) return
        if (!LocationHelper.hasAnyLocationPermission(context)) return
        viewModelScope.launch {
            val fix = LocationHelper.fetchSingleFix(context)
            if (fix != null) {
                _localDeviceFix.value = fix
                val battery = if (profile.batterySharingEnabled) {
                    LocationHelper.readBatterySnapshot(context)
                } else {
                    com.example.location.BatterySnapshot(null, null)
                }
                repository.publishSharedLocation(
                    profile = profile,
                    latitude = fix.latitude,
                    longitude = fix.longitude,
                    accuracyMeters = fix.accuracyMeters,
                    batteryLevel = battery.level,
                    charging = battery.charging,
                    placeLabel = null
                )
            }
        }
    }

    fun setSharingEnabled(context: Context, enabled: Boolean) {
        viewModelScope.launch {
            if (enabled) {
                if (!LocationHelper.hasAnyLocationPermission(context)) {
                    _statusBanner.value = "Grant location permission to turn on live sharing."
                    return@launch
                }
                repository.updatePrivacySettings(uid = currentUserId, sharingEnabled = true)
                    .onSuccess {
                        LocationHelper.startSharingService(context)
                        val fix = LocationHelper.fetchSingleFix(context)
                        val profile = (userProfile.value as? UiState.Success)?.data
                        if (fix != null && profile != null) {
                            _localDeviceFix.value = fix
                            val battery = if (profile.batterySharingEnabled) {
                                LocationHelper.readBatterySnapshot(context)
                            } else {
                                com.example.location.BatterySnapshot(null, null)
                            }
                            repository.publishSharedLocation(
                                profile = profile.copy(sharingEnabled = true),
                                latitude = fix.latitude,
                                longitude = fix.longitude,
                                accuracyMeters = fix.accuracyMeters,
                                batteryLevel = battery.level,
                                charging = battery.charging,
                                placeLabel = null
                            )
                        }
                        _statusBanner.value = "Live location sharing enabled."
                    }
                    .onFailure {
                        _statusBanner.value = it.localizedMessage ?: "Could not enable sharing."
                    }
            } else {
                LocationHelper.stopSharingService(context)
                repository.updatePrivacySettings(uid = currentUserId, sharingEnabled = false)
                    .onSuccess { _statusBanner.value = "Location sharing stopped immediately." }
                    .onFailure { _statusBanner.value = it.localizedMessage ?: "Failed to stop sharing." }
            }
        }
    }

    fun setBatterySharingEnabled(context: Context, enabled: Boolean) {
        viewModelScope.launch {
            repository.updatePrivacySettings(uid = currentUserId, batterySharingEnabled = enabled)
                .onSuccess {
                    if (enabled) refreshLocalFixIfPermitted(context)
                    _statusBanner.value = if (enabled) {
                        "Battery status will be shared when live location is active."
                    } else {
                        "Battery status sharing disabled."
                    }
                }
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Failed to update setting." }
        }
    }

    fun setHistoryEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updatePrivacySettings(uid = currentUserId, historyEnabled = enabled)
                .onSuccess {
                    _statusBanner.value = if (enabled) {
                        "Opt-in location history recording enabled."
                    } else {
                        "Location history recording disabled."
                    }
                }
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Failed to update history setting." }
        }
    }

    fun clearLocationHistory() {
        viewModelScope.launch {
            repository.clearLocationHistory(currentUserId)
                .onSuccess { _statusBanner.value = "Location history cleared." }
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Failed to clear history." }
        }
    }

    fun updateProfile(
        displayName: String,
        handle: String,
        statusMessage: String,
        avatarColorHex: String,
        onComplete: () -> Unit = {}
    ) {
        val current = (userProfile.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            repository.updateProfile(current, displayName, handle, statusMessage, avatarColorHex)
                .onSuccess {
                    _statusBanner.value = "Profile updated."
                    onComplete()
                }
                .onFailure {
                    _statusBanner.value = it.localizedMessage ?: "Could not update profile."
                }
        }
    }

    fun searchFriend(query: String) {
        viewModelScope.launch {
            _isSearching.value = true
            _searchFeedback.value = null
            _searchResult.value = null
            repository.findUserByHandleOrCode(query)
                .onSuccess { found ->
                    _searchResult.value = found
                    if (found == null) {
                        _searchFeedback.value = "No explorer found matching \"$query\". Check exact @handle or Friend Code."
                    }
                }
                .onFailure {
                    _searchFeedback.value = it.localizedMessage ?: "Search failed."
                }
            _isSearching.value = false
        }
    }

    fun clearSearch() {
        _searchResult.value = null
        _searchFeedback.value = null
    }

    fun sendFriendRequest(target: PublicProfile) {
        val me = (userProfile.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            repository.sendFriendRequest(me, target)
                .onSuccess {
                    _statusBanner.value = "Friend request sent to ${target.displayName}."
                    _searchResult.value = null
                }
                .onFailure {
                    _statusBanner.value = it.localizedMessage ?: "Unable to send friend request."
                }
        }
    }

    fun acceptFriendRequest(friendshipId: String) {
        viewModelScope.launch {
            repository.acceptFriendRequest(friendshipId)
                .onSuccess { _statusBanner.value = "Friend request accepted." }
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Failed to accept request." }
        }
    }

    fun removeFriendship(friendshipId: String) {
        viewModelScope.launch {
            repository.removeFriendship(friendshipId)
                .onSuccess { _statusBanner.value = "Connection removed." }
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Failed to remove connection." }
        }
    }

    fun togglePauseForFriend(friendship: Friendship) {
        viewModelScope.launch {
            repository.togglePauseForFriend(friendship, currentUserId)
                .onSuccess {
                    val nowPaused = currentUserId !in friendship.pausedByUids
                    _statusBanner.value = if (nowPaused) {
                        "Paused your live location for ${friendship.otherName(currentUserId)}."
                    } else {
                        "Resumed sharing with ${friendship.otherName(currentUserId)}."
                    }
                }
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Failed to update friend sharing." }
        }
    }

    fun openChat(friendUid: String?) {
        _activeChatFriendUid.value = friendUid
    }

    fun sendChatMessage(
        friendUid: String,
        text: String,
        attachCurrentPin: Boolean = false
    ) {
        val myLoc = mySharedLocation.value?.takeIf { it.sharingActive }
        val localFix = _localDeviceFix.value
        val lat = if (attachCurrentPin) myLoc?.latitude ?: localFix?.latitude else null
        val lng = if (attachCurrentPin) myLoc?.longitude ?: localFix?.longitude else null
        val placeName = if (attachCurrentPin && lat != null) "Current Pin" else null

        viewModelScope.launch {
            repository.sendMessage(
                myUid = currentUserId,
                friendUid = friendUid,
                text = text,
                sharedLat = lat,
                sharedLng = lng,
                sharedPlaceName = placeName
            ).onFailure {
                _statusBanner.value = it.localizedMessage ?: "Failed to send message."
            }
        }
    }

    fun saveFavoritePlace(
        name: String,
        category: PlaceCategory,
        latitude: Double,
        longitude: Double,
        addressHint: String,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.saveFavoritePlace(
                uid = currentUserId,
                name = name,
                category = category,
                latitude = latitude,
                longitude = longitude,
                addressHint = addressHint
            ).onSuccess {
                _statusBanner.value = "Saved \"$name\" to Favorite Places."
                onComplete()
            }.onFailure {
                _statusBanner.value = it.localizedMessage ?: "Could not save favorite place."
            }
        }
    }

    fun deleteFavoritePlace(placeId: String) {
        viewModelScope.launch {
            repository.deleteFavoritePlace(currentUserId, placeId)
                .onSuccess { _statusBanner.value = "Place deleted." }
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Could not delete place." }
        }
    }

    fun deleteAccount(context: Context) {
        if (_isDeletingAccount.value) return
        val profile = (userProfile.value as? UiState.Success)?.data
        viewModelScope.launch {
            _isDeletingAccount.value = true
            _requiresRecentLoginForDelete.value = false
            LocationHelper.stopSharingService(context)
            repository.deleteUserData(profile)
                .onSuccess {
                    AuthRepository.deleteAuthAccount(context)
                        .onFailure { authErr ->
                            val msg = authErr.localizedMessage ?: ""
                            if (msg.contains("recent", ignoreCase = true) ||
                                msg.contains("CREDENTIAL_TOO_OLD", ignoreCase = true)
                            ) {
                                _requiresRecentLoginForDelete.value = true
                                _statusBanner.value = "Data cleared. Sign in again to finish deleting your Google Auth account."
                            } else {
                                _statusBanner.value = msg.ifBlank { "Account deletion failed." }
                            }
                        }
                }
                .onFailure {
                    _statusBanner.value = it.localizedMessage ?: "Failed to delete account data."
                }
            _isDeletingAccount.value = false
        }
    }

    fun dismissRecentLoginNotice() {
        _requiresRecentLoginForDelete.value = false
    }

    private fun <T> Flow<T>.toUiState(fallbackError: String): Flow<UiState<T>> =
        map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, fallbackError, rootError)
                emit(UiState.Error(rootError.message ?: fallbackError))
            }

    private companion object {
        const val TAG = "WorldlyVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
