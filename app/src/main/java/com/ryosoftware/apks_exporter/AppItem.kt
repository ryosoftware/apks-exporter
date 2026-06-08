package com.ryosoftware.apks_exporter

import android.app.Activity
import android.app.usage.StorageStats
import android.content.Intent
import android.content.pm.PackageInfo
import android.graphics.drawable.Drawable
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import com.ryosoftware.utilities.LogUtilities
import com.ryosoftware.utilities.PackageManagerUtilities
import androidx.core.net.toUri
import kotlinx.coroutines.flow.Flow

data class AppItem(
    val packageInfo: PackageInfo,
    val stats: StorageStats?,
    val icon: Drawable?,
    val appLabel: String
) {
    val packageName: String get() = packageInfo.packageName
    val versionName: String? get() = packageInfo.versionName
    val versionCode: Long get() = packageInfo.longVersionCode

    val isDisabled: Boolean get() = !(packageInfo.applicationInfo?.enabled ?: false)
    val isSystemApp: Boolean get() = MainService.isSystemApplication(packageInfo)
    val isUpdatedSystemApp: Boolean get() = MainService.isSystemApplicationUpdated(packageInfo)
    val isAppUpdated: Boolean get() = MainService.isAppUpdated(packageInfo)
    val isAppBacked: Boolean get() = MainService.isAppBacked(packageInfo)
    val apkCount: Int get() {
        val splits = packageInfo.applicationInfo?.splitSourceDirs
        return 1 + if (splits.isNullOrEmpty()) 0 else splits.size + 0
    }
    val apkSize: Long get() {
        val applicationInfo = packageInfo.applicationInfo ?: return 0
        var total = File(applicationInfo.sourceDir).length()
        if (applicationInfo.splitSourceDirs != null) {
            for (split in applicationInfo.splitSourceDirs) {
                total += File(split).length()
            }
        }
        return total
    }

    val apkDataSize: Long get() {
        return stats?.dataBytes ?: -1L
    }

    val apkCacheSize: Long get() {
        return stats?.cacheBytes ?: -1L
    }

    fun canAutomaticallyBackup(): Boolean = MainService.canAutomaticallyBackup(packageInfo)
    fun canAutomaticallyBackup(value: Boolean) = MainService.canAutomaticallyBackup(packageInfo, value)
    fun observeCanAutomaticallyBackup(): Flow<Boolean> = ApplicationPreferences.observe(
        MainService.getCanAutomaticallyBackupAppKey(packageName), true
    )
    fun setBackupDone() = MainService.setBackupDone(packageInfo)

    fun uninstallApp(activity: Activity) {
        try {
            PackageManagerUtilities.uninstallPackage(activity, packageName)
        } catch (e: Exception) {
            LogUtilities.show(this, e)
            Toast.makeText(activity, R.string.cant_complete_requested_operation, Toast.LENGTH_SHORT).show()
        }
    }

    fun showAppInfo(activity: Activity) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData("package:$packageName".toUri())
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            activity.startActivity(intent)
        } catch (e: Exception) {
            LogUtilities.show(this, e)
            Toast.makeText(activity, R.string.cant_complete_requested_operation, Toast.LENGTH_SHORT).show()
        }
    }

    fun openApp(activity: Activity) {
        try {
            activity.packageManager.getLaunchIntentForPackage(packageName)?.let {
                it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                activity.startActivity(it)
            }
        } catch (e: Exception) {
            LogUtilities.show(this, e)
            Toast.makeText(activity, R.string.cant_complete_requested_operation, Toast.LENGTH_SHORT).show()
        }
    }

    fun shareApk(activity: Activity) {
        try {
            val ai = packageInfo.applicationInfo ?: return
            val src = File(ai.sourceDir)
            val destDir = File(activity.cacheDir, "share").also { it.mkdirs() }
            val dest = File(destDir, appLabel.replace(" ", "_") + ".apk")
            src.copyTo(dest, overwrite = true)
            val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.files-provider", dest)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.android.package-archive"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            activity.startActivity(Intent.createChooser(intent, null))
        } catch (e: Exception) {
            LogUtilities.show(this, e)
            Toast.makeText(activity, R.string.cant_complete_requested_operation, Toast.LENGTH_SHORT).show()
        }
    }
}
