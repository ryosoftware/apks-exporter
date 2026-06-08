package com.ryosoftware.utilities

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings

object PermissionUtilities {

    fun requestPermissions(activity: Activity, requestedPermissions: Array<String>, requestCode: Int): Boolean {
        val notGranted = requestedPermissions.filter {
            activity.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }
        return if (notGranted.isNotEmpty()) {
            LogUtilities.show(PermissionUtilities::class.java, "Requesting permission for ${notGranted.size} not granted permission/s")
            activity.requestPermissions(notGranted.toTypedArray(), requestCode)
            true
        } else {
            LogUtilities.show(PermissionUtilities::class.java, "All required permissions has been granted")
            false
        }
    }

    fun requestPermission(activity: Activity, requestedPermission: String, requestCode: Int): Boolean {
        return if ((Build.VERSION.SDK_INT >= 26) && (Manifest.permission.INSTALL_PACKAGES == requestedPermission)) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${activity.packageName}"))
            activity.startActivityForResult(intent, requestCode)
            true
        } else {
            requestPermissions(activity, arrayOf(requestedPermission), requestCode)
        }
    }

    fun permissionGranted(context: Context, permission: String): Boolean {
        if (Manifest.permission.INSTALL_PACKAGES == permission) {
            return ((Build.VERSION.SDK_INT < 26) || context.packageManager.canRequestPackageInstalls())
        }
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }
}
