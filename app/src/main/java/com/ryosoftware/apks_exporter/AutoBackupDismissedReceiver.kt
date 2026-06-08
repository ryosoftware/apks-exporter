package com.ryosoftware.apks_exporter

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ryosoftware.utilities.LogUtilities

class AutoBackupDismissedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        LogUtilities.show(this, "Received event $action")
        if (action == MainBackupWorker.ACTION_NOTIFICATION_DISMISSED) {
            ApplicationPreferences.delete(
                arrayOf(
                    ApplicationPreferences.LAST_BACKED_APP_PACKAGES_KEY,
                    ApplicationPreferences.LAST_BACKUP_ERRORS_COUNT_KEY
                )
            )
        }
    }
}
