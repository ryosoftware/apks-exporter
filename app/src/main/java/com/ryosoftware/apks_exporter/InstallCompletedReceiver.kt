package com.ryosoftware.apks_exporter

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.widget.Toast
import com.ryosoftware.utilities.LogUtilities

class InstallCompletedReceiver : BroadcastReceiver() {

    @SuppressLint("UnsafeIntentLaunch")
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        LogUtilities.show(this, "Received event $action")

        if (ACTION_INSTALL_COMPLETED == action) {
            val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
            when (status) {
                PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                    val component = intent.component
                    if (component != null && context.packageName == component.packageName) {
                        val confirmationIntent = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                        if (confirmationIntent != null) {
                            val packageName = confirmationIntent.`package`
                            val confirmAction = confirmationIntent.action
                            if (INSTALLER_ALLOWED_PACKAGES.contains(packageName) &&
                                ACTION_PACKAGE_MANAGER_CONFIRM_INSTALL == confirmAction
                            ) {
                                confirmationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(confirmationIntent)
                        }
                    }
                }
                PackageInstaller.STATUS_SUCCESS -> {
                    LogUtilities.show(this, "Install successfully")
                    Toast.makeText(context, context.getString(R.string.install_success), Toast.LENGTH_SHORT).show()
                }
                PackageInstaller.STATUS_FAILURE,
                PackageInstaller.STATUS_FAILURE_ABORTED,
                PackageInstaller.STATUS_FAILURE_INVALID -> {
                    LogUtilities.show(this, "Install Completed status failure: $status (${intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)})")
                }
            }
        }
    }

    companion object {
        val ACTION_INSTALL_COMPLETED: String = BuildConfig.APPLICATION_ID + ".INSTALL_COMPLETED"
        private val INSTALLER_ALLOWED_PACKAGES = listOf("com.android.packageinstaller", "com.google.android.packageinstaller")
        private const val ACTION_PACKAGE_MANAGER_CONFIRM_INSTALL = "android.content.pm.action.CONFIRM_INSTALL"
    }
}
