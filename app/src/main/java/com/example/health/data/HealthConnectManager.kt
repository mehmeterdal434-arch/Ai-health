package com.example.health.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

enum class HealthConnectAvailability {
    INSTALLED,
    NOT_INSTALLED,
    NOT_SUPPORTED
}

data class HealthPermissionState(
    val stepsGranted: Boolean = true,
    val heartRateGranted: Boolean = true,
    val sleepGranted: Boolean = true,
    val spO2Granted: Boolean = true,
    val caloriesGranted: Boolean = true
) {
    val allGranted: Boolean
        get() = stepsGranted && heartRateGranted && sleepGranted && spO2Granted && caloriesGranted
}

/**
 * Health Connect Client & Intent helper for Samsung Health synchronization.
 */
class HealthConnectManager(private val context: Context) {

    private val healthConnectPackage = "com.google.android.apps.healthdata"
    private val samsungHealthPackage = "com.sec.android.app.shealth"

    fun checkHealthConnectAvailability(): HealthConnectAvailability {
        return try {
            val packageManager = context.packageManager
            packageManager.getPackageInfo(healthConnectPackage, PackageManager.MATCH_UNINSTALLED_PACKAGES)
            HealthConnectAvailability.INSTALLED
        } catch (e: PackageManager.NameNotFoundException) {
            HealthConnectAvailability.NOT_INSTALLED
        } catch (e: Exception) {
            HealthConnectAvailability.NOT_SUPPORTED
        }
    }

    fun isSamsungHealthInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo(samsungHealthPackage, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getPlayStoreIntentForHealthConnect(): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://play.google.com/store/apps/details?id=$healthConnectPackage")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun getPlayStoreIntentForSamsungHealth(): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://play.google.com/store/apps/details?id=$samsungHealthPackage")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun getHealthConnectSettingsIntent(): Intent {
        return Intent("androidx.health.ACTION_HEALTH_CONNECT_SETTINGS").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
