package com.example.domain.util

import androidx.compose.ui.graphics.Color
import com.google.firebase.Timestamp
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object GeoUtils {
    private const val EARTH_RADIUS_METERS = 6_371_000.0

    fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_METERS * c
    }

    fun formatDistance(meters: Double): String {
        return when {
            meters < 100.0 -> "${meters.toInt()} m"
            meters < 1000.0 -> "${(meters / 10).toInt() * 10} m"
            meters < 10_000.0 -> String.format(Locale.US, "%.1f km", meters / 1000.0)
            else -> "${(meters / 1000.0).toInt()} km"
        }
    }

    fun formatRelativeTime(timestamp: Timestamp?): String {
        if (timestamp == null) return "Just now"
        val diffMillis = (System.currentTimeMillis() - timestamp.toDate().time).coerceAtLeast(0L)
        val seconds = diffMillis / 1000L
        return when {
            seconds < 30 -> "Live · Just now"
            seconds < 60 -> "${seconds}s ago"
            seconds < 3600 -> "${seconds / 60}m ago"
            seconds < 86_400 -> "${seconds / 3600}h ago"
            else -> "${seconds / 86_400}d ago"
        }
    }

    fun pairId(a: String, b: String): String =
        if (a < b) "${a}_${b}" else "${b}_${a}"

    fun sortedPair(a: String, b: String): List<String> =
        if (a < b) listOf(a, b) else listOf(b, a)

    fun sanitizeHandle(raw: String): String {
        val cleaned = raw.lowercase(Locale.US)
            .replace(Regex("[^a-z0-9_]"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')
        return when {
            cleaned.length >= 3 -> cleaned.take(24)
            cleaned.isNotEmpty() -> (cleaned + "_usr").take(24)
            else -> "explorer_${(1000..9999).random()}"
        }
    }

    fun generateFriendCode(uid: String): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val seed = uid.hashCode().toLong() and 0xFFFFFFFFL
        val sb = StringBuilder("WLD")
        var v = seed + System.nanoTime()
        repeat(5) {
            v = (v * 6364136223846793005L + 1442695040888963407L)
            val idx = ((v ushr 16) % chars.length).toInt().let { if (it < 0) -it else it }
            sb.append(chars[idx])
        }
        return sb.toString()
    }

    fun parseHexColor(hex: String, fallback: Color = Color(0xFF22D3EE)): Color {
        return try {
            Color(android.graphics.Color.parseColor(hex))
        } catch (_: Exception) {
            fallback
        }
    }

    val AvatarPaletteHex = listOf(
        "#22D3EE",
        "#10B981",
        "#A78BFA",
        "#F59E0B",
        "#F43F5E",
        "#38BDF8"
    )
}
