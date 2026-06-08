package com.ryosoftware.utilities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.graphics.drawable.Drawable
import androidx.annotation.RequiresPermission
import androidx.core.net.toUri

object PackageManagerUtilities {

    fun getSignature(context: Context, packageName: String): Array<Signature>? {
        return try {
            context.packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)?.signatures
        } catch (e: Exception) {
            LogUtilities.show(PackageManagerUtilities::class.java, e)
            null
        }
    }

    fun getApplicationInfo(context: Context, packageName: String, flags: Int = 0): ApplicationInfo? {
        return try {
            if (packageName.isEmpty()) throw Exception("No package name specified")
            context.packageManager.getPackageInfo(packageName, flags)?.applicationInfo
        } catch (e: Exception) {
            LogUtilities.show(PackageManagerUtilities::class.java, e)
            null
        }
    }

    fun getApplicationIcon(context: Context, packageName: String): Drawable? {
        return try {
            getApplicationInfo(context, packageName)?.loadIcon(context.packageManager)
        } catch (e: Exception) {
            LogUtilities.show(PackageManagerUtilities::class.java, e)
            null
        }
    }

    fun getNotInstalledApplicationIcon(context: Context, pathname: String): Drawable? {
        return try {
            val packageInfo = context.packageManager.getPackageArchiveInfo(pathname, 0) ?: return null
            val appInfo = packageInfo.applicationInfo ?: return null
            appInfo.sourceDir = pathname
            appInfo.publicSourceDir = pathname
            appInfo.loadIcon(context.packageManager)
        } catch (e: Exception) {
            LogUtilities.show(PackageManagerUtilities::class.java, e)
            null
        }
    }

    fun getApplicationLabel(context: Context, packageName: String, defaultValue: String): String {
        return try {
            val appInfo = getApplicationInfo(context, packageName)
            appInfo?.loadLabel(context.packageManager)?.toString() ?: defaultValue
        } catch (e: Exception) {
            LogUtilities.show(PackageManagerUtilities::class.java, e)
            defaultValue
        }
    }

    @SuppressLint("InlinedApi")
    fun isPackageInstalled(context: Context, packageName: String): Boolean {
        return try {
            val appInfo = getApplicationInfo(context, packageName)
            appInfo != null && (appInfo.flags and ApplicationInfo.FLAG_INSTALLED) == ApplicationInfo.FLAG_INSTALLED
        } catch (e: Exception) {
            LogUtilities.show(PackageManagerUtilities::class.java, e)
            false
        }
    }

    fun uninstallPackage(activity: Activity, packageName: String): Boolean {
        return try {
            activity.startActivity(Intent(Intent.ACTION_DELETE, "package:$packageName".toUri()))
            true
        } catch (e: Exception) {
            LogUtilities.show(PackageManagerUtilities::class.java, e)
            false
        }
    }

    @RequiresPermission(Manifest.permission.KILL_BACKGROUND_PROCESSES)
    fun forceStop(context: Context, packageName: String) {
        try {
            (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).killBackgroundProcesses(packageName)
        } catch (e: Exception) {
            LogUtilities.show(PackageManagerUtilities::class.java, e)
        }
    }

    fun getPackageInstalledLocation(context: Context, packageName: String): String? {
        return try {
            getApplicationInfo(context, packageName)?.sourceDir
        } catch (e: Exception) {
            LogUtilities.show(PackageManagerUtilities::class.java, e)
            null
        }
    }

    fun countPackageInstalledLocations(context: Context, packageName: String): Int {
        return try {
            val appInfo = getApplicationInfo(context, packageName) ?: return 0
            val splits = appInfo.splitSourceDirs
            1 + if (splits.isNullOrEmpty()) 0 else splits.size
        } catch (e: Exception) {
            LogUtilities.show(PackageManagerUtilities::class.java, e)
            0
        }
    }
    fun getPackageInstalledLocations(context: Context, packageName: String): List<String>? {
        return try {
            val appInfo = getApplicationInfo(context, packageName) ?: return null
            val sources = mutableListOf(appInfo.sourceDir)
            val splits = appInfo.splitSourceDirs
            if (!splits.isNullOrEmpty()) sources.addAll(splits)
            sources
        } catch (e: Exception) {
            LogUtilities.show(PackageManagerUtilities::class.java, e)
            null
        }
    }

    fun getPackageDataLocation(context: Context, packageName: String): String? {
        return try {
            getApplicationInfo(context, packageName)?.dataDir
        } catch (e: Exception) {
            LogUtilities.show(PackageManagerUtilities::class.java, e)
            null
        }
    }

    fun isExecutable(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getLaunchIntentForPackage(packageName) != null
        } catch (e: Exception) {
            LogUtilities.show(PackageManagerUtilities::class.java, e)
            false
        }
    }

    fun execute(activity: Activity, packageName: String): Boolean {
        return try {
            val intent = activity.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) { activity.startActivity(intent); true } else false
        } catch (e: Exception) {
            LogUtilities.show(PackageManagerUtilities::class.java, e)
            false
        }
    }
}
