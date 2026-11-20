package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firestore.OperationType
import com.example.data.firestore.handleFirestoreError
import com.example.domain.model.ChatMessage
import com.example.domain.model.ChatThread
import com.example.domain.model.FavoritePlace
import com.example.domain.model.Friendship
import com.example.domain.model.LocationHistoryEntry
import com.example.domain.model.PlaceCategory
import com.example.domain.model.PublicProfile
import com.example.domain.model.SharedLocation
import com.example.domain.model.UserProfile
import com.example.domain.util.GeoUtils
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Locale

class WorldlyRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

    // --- USER PROFILE & INITIALIZATION ---

    fun observeUserProfile(uid: String): Flow<UserProfile?> =
        db.collection("users").document(uid)
            .snapshots()
            .map { snapshot ->
                if (!snapshot.exists()) null
                else snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                logFlowError(error, OperationType.GET, "users/$uid", uid)
                throw error
            }

    suspend fun ensureUserProfile(
        uid: String,
        defaultName: String?,
        email: String?
    ): Result<UserProfile> {
        val userPath = "users/$uid"
        return try {
            val currentUid = requireUserId()
            check(currentUid == uid) { "Authenticated user mismatch." }

            val existingSnap = db.collection("users").document(uid).get().await()
            if (existingSnap.exists()) {
                val profile = existingSnap.toObject(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                if (profile != null) return Result.success(profile)
            }

            val cleanName = (defaultName?.takeIf { it.isNotBlank() } ?: "Explorer").take(50)
            val baseHandle = GeoUtils.sanitizeHandle(
                email?.substringBefore("@") ?: cleanName
            )
            val uniqueHandle = resolveAvailableHandle(baseHandle, uid)
            val friendCode = GeoUtils.generateFriendCode(uid)
            val avatarColor = GeoUtils.AvatarPaletteHex[
                (uid.hashCode() and 0x7FFFFFFF) % GeoUtils.AvatarPaletteHex.size
            ]

            val batch = db.batch()
            val userPayload = mapOf(
                "userId" to uid,
                "email" to email?.take(200),
                "displayName" to cleanName,
                "handle" to uniqueHandle,
                "statusMessage" to "Available on WORLDLY",
                "avatarColorHex" to avatarColor,
                "friendCode" to friendCode,
                "sharingEnabled" to false,
                "batterySharingEnabled" to false,
                "historyEnabled" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>

            val publicPayload = mapOf(
                "userId" to uid,
                "displayName" to cleanName,
                "handle" to uniqueHandle,
                "statusMessage" to "Available on WORLDLY",
                "avatarColorHex" to avatarColor,
                "friendCode" to friendCode,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            val handlePayload = mapOf(
                "userId" to uid,
                "handle" to uniqueHandle,
                "displayName" to cleanName,
                "avatarColorHex" to avatarColor,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            val codePayload = mapOf(
                "userId" to uid,
                "friendCode" to friendCode,
                "handle" to uniqueHandle,
                "displayName" to cleanName,
                "avatarColorHex" to avatarColor,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            batch.set(db.collection("users").document(uid), userPayload)
            batch.set(db.collection("publicProfiles").document(uid), publicPayload)
            batch.set(db.collection("handleLookup").document(uniqueHandle), handlePayload)
            batch.set(db.collection("friendCodeLookup").document(friendCode), codePayload)
            batch.commit().await()

            Result.success(
                UserProfile(
                    id = uid,
                    userId = uid,
                    email = email,
                    displayName = cleanName,
                    handle = uniqueHandle,
                    statusMessage = "Available on WORLDLY",
                    avatarColorHex = avatarColor,
                    friendCode = friendCode,
                    sharingEnabled = false,
                    batterySharingEnabled = false,
                    historyEnabled = false
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, userPath)
            Result.failure(e)
        }
    }

    private suspend fun resolveAvailableHandle(base: String, uid: String): String {
        val candidate = base.take(24)
        val snap = db.collection("handleLookup").document(candidate).get().await()
        if (!snap.exists() || snap.getString("userId") == uid) return candidate
        val suffix = "_" + uid.take(4).lowercase(Locale.US).replace(Regex("[^a-z0-9]"), "x")
        return (candidate.take(24 - suffix.length) + suffix)
    }

    suspend fun updateProfile(
        currentProfile: UserProfile,
        displayName: String,
        rawHandle: String,
        statusMessage: String,
        avatarColorHex: String
    ): Result<Unit> {
        val uid = currentProfile.userId.ifBlank { auth.currentUser?.uid ?: "" }
        val userPath = "users/$uid"
        return try {
            val authUid = requireUserId()
            check(authUid == uid) { "Cannot update another user's profile." }

            val cleanName = displayName.trim().take(50)
            require(cleanName.isNotEmpty()) { "Display name cannot be empty." }
            val cleanHandle = GeoUtils.sanitizeHandle(rawHandle)
            require(cleanHandle.length in 3..24) { "Handle must be 3-24 characters." }
            val cleanStatus = statusMessage.trim().take(120)

            if (cleanHandle != currentProfile.handle) {
                val lookup = db.collection("handleLookup").document(cleanHandle).get().await()
                if (lookup.exists() && lookup.getString("userId") != uid) {
                    return Result.failure(IllegalArgumentException("Handle @$cleanHandle is already taken."))
                }
            }

            val batch = db.batch()
            batch.update(
                db.collection("users").document(uid),
                mapOf(
                    "displayName" to cleanName,
                    "handle" to cleanHandle,
                    "statusMessage" to cleanStatus,
                    "avatarColorHex" to avatarColorHex,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                db.collection("publicProfiles").document(uid),
                mapOf(
                    "displayName" to cleanName,
                    "handle" to cleanHandle,
                    "statusMessage" to cleanStatus,
                    "avatarColorHex" to avatarColorHex,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (cleanHandle != currentProfile.handle && currentProfile.handle.isNotBlank()) {
                batch.delete(db.collection("handleLookup").document(currentProfile.handle))
                batch.set(
                    db.collection("handleLookup").document(cleanHandle),
                    mapOf(
                        "userId" to uid,
                        "handle" to cleanHandle,
                        "displayName" to cleanName,
                        "avatarColorHex" to avatarColorHex,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.update(
                    db.collection("handleLookup").document(cleanHandle),
                    mapOf(
                        "displayName" to cleanName,
                        "avatarColorHex" to avatarColorHex,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            if (currentProfile.friendCode.isNotBlank()) {
                batch.update(
                    db.collection("friendCodeLookup").document(currentProfile.friendCode),
                    mapOf(
                        "handle" to cleanHandle,
                        "displayName" to cleanName,
                        "avatarColorHex" to avatarColorHex,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userPath)
            Result.failure(e)
        }
    }

    suspend fun updatePrivacySettings(
        uid: String,
        sharingEnabled: Boolean? = null,
        batterySharingEnabled: Boolean? = null,
        historyEnabled: Boolean? = null
    ): Result<Unit> {
        val path = "users/$uid"
        return try {
            val authUid = requireUserId()
            check(authUid == uid)
            val updates = mutableMapOf<String, Any>("updatedAt" to FieldValue.serverTimestamp())
            if (sharingEnabled != null) updates["sharingEnabled"] = sharingEnabled
            if (batterySharingEnabled != null) updates["batterySharingEnabled"] = batterySharingEnabled
            if (historyEnabled != null) updates["historyEnabled"] = historyEnabled
            db.collection("users").document(uid).update(updates).await()

            if (sharingEnabled == false) {
                stopSharedLocation(uid)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- FRIEND SEARCH & FRIENDSHIPS ---

    suspend fun findUserByHandleOrCode(query: String): Result<PublicProfile?> {
        val trimmed = query.trim().removePrefix("@")
        if (trimmed.isBlank()) return Result.success(null)
        return try {
            val uid = requireUserId()
            val normalizedCode = trimmed.uppercase(Locale.US)
            val targetUid: String? = if (normalizedCode.startsWith("WLD") && normalizedCode.length in 6..10) {
                db.collection("friendCodeLookup").document(normalizedCode).get().await()
                    .takeIf { it.exists() }?.getString("userId")
            } else {
                val handle = trimmed.lowercase(Locale.US)
                db.collection("handleLookup").document(handle).get().await()
                    .takeIf { it.exists() }?.getString("userId")
                    ?: if (normalizedCode.matches(Regex("^[A-Z0-9]{6,10}$"))) {
                        db.collection("friendCodeLookup").document(normalizedCode).get().await()
                            .takeIf { it.exists() }?.getString("userId")
                    } else null
            }

            if (targetUid.isNullOrBlank() || targetUid == uid) {
                return Result.success(null)
            }
            val pubSnap = db.collection("publicProfiles").document(targetUid).get().await()
            val profile = pubSnap.toObject(
                PublicProfile::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(profile)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "handleLookup/$trimmed")
            Result.failure(e)
        }
    }

    fun observeFriendships(uid: String): Flow<List<Friendship>> =
        db.collection("friendships")
            .whereArrayContains("participantUids", uid)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Friendship::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                logFlowError(error, OperationType.LIST, "friendships", uid)
                throw error
            }

    suspend fun sendFriendRequest(me: UserProfile, target: PublicProfile): Result<Unit> {
        val fId = GeoUtils.pairId(me.userId, target.userId)
        val path = "friendships/$fId"
        return try {
            val uid = requireUserId()
            check(uid == me.userId && uid != target.userId)
            val existing = db.collection("friendships").document(fId).get().await()
            if (existing.exists()) {
                return Result.failure(IllegalStateException("Friendship or pending request already exists."))
            }
            val payload = mapOf(
                "participantUids" to GeoUtils.sortedPair(me.userId, target.userId),
                "requesterUid" to me.userId,
                "recipientUid" to target.userId,
                "requesterName" to me.displayName,
                "requesterHandle" to me.handle,
                "requesterColor" to me.avatarColorHex,
                "recipientName" to target.displayName,
                "recipientHandle" to target.handle,
                "recipientColor" to target.avatarColorHex,
                "status" to "pending",
                "pausedByUids" to emptyList<String>(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("friendships").document(fId).set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun acceptFriendRequest(friendshipId: String): Result<Unit> {
        val path = "friendships/$friendshipId"
        return try {
            requireUserId()
            db.collection("friendships").document(friendshipId)
                .update(
                    mapOf(
                        "status" to "accepted",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun removeFriendship(friendshipId: String): Result<Unit> {
        val path = "friendships/$friendshipId"
        return try {
            requireUserId()
            db.collection("friendships").document(friendshipId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun togglePauseForFriend(friendship: Friendship, myUid: String): Result<Unit> {
        val path = "friendships/${friendship.id}"
        return try {
            requireUserId()
            val updatedPaused = if (myUid in friendship.pausedByUids) {
                friendship.pausedByUids - myUid
            } else {
                (friendship.pausedByUids + myUid).distinct().take(2)
            }
            db.collection("friendships").document(friendship.id)
                .update(
                    mapOf(
                        "pausedByUids" to updatedPaused,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- OPT-IN REAL-TIME LOCATIONS ---

    fun observeMySharedLocation(uid: String): Flow<SharedLocation?> =
        db.collection("sharedLocations").document(uid)
            .snapshots()
            .map { snapshot ->
                if (!snapshot.exists()) null
                else snapshot.toObject(SharedLocation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                logFlowError(error, OperationType.GET, "sharedLocations/$uid", uid)
                emit(null)
            }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeAcceptedFriendsLocations(uid: String): Flow<List<SharedLocation>> {
        return observeFriendships(uid).flatMapLatest { friendships ->
            val accepted = friendships.filter { it.status == "accepted" }
            if (accepted.isEmpty()) {
                flowOf(emptyList())
            } else {
                val flows = accepted.map { friendship ->
                    val friendUid = friendship.otherUid(uid)
                    if (friendUid in friendship.pausedByUids) {
                        flowOf<SharedLocation?>(null)
                    } else {
                        db.collection("sharedLocations").document(friendUid)
                            .snapshots()
                            .map { snap ->
                                val loc = if (snap.exists()) {
                                    snap.toObject(
                                        SharedLocation::class.java,
                                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                                    )
                                } else null
                                loc?.takeIf { it.sharingActive }
                            }
                            .catch { error ->
                                // Friend may have paused or disabled sharing -> PERMISSION_DENIED is expected privacy behavior
                                val fsErr = generateSequence(error) { it.cause }
                                    .filterIsInstance<FirebaseFirestoreException>()
                                    .firstOrNull()
                                if (fsErr == null && error is CancellationException) throw error
                                emit(null)
                            }
                    }
                }
                combine(flows) { array -> array.filterNotNull() }
            }
        }
    }

    suspend fun publishSharedLocation(
        profile: UserProfile,
        latitude: Double,
        longitude: Double,
        accuracyMeters: Double,
        batteryLevel: Int?,
        charging: Boolean?,
        placeLabel: String?
    ): Result<Unit> {
        val uid = profile.userId
        val path = "sharedLocations/$uid"
        return try {
            val authUid = requireUserId()
            check(authUid == uid)
            val payload = mutableMapOf<String, Any?>(
                "userId" to uid,
                "displayName" to profile.displayName,
                "handle" to profile.handle,
                "avatarColorHex" to profile.avatarColorHex,
                "sharingActive" to true,
                "latitude" to latitude.coerceIn(-90.0, 90.0),
                "longitude" to longitude.coerceIn(-180.0, 180.0),
                "accuracyMeters" to accuracyMeters.coerceIn(0.0, 100_000.0),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (profile.batterySharingEnabled && batteryLevel != null) {
                payload["batteryLevel"] = batteryLevel.coerceIn(0, 100)
                if (charging != null) payload["charging"] = charging
            }
            if (!placeLabel.isNullOrBlank()) {
                payload["placeLabel"] = placeLabel.take(60)
            }
            val cleanPayload = payload.filterValues { it != null } as Map<String, Any>
            db.collection("sharedLocations").document(uid).set(cleanPayload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun stopSharedLocation(uid: String): Result<Unit> {
        val path = "sharedLocations/$uid"
        return try {
            val authUid = requireUserId()
            check(authUid == uid)
            val docRef = db.collection("sharedLocations").document(uid)
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(
                    mapOf(
                        "sharingActive" to false,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- PRIVATE 1-TO-1 MESSAGING ---

    fun observeChats(uid: String): Flow<List<ChatThread>> =
        db.collection("chats")
            .whereArrayContains("participantUids", uid)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(ChatThread::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.updatedAt ?: com.google.firebase.Timestamp(0, 0) }
            }
            .catch { error ->
                logFlowError(error, OperationType.LIST, "chats", uid)
                throw error
            }

    fun observeMessages(chatId: String, uid: String): Flow<List<ChatMessage>> =
        db.collection("chats").document(chatId).collection("messages")
            .whereArrayContains("participantUids", uid)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(ChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.createdAt ?: com.google.firebase.Timestamp(0, 0) }
            }
            .catch { error ->
                logFlowError(error, OperationType.LIST, "chats/$chatId/messages", uid)
                throw error
            }

    suspend fun sendMessage(
        myUid: String,
        friendUid: String,
        text: String,
        sharedLat: Double? = null,
        sharedLng: Double? = null,
        sharedPlaceName: String? = null
    ): Result<Unit> {
        val chatId = GeoUtils.pairId(myUid, friendUid)
        val path = "chats/$chatId/messages"
        return try {
            val authUid = requireUserId()
            check(authUid == myUid)
            val cleanText = text.trim().take(500)
            require(cleanText.isNotEmpty()) { "Message cannot be empty." }

            val participants = GeoUtils.sortedPair(myUid, friendUid)
            val chatPayload = mapOf(
                "participantUids" to participants,
                "lastMessageText" to cleanText,
                "lastSenderUid" to myUid,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val msgPayload = mutableMapOf<String, Any?>(
                "participantUids" to participants,
                "senderUid" to myUid,
                "text" to cleanText,
                "sharedLat" to sharedLat,
                "sharedLng" to sharedLng,
                "sharedPlaceName" to sharedPlaceName?.take(60),
                "createdAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>

            val batch = db.batch()
            batch.set(db.collection("chats").document(chatId), chatPayload)
            batch.set(db.collection("chats").document(chatId).collection("messages").document(), msgPayload)
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    // --- LOCATION HISTORY (OPT-IN) ---

    fun observeLocationHistory(uid: String): Flow<List<LocationHistoryEntry>> =
        db.collection("users").document(uid).collection("locationHistory")
            .orderBy("recordedAt", Query.Direction.DESCENDING)
            .limit(100)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    LocationHistoryEntry::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                logFlowError(error, OperationType.LIST, "users/$uid/locationHistory", uid)
                throw error
            }

    suspend fun recordLocationHistory(
        uid: String,
        latitude: Double,
        longitude: Double,
        accuracyMeters: Double
    ): Result<Unit> {
        val path = "users/$uid/locationHistory"
        return try {
            val authUid = requireUserId()
            check(authUid == uid)
            val payload = mapOf(
                "latitude" to latitude.coerceIn(-90.0, 90.0),
                "longitude" to longitude.coerceIn(-180.0, 180.0),
                "accuracyMeters" to accuracyMeters.coerceIn(0.0, 100_000.0),
                "recordedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid).collection("locationHistory")
                .document().set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun clearLocationHistory(uid: String): Result<Unit> {
        val path = "users/$uid/locationHistory"
        return try {
            val authUid = requireUserId()
            check(authUid == uid)
            val col = db.collection("users").document(uid).collection("locationHistory")
            val docs = col.limit(200).get().await().documents
            if (docs.isNotEmpty()) {
                val batch = db.batch()
                docs.forEach { batch.delete(it.reference) }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- FAVORITE PLACES ---

    fun observeFavoritePlaces(uid: String): Flow<List<FavoritePlace>> =
        db.collection("users").document(uid).collection("favoritePlaces")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(FavoritePlace::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: com.google.firebase.Timestamp(0, 0) }
            }
            .catch { error ->
                logFlowError(error, OperationType.LIST, "users/$uid/favoritePlaces", uid)
                throw error
            }

    suspend fun saveFavoritePlace(
        uid: String,
        name: String,
        category: PlaceCategory,
        latitude: Double,
        longitude: Double,
        addressHint: String
    ): Result<Unit> {
        val path = "users/$uid/favoritePlaces"
        return try {
            val authUid = requireUserId()
            check(authUid == uid)
            val cleanName = name.trim().take(50)
            require(cleanName.isNotEmpty()) { "Place name cannot be empty." }
            val payload = mapOf(
                "name" to cleanName,
                "category" to category.wireValue,
                "latitude" to latitude.coerceIn(-90.0, 90.0),
                "longitude" to longitude.coerceIn(-180.0, 180.0),
                "addressHint" to addressHint.trim().take(100),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid).collection("favoritePlaces")
                .document().set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteFavoritePlace(uid: String, placeId: String): Result<Unit> {
        val path = "users/$uid/favoritePlaces/$placeId"
        return try {
            val authUid = requireUserId()
            check(authUid == uid)
            db.collection("users").document(uid).collection("favoritePlaces")
                .document(placeId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- FULL ACCOUNT DATA DELETION ---

    suspend fun deleteUserData(profile: UserProfile?): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in."))
        return try {
            val historyDocs = db.collection("users").document(uid).collection("locationHistory")
                .get().await().documents
            val placeDocs = db.collection("users").document(uid).collection("favoritePlaces")
                .get().await().documents
            val friendships = db.collection("friendships")
                .whereArrayContains("participantUids", uid).get().await().documents

            val batch = db.batch()
            historyDocs.forEach { batch.delete(it.reference) }
            placeDocs.forEach { batch.delete(it.reference) }
            friendships.forEach { batch.delete(it.reference) }
            batch.delete(db.collection("sharedLocations").document(uid))
            batch.delete(db.collection("publicProfiles").document(uid))
            if (profile != null && profile.handle.isNotBlank()) {
                batch.delete(db.collection("handleLookup").document(profile.handle))
            }
            if (profile != null && profile.friendCode.isNotBlank()) {
                batch.delete(db.collection("friendCodeLookup").document(profile.friendCode))
            }
            batch.delete(db.collection("users").document(uid))
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$uid")
            Result.failure(e)
        }
    }

    private fun logFlowError(
        error: Throwable,
        operationType: OperationType,
        path: String,
        expectedUid: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
    }
}
