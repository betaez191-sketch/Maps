package com.example.location

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.repository.WorldlyRepository
import com.example.domain.model.UserProfile
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LocationSharingService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var fusedClient: FusedLocationProviderClient
    private lateinit var repository: WorldlyRepository

    private var profileJob: Job? = null
    private var latestProfile: UserProfile? = null
    private var locationCallback: LocationCallback? = null
    private var lastHistoryWriteMs: Long = 0L

    override fun onCreate() {
        super.onCreate()
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        repository = WorldlyRepository(applicationContext)
        LocationHelper.ensureNotificationChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_SHARING -> {
                stopSharingImmediately()
                return START_NOT_STICKY
            }
            ACTION_START_SHARING, null -> {
                val uid = Firebase.auth.currentUser?.uid
                if (uid == null || !LocationHelper.hasAnyLocationPermission(this)) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                promoteToForeground()
                startObservingAndTracking(uid)
            }
        }
        return START_STICKY
    }

    private fun promoteToForeground() {
        val notification = buildPersistentNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        _isServiceRunning.value = true
    }

    private fun buildPersistentNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            100,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, LocationSharingService::class.java).apply {
            action = ACTION_STOP_SHARING
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            101,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, LocationHelper.CHANNEL_LIVE_SHARING)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.fgs_notif_title))
            .setContentText(getString(R.string.fgs_notif_body))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.fgs_stop_action),
                stopPendingIntent
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    @SuppressLint("MissingPermission")
    private fun startObservingAndTracking(uid: String) {
        profileJob?.cancel()
        profileJob = serviceScope.launch {
            try {
                repository.observeUserProfile(uid).collect { profile ->
                    latestProfile = profile
                    if (profile != null && !profile.sharingEnabled) {
                        stopLocationUpdatesOnly()
                        stopForegroundAndSelf()
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Profile observer ended in service: ${e.message}")
            }
        }

        if (locationCallback != null) return

        val priority = if (LocationHelper.hasFineLocationPermission(this)) {
            Priority.PRIORITY_BALANCED_POWER_ACCURACY
        } else {
            Priority.PRIORITY_LOW_POWER
        }

        val request = LocationRequest.Builder(priority, 12_000L)
            .setMinUpdateIntervalMillis(8_000L)
            .setMinUpdateDistanceMeters(15f)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                val currentUid = Firebase.auth.currentUser?.uid ?: return
                val profile = latestProfile ?: return
                if (!profile.sharingEnabled) return
                if (!LocationHelper.hasAnyLocationPermission(applicationContext)) {
                    stopSharingImmediately()
                    return
                }

                val battery = if (profile.batterySharingEnabled) {
                    LocationHelper.readBatterySnapshot(applicationContext)
                } else {
                    BatterySnapshot(null, null)
                }

                serviceScope.launch {
                    repository.publishSharedLocation(
                        profile = profile,
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        accuracyMeters = loc.accuracy.toDouble().coerceAtLeast(0.0),
                        batteryLevel = battery.level,
                        charging = battery.charging,
                        placeLabel = null
                    )

                    val now = System.currentTimeMillis()
                    if (profile.historyEnabled && now - lastHistoryWriteMs >= 60_000L) {
                        lastHistoryWriteMs = now
                        repository.recordLocationHistory(
                            uid = currentUid,
                            latitude = loc.latitude,
                            longitude = loc.longitude,
                            accuracyMeters = loc.accuracy.toDouble().coerceAtLeast(0.0)
                        )
                    }
                }
            }
        }

        locationCallback = callback
        try {
            fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
        } catch (se: SecurityException) {
            Log.w(TAG, "Location permission revoked while starting service", se)
            stopSharingImmediately()
        }
    }

    private fun stopLocationUpdatesOnly() {
        locationCallback?.let { fusedClient.removeLocationUpdates(it) }
        locationCallback = null
    }

    private fun stopSharingImmediately() {
        stopLocationUpdatesOnly()
        val uid = Firebase.auth.currentUser?.uid
        serviceScope.launch {
            if (uid != null) {
                repository.updatePrivacySettings(uid = uid, sharingEnabled = false)
            }
            stopForegroundAndSelf()
        }
    }

    private fun stopForegroundAndSelf() {
        _isServiceRunning.value = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopLocationUpdatesOnly()
        profileJob?.cancel()
        _isServiceRunning.value = false
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "LocationSharingService"
        private const val NOTIFICATION_ID = 4091
        const val ACTION_START_SHARING = "com.example.worldly.START_SHARING"
        const val ACTION_STOP_SHARING = "com.example.worldly.STOP_SHARING"

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()
    }
}
