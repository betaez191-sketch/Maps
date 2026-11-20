package com.example.location

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await

data class BatterySnapshot(
    val level: Int?,
    val charging: Boolean?
)

data class LocationFix(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Double
)

object LocationHelper {
    const val CHANNEL_LIVE_SHARING = "worldly_live_sharing"
    const val CHANNEL_ALERTS = "worldly_social_alerts"

    fun hasAnyLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun hasFineLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun readBatterySnapshot(context: Context): BatterySnapshot {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val levelFromIntent = intent?.let {
            val lvl = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (lvl >= 0 && scale > 0) ((lvl * 100f) / scale).toInt().coerceIn(0, 100) else null
        }
        val level = levelFromIntent ?: bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            ?.takeIf { it in 0..100 }
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING,
            BatteryManager.BATTERY_STATUS_FULL -> true
            BatteryManager.BATTERY_STATUS_DISCHARGING,
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> false
            else -> null
        }
        return BatterySnapshot(level = level, charging = charging)
    }

    @SuppressLint("MissingPermission")
    suspend fun fetchSingleFix(context: Context): LocationFix? {
        if (!hasAnyLocationPermission(context)) return null
        val client = LocationServices.getFusedLocationProviderClient(context)
        return try {
            val priority = if (hasFineLocationPermission(context)) {
                Priority.PRIORITY_HIGH_ACCURACY
            } else {
                Priority.PRIORITY_BALANCED_POWER_ACCURACY
            }
            val tokenSource = CancellationTokenSource()
            val loc = client.getCurrentLocation(priority, tokenSource.token).await()
                ?: client.lastLocation.await()
            loc?.let {
                LocationFix(
                    latitude = it.latitude,
                    longitude = it.longitude,
                    accuracyMeters = it.accuracy.toDouble().coerceAtLeast(0.0)
                )
            }
        } catch (_: SecurityException) {
            null
        } catch (_: Exception) {
            null
        }
    }

    fun ensureNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val fgsChannel = NotificationChannel(
                CHANNEL_LIVE_SHARING,
                context.getString(R.string.fgs_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.fgs_channel_desc)
                setShowBadge(false)
            }
            val alertsChannel = NotificationChannel(
                CHANNEL_ALERTS,
                context.getString(R.string.alerts_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.alerts_channel_desc)
            }
            manager.createNotificationChannel(fgsChannel)
            manager.createNotificationChannel(alertsChannel)
        }
    }

    fun notifySocialAlert(context: Context, title: String, body: String, notificationId: Int) {
        if (!hasNotificationPermission(context)) return
        ensureNotificationChannels(context)
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, notification)
    }

    fun startSharingService(context: Context) {
        if (!hasAnyLocationPermission(context)) return
        val intent = Intent(context, LocationSharingService::class.java).apply {
            action = LocationSharingService.ACTION_START_SHARING
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun stopSharingService(context: Context) {
        val intent = Intent(context, LocationSharingService::class.java).apply {
            action = LocationSharingService.ACTION_STOP_SHARING
        }
        context.startService(intent)
    }
}
