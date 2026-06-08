package com.ryosoftware.apks_exporter

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ryosoftware.utilities.LogUtilities

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        LogUtilities.show(this, "Received event $action")
        if (Intent.ACTION_BOOT_COMPLETED == action) MainBackupWorker.onBootCompleted(context)
    }
}
