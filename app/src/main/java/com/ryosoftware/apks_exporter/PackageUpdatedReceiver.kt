package com.ryosoftware.apks_exporter

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ryosoftware.utilities.LogUtilities

class PackageUpdatedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        LogUtilities.show(this, "Received event '$action'")
        if (Intent.ACTION_MY_PACKAGE_REPLACED == action) MainBackupWorker.onPackageAddedOrUpdated(context)
    }
}
