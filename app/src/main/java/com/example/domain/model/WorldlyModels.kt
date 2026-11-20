package com.example.domain.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String? = null,
    val displayName: String = "",
    val handle: String = "",
    val statusMessage: String = "",
    val avatarColorHex: String = "#22D3EE",
    val friendCode: String = "",
    val sharingEnabled: Boolean = false,
    val batterySharingEnabled: Boolean = false,
    val historyEnabled: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PublicProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val handle: String = "",
    val statusMessage: String = "",
    val avatarColorHex: String = "#22D3EE",
    val friendCode: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Friendship(
    @DocumentId val id: String = "",
    val participantUids: List<String> = emptyList(),
    val requesterUid: String = "",
    val recipientUid: String = "",
    val requesterName: String = "",
    val requesterHandle: String = "",
    val requesterColor: String = "#22D3EE",
    val recipientName: String = "",
    val recipientHandle: String = "",
    val recipientColor: String = "#10B981",
    val status: String = "pending",
    val pausedByUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun otherUid(myUid: String): String =
        if (requesterUid == myUid) recipientUid else requesterUid

    fun otherName(myUid: String): String =
        if (requesterUid == myUid) recipientName else requesterName

    fun otherHandle(myUid: String): String =
        if (requesterUid == myUid) recipientHandle else requesterHandle

    fun otherColor(myUid: String): String =
        if (requesterUid == myUid) recipientColor else requesterColor
}

@Keep
@IgnoreExtraProperties
data class SharedLocation(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val handle: String = "",
    val avatarColorHex: String = "#22D3EE",
    val sharingActive: Boolean = false,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val accuracyMeters: Double = 0.0,
    val batteryLevel: Int? = null,
    val charging: Boolean? = null,
    val placeLabel: String? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChatThread(
    @DocumentId val id: String = "",
    val participantUids: List<String> = emptyList(),
    val lastMessageText: String = "",
    val lastSenderUid: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val participantUids: List<String> = emptyList(),
    val senderUid: String = "",
    val text: String = "",
    val sharedLat: Double? = null,
    val sharedLng: Double? = null,
    val sharedPlaceName: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LocationHistoryEntry(
    @DocumentId val id: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val accuracyMeters: Double = 0.0,
    @ServerTimestamp val recordedAt: Timestamp? = null
)

enum class PlaceCategory(val wireValue: String, val label: String) {
    HOME("home", "Home"),
    WORK("work", "Work"),
    HANGOUT("hangout", "Hangout"),
    CUSTOM("custom", "Pinned");

    companion object {
        fun fromWire(raw: String): PlaceCategory =
            entries.firstOrNull { it.wireValue == raw } ?: CUSTOM
    }
}

@Keep
@IgnoreExtraProperties
data class FavoritePlace(
    @DocumentId val id: String = "",
    val name: String = "",
    val category: String = PlaceCategory.CUSTOM.wireValue,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val addressHint: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
