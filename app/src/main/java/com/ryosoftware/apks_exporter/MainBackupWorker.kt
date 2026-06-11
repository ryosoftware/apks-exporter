package com.ryosoftware.apks_exporter

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.ServiceInfo
import android.text.format.DateUtils
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.ryosoftware.utilities.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.json.JSONObject
import java.util.concurrent.TimeUnit

@HiltWorker
class MainBackupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val applicationPreferences: ApplicationPreferences,
    private val mainService: MainService
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        setForeground(
            ForegroundInfo(
                SERVICE_NOTIFICATION_ID,
                createForegroundNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        )
        doAutoBackupApps()
        return Result.success()
    }

    private fun createForegroundNotification(): Notification {
        val notification = NotificationCompat.Builder(applicationContext, Main.BACKGROUND_TASKS_NOTIFICATION_CHANNEL)
        val body = applicationContext.getString(R.string.checking_if_recently_installed_or_updated_apps)
        notification.setContentTitle(applicationContext.getString(R.string.app_name))
        notification.setContentText(body)
        notification.setStyle(NotificationCompat.BigTextStyle().bigText(body))
        notification.setSmallIcon(R.drawable.ic_stat_notification_main_service)
        notification.setWhen(System.currentTimeMillis())
        notification.setShowWhen(true)
        notification.setContentIntent(
            PendingIntent.getActivity(
                applicationContext,
                SERVICE_NOTIFICATION_ID,
                Intent(applicationContext, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        notification.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        notification.setPriority(NotificationCompat.PRIORITY_DEFAULT)
        return notification.build()
    }

    private fun getAppsThatNeedsToBeBacked(): List<PackageInfo>? {
        val allPackages = applicationContext.packageManager.getInstalledPackages(0)
        val autoEnableBackupForNewApps = ApplicationPreferences.get(
            ApplicationPreferences.AUTO_BACKUP_NEW_APPS_KEY,
            ApplicationPreferences.AUTO_BACKUP_NEW_APPS_DEFAULT
        )
        val showDisabledApps = ApplicationPreferences.get(
            ApplicationPreferences.SHOW_DISABLED_APPS_KEY,
            ApplicationPreferences.SHOW_DISABLED_APPS_DEFAULT
        )
        val appsToBeBacked = mutableListOf<PackageInfo>()
        for (packageInfo in allPackages) {
            if (packageInfo.applicationInfo?.packageName == null) continue
            val canAutoBackup = MainService.resolveAutoBackupPreference(packageInfo, autoEnableBackupForNewApps)
            if (!canAutoBackup && !showDisabledApps && (packageInfo.applicationInfo?.enabled == false)) continue
            if (!canAutoBackup) continue
            if (MainService.isAppBacked(packageInfo)) continue
            appsToBeBacked.add(packageInfo)
        }
        return appsToBeBacked.ifEmpty { null }
    }

    private fun getBackedAppsNotificationBody(
        backedApps: Map<String, String>,
        crash: Boolean,
        errors: Int): String {
        val backedAppsCount = backedApps.size ?: 0

        if (crash) {
            return applicationContext.getString(R.string.error_performing_auto_apps_backup)
        }
        else if (backedAppsCount == 0) {
            return applicationContext.getString(R.string.auto_apps_backup_none)
        }
        return applicationContext.resources.getQuantityString(
            if (errors == 0) R.plurals.auto_apps_backup_ended_without_error
            else R.plurals.auto_apps_backup_ended_with_error,
            backedAppsCount,
            backedAppsCount,
            StringUtilities.join(backedApps.values.toList(), applicationContext.getString(R.string.strings_middle_separator), applicationContext.getString(R.string.strings_and_separator)),
            errors
        )
    }

    private fun getBackedAppsNotification(
        body: String,
        backedAppsCount: Int,
        crash: Boolean,
    ): Notification {
        val channelId: String = if (crash) {
            Main.AUTO_CREATED_BACKUPS_ERROR_NOTIFICATION_CHANNEL
        } else if (backedAppsCount == 0) {
            Main.AUTO_CREATED_BACKUPS_NONE_NOTIFICATION_CHANNEL
        } else {
            Main.AUTO_CREATED_BACKUPS_NOTIFICATION_CHANNEL
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
        notification.setContentTitle(applicationContext.getString(R.string.app_name))
        notification.setContentText(body)
        notification.setStyle(NotificationCompat.BigTextStyle().bigText(body))
        notification.setSmallIcon(R.drawable.ic_stat_notification_auto_created_backups)
        notification.setWhen(System.currentTimeMillis())
        notification.setShowWhen(true)
        notification.setContentIntent(
            PendingIntent.getActivity(
                applicationContext,
                BACKED_APPS_NOTIFICATION_ID,
                Intent(applicationContext, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        notification.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        notification.setPriority(NotificationCompat.PRIORITY_DEFAULT)
        if (!crash) {
            notification.setDeleteIntent(
                PendingIntent.getBroadcast(
                    applicationContext,
                    0,
                    Intent(applicationContext, AutoBackupDismissedReceiver::class.java).apply {
                        action = ACTION_NOTIFICATION_DISMISSED
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        }
        return notification.build()
    }

    fun persistAutoBackupAppsData(backedApps: Map<String, String>, errors: Int) {
        ApplicationPreferences.put(ApplicationPreferences.LAST_BACKED_APP_PACKAGES_KEY, JSONObject(backedApps).toString())
        ApplicationPreferences.put(ApplicationPreferences.LAST_BACKUP_ERRORS_COUNT_KEY, errors)
    }

    private fun getPersistedAutoBackupAppsData(): Pair<MutableMap<String, String>, Int> {
        val json = ApplicationPreferences.get(ApplicationPreferences.LAST_BACKED_APP_PACKAGES_KEY, "")
        val errors = ApplicationPreferences.get(ApplicationPreferences.LAST_BACKUP_ERRORS_COUNT_KEY, 0)
        val backedApps = if (json.isNotEmpty()) {
            val jsonObject = JSONObject(json)
            jsonObject.keys().asSequence().associateWith { jsonObject.getString(it) }.toMutableMap()
        } else {
            mutableMapOf()
        }
        return backedApps to errors
    }

    @SuppressLint("MissingPermission")
    private fun doAutoBackupApps() {
        if (!ApplicationPreferences.get(
                ApplicationPreferences.AUTO_BACKUP_APPS_KEY,
                ApplicationPreferences.AUTO_BACKUP_APPS_DEFAULT)) return

        var crash = false
        var (backedApps, errors) = getPersistedAutoBackupAppsData()
        val appsToBeBacked = getAppsThatNeedsToBeBacked()
        try {
            if (appsToBeBacked != null) {
                for (packageInfo in appsToBeBacked) {
                    if (!MainService.doBackup(applicationContext, packageInfo)) {
                        errors ++
                        continue
                    }
                    backedApps[packageInfo.packageName] =
                        PackageManagerUtilities.getApplicationLabel(
                            applicationContext,
                            packageInfo.packageName,
                            packageInfo.packageName
                        )
                }
            }
        } catch (e: Exception) {
            LogUtilities.show(this@MainBackupWorker, e)
            crash = true
        }

        val now = System.currentTimeMillis()
        ApplicationPreferences.put(ApplicationPreferences.LAST_AUTO_BACKUP_TIME_KEY, now)
        persistAutoBackupAppsData(backedApps, errors)

        val autoBackupResultString = getBackedAppsNotificationBody(backedApps, crash, errors)
        ApplicationPreferences.put(ApplicationPreferences.LAST_BACKUP_RESULTS_KEY, autoBackupResultString)

        val notificationWillBePosted = crash || (!appsToBeBacked.isNullOrEmpty()) || backedApps.isEmpty()
        if (notificationWillBePosted && PermissionUtilities.permissionGranted(applicationContext, Manifest.permission.POST_NOTIFICATIONS)) {
            val backedAppsCount = backedApps.size ?: 0
            val notification = getBackedAppsNotification(autoBackupResultString, backedAppsCount, crash)
            val notificationId = if (crash) BACKED_APPS_ERROR_NOTIFICATION_ID else BACKED_APPS_NOTIFICATION_ID
            NotificationManagerCompat.from(applicationContext).notify(notificationId, notification)
        }

        val intent = Intent(ACTION_AUTO_BACKUP_APPS_DONE)
        intent.putExtra(EXTRA_CRASH, crash)
        intent.putExtra(EXTRA_BACKED_APPS_COUNT, backedApps.size)
        intent.putExtra(EXTRA_ERRORS_COUNT, errors)
        applicationContext.sendBroadcast(intent)

        setNextExecutionTime(applicationContext, false, now)
    }

    companion object {
        private const val SERVICE_NOTIFICATION_ID = 1001
        private const val BACKED_APPS_NOTIFICATION_ID = SERVICE_NOTIFICATION_ID + 1
        private const val BACKED_APPS_ERROR_NOTIFICATION_ID = BACKED_APPS_NOTIFICATION_ID + 1

        const val AUTO_BACKUP_APPS_WORKER_TAG = "auto-backup-apps"

        const val ACTION_NOTIFICATION_DISMISSED = BuildConfig.APPLICATION_ID + ".NOTIFICATION_DISMISSED";
        const val ACTION_AUTO_BACKUP_APPS_DONE = BuildConfig.APPLICATION_ID + ".AUTO_BACKUPS_DONE"
        const val EXTRA_CRASH = "crash"
        const val EXTRA_BACKED_APPS_COUNT = "backedCount"
        const val EXTRA_ERRORS_COUNT = "errorsCount"

        private const val INTERVAL_BEFORE_AUTO_BACKUP_APPS = 4 * DateUtils.HOUR_IN_MILLIS
        @SuppressLint("DefaultLocale")
        private fun setNextExecutionTime(context: Context, force: Boolean, lastBackupTime: Long) {
            if (!ApplicationPreferences.get(
                    ApplicationPreferences.AUTO_BACKUP_APPS_KEY,
                    ApplicationPreferences.AUTO_BACKUP_APPS_DEFAULT)) return

            val lastAutoBackupTime = lastBackupTime.takeIf { it > 0L }
                ?: ApplicationPreferences.get(ApplicationPreferences.LAST_AUTO_BACKUP_TIME_KEY, 0L)
            val now = System.currentTimeMillis()
            val elapsed = now - lastAutoBackupTime
            val nextExecutionTime = if ((elapsed >= INTERVAL_BEFORE_AUTO_BACKUP_APPS) || force) {
                0
            } else {
                INTERVAL_BEFORE_AUTO_BACKUP_APPS - elapsed
            }

            val request = OneTimeWorkRequestBuilder<MainBackupWorker>()
                .setInitialDelay(nextExecutionTime, TimeUnit.MILLISECONDS)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .addTag(AUTO_BACKUP_APPS_WORKER_TAG)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                AUTO_BACKUP_APPS_WORKER_TAG,
                ExistingWorkPolicy.REPLACE,
                request
            )

            ApplicationPreferences.put(ApplicationPreferences.NEXT_AUTO_BACKUP_TIME_KEY, now + nextExecutionTime)

            LogUtilities.show(MainBackupWorker::class.java.name, String.format("Auto Backup Execution will be executed into %d seconds", nextExecutionTime / 1000))
        }

        fun onBootCompleted(context: Context) = setNextExecutionTime(context, true, 0L)

        fun onPackageAddedOrUpdated(context: Context) = setNextExecutionTime(context, false, 0L)

        fun onAppExecuted(context: Context) = setNextExecutionTime(context, false, 0L)

        fun onBackupForced(context: Context) = setNextExecutionTime(context, true, 0L)
    }
}
