package com.ryosoftware.apks_exporter

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.text.format.DateUtils
import androidx.documentfile.provider.DocumentFile
import androidx.work.*
import com.ryosoftware.utilities.*
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.net.toUri

@Singleton
class MainService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        lateinit var instance: MainService

        private const val APK_FILE_MIME_TYPE = "application/vnd.android.package-archive"
        private const val ZIP_FILE_MIME_TYPE = "ZIP_FILE_MIME_TYPE"
        private const val LAST_BACKUP_VERSION_NUMBER_SUFFIX = "last-backup-version-number"
        private const val LAST_APP_VERSION_NUMBER_SUFFIX = "last-version-number"
        private const val LAST_APP_UPDATE_TIME_SUFFIX = "last-update-time"
        private const val LAST_APP_UPDATE_TIMES_NOTIFIED_SUFFIX = "last-update-notified-times"
        private const val LAST_BACKUP_TIME_SUFFIX = "last-backup-time"
        private const val CAN_AUTOMATICALLY_BACKUP_SUFFIX = "can-automatically-backup"
        val APP_SUFFIXES = listOf(
            LAST_BACKUP_VERSION_NUMBER_SUFFIX,
            LAST_APP_VERSION_NUMBER_SUFFIX,
            LAST_APP_UPDATE_TIME_SUFFIX,
            LAST_APP_UPDATE_TIMES_NOTIFIED_SUFFIX,
            LAST_BACKUP_TIME_SUFFIX,
            CAN_AUTOMATICALLY_BACKUP_SUFFIX
        )
        private const val NOTIFY_UPDATED_INTERVAL = DateUtils.DAY_IN_MILLIS * 3
        private const val NOTIFY_UPDATED_TIMES = 3
        private const val BASE_APK_FILENAME = "base.apk"
        const val REMOVABLE_SPLIT_PREFIX = "split_"

        fun isSystemApplication(packageInfo: PackageInfo): Boolean = instance.isSystemApplication(packageInfo)
        fun isSystemApplicationUpdated(packageInfo: PackageInfo): Boolean = instance.isSystemApplicationUpdated(packageInfo)
        fun isAppUpdated(packageInfo: PackageInfo): Boolean = instance.isAppUpdated(packageInfo)
        fun isAppBacked(packageInfo: PackageInfo): Boolean = instance.isAppBacked(packageInfo)
        fun setBackupDone(packageInfo: PackageInfo) = instance.setBackupDone(packageInfo)
        fun canAutomaticallyBackup(packageInfo: PackageInfo): Boolean = instance.canAutomaticallyBackup(packageInfo)
        fun canAutomaticallyBackup(packageInfo: PackageInfo, value: Boolean) = instance.canAutomaticallyBackup(packageInfo, value)
        fun hasAutoBackupPreference(packageInfo: PackageInfo): Boolean = instance.hasAutoBackupPreference(packageInfo)
        fun resolveAutoBackupPreference(packageInfo: PackageInfo, autoEnableBackupForNewApps: Boolean): Boolean =
            instance.resolveAutoBackupPreference(packageInfo, autoEnableBackupForNewApps)
        fun incrementAppUpdateNotificationCount(packageName: String) = instance.incrementAppUpdateNotificationCount(packageName)
        fun doBackup(context: Context, packageInfo: PackageInfo): Boolean = instance.doBackup(context, packageInfo)
        fun getLastBackupVersionNumberKey(packageName: String): String = instance.getLastBackupVersionNumberKey(packageName)
        fun getLastBackupTimeKey(packageName: String): String = instance.getLastBackupTimeKey(packageName)
        fun getCanAutomaticallyBackupAppKey(packageName: String): String = instance.getCanAutomaticallyBackupAppKey(packageName)
        fun getLastAppVersionNumberKey(packageName: String): String = instance.getLastAppVersionNumberKey(packageName)
        fun getLastAppUpdateTimeKey(packageName: String): String = instance.getLastAppUpdateTimeKey(packageName)
        fun getLastAppUpdateNotificationTimesKey(packageName: String): String = instance.getLastAppUpdateNotificationTimesKey(packageName)
    }

    init {
        instance = this
    }
    fun isSystemApplication(packageInfo: PackageInfo): Boolean =
        (packageInfo.applicationInfo != null) &&
                ((packageInfo.applicationInfo!!.flags and ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM)

    fun isSystemApplicationUpdated(packageInfo: PackageInfo): Boolean =
        (packageInfo.applicationInfo != null) &&
                ((packageInfo.applicationInfo!!.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)

    fun setBackupDone(packageInfo: PackageInfo) {
        ApplicationPreferences.put(
            getLastBackupVersionNumberKey(packageInfo.packageName),
            packageInfo.longVersionCode
        )
        ApplicationPreferences.put(
            getLastBackupTimeKey(packageInfo.packageName),
            System.currentTimeMillis()
        )
    }

    fun isAppBacked(packageInfo: PackageInfo) : Boolean {
        return ApplicationPreferences.get(getLastBackupVersionNumberKey(packageInfo.packageName), 0L) == packageInfo.longVersionCode
    }

    private fun setAppUpdated(packageInfo: PackageInfo) {
        ApplicationPreferences.put(
            getLastAppVersionNumberKey(packageInfo.packageName),
            packageInfo.longVersionCode
        )
        ApplicationPreferences.put(
            getLastAppUpdateTimeKey(packageInfo.packageName),
            System.currentTimeMillis()
        )
        ApplicationPreferences.put(
            getLastAppUpdateNotificationTimesKey(packageInfo.packageName),
            0
        )
    }
    fun isAppUpdated(packageInfo: PackageInfo): Boolean {
        if (ApplicationPreferences.get(getLastAppVersionNumberKey(packageInfo.packageName), 0L) < packageInfo.longVersionCode) {
            setAppUpdated(packageInfo)
        }

        val lastAppUpdateTimeKey = getLastAppUpdateTimeKey(packageInfo.packageName)
        val appUpdatedNotifiedTimesKey = getLastAppUpdateNotificationTimesKey(packageInfo.packageName)

        val timeRemaining = ApplicationPreferences.get(lastAppUpdateTimeKey, 0) + NOTIFY_UPDATED_INTERVAL > System.currentTimeMillis()
        val viewsRemaining = ApplicationPreferences.get(appUpdatedNotifiedTimesKey, 0) < NOTIFY_UPDATED_TIMES

        return timeRemaining || viewsRemaining
    }

    fun incrementAppUpdateNotificationCount(packageName: String) {
        val key = getLastAppUpdateNotificationTimesKey(packageName)
        val currentCount = ApplicationPreferences.get(key, 0)
        ApplicationPreferences.put(key, currentCount + 1)
    }

    fun canAutomaticallyBackup(packageInfo: PackageInfo): Boolean =
        ApplicationPreferences.get(
            getCanAutomaticallyBackupAppKey(packageInfo.packageName),
            true
        )

    fun canAutomaticallyBackup(packageInfo: PackageInfo, value: Boolean) {
        ApplicationPreferences.put(
            getCanAutomaticallyBackupAppKey(packageInfo.packageName),
            value
        )
    }

    fun hasAutoBackupPreference(packageInfo: PackageInfo): Boolean =
        ApplicationPreferences.containsKey(getCanAutomaticallyBackupAppKey(packageInfo.packageName))

    fun resolveAutoBackupPreference(packageInfo: PackageInfo, autoEnableBackupForNewApps: Boolean): Boolean {
        var canAutoBackup = canAutomaticallyBackup(packageInfo)
        if (!hasAutoBackupPreference(packageInfo)) {
            canAutoBackup = (!isSystemApplication(packageInfo)) && autoEnableBackupForNewApps
            canAutomaticallyBackup(packageInfo, canAutoBackup)
        }
        return canAutoBackup
    }

    private fun getApks(context: Context, packageName: String): Map<String, File>? {
        val apks = PackageManagerUtilities.getPackageInstalledLocations(context, packageName)
        if (!apks.isNullOrEmpty()) {
            val files = HashMap<String, File>()
            for (apk in apks) {
                val apkFile = File(apk)
                var apkName = apkFile.name
                if (apkName.startsWith(REMOVABLE_SPLIT_PREFIX)) {
                    apkName = apkName.substring(REMOVABLE_SPLIT_PREFIX.length)
                }
                files[apkName] = apkFile
            }
            return files
        }
        return null
    }

    private fun deleteFiles(folder: DocumentFile) {
        try {
            for (file in folder.listFiles()) file.delete()
        } catch (e: Exception) {
            LogUtilities.show(MainService::class.java, e)
        }
    }

    private fun compressFiles(
        context: Context,
        destinationFile: DocumentFile,
        apkFiles: Map<String, File>
    ): Boolean {
        try {
            context.contentResolver.openOutputStream(destinationFile.uri)?.use { outputStream ->
                return ZipFileUtilities.createZip(outputStream, apkFiles, false)
            }
        } catch (e: Exception) {
            LogUtilities.show(MainService::class.java, e)
        }
        return false
    }

    private fun saveFiles(
        context: Context,
        destinationFolder: DocumentFile,
        apkFiles: Map<String, File>
    ): Boolean {
        try {
            val createdFiles = ArrayList<DocumentFile>()
            var success = true
            for ((fileName, file) in apkFiles) {
                val documentFile = destinationFolder.createFile(APK_FILE_MIME_TYPE, fileName)
                if (!FileUtilities.copyFile(context, file, documentFile!!)) {
                    success = false
                    break
                }
                createdFiles.add(documentFile)
            }
            if (!success) {
                for (file in createdFiles) file.delete()
            }
            return success
        } catch (e: Exception) {
            LogUtilities.show(MainService::class.java, e)
            return false
        }
    }

    fun doBackup(context: Context, packageInfo: PackageInfo): Boolean {
        try {
            val backupsFolder = ApplicationPreferences.get<String?>(ApplicationPreferences.SAVE_FOLDER_KEY, null)
                ?: return false
            val backupsFolderDocument = DocumentFile.fromTreeUri(context, backupsFolder.toUri())
                ?: return false
            if (!backupsFolderDocument.canWrite()) return false

            val saveAsZipFile = ApplicationPreferences.get(
                ApplicationPreferences.SAVE_AS_ZIP_FILE_KEY,
                ApplicationPreferences.SAVE_AS_ZIP_FILE_DEFAULT
            )
            val doNotCompressSingleFiles = ApplicationPreferences.get(
                ApplicationPreferences.DO_NOT_COMPRESS_SINGLE_FILES_KEY,
                ApplicationPreferences.DO_NOT_COMPRESS_SINGLE_FILES_DEFAULT
            )
            val useApkmExtension = ApplicationPreferences.get(
                ApplicationPreferences.APKM_EXTENSION_KEY,
                ApplicationPreferences.APKM_EXTENSION_DEFAULT
            )
            val useVersionLabel = ApplicationPreferences.get(
                ApplicationPreferences.USE_APP_LABEL_KEY,
                ApplicationPreferences.USE_APP_LABEL_DEFAULT
            )
            val useLongVersionNumber = ApplicationPreferences.get(
                ApplicationPreferences.USE_LONG_VERSION_NUMBER_KEY,
                ApplicationPreferences.USE_LONG_VERSION_NUMBER_DEFAULT
            )

            val appVersionNumber = if (useLongVersionNumber) {
                packageInfo.versionName ?: packageInfo.longVersionCode.toString()
            } else {
                packageInfo.longVersionCode.toString()
            }
            val appName = if (useVersionLabel) {
                PackageManagerUtilities.getApplicationLabel(
                    context,
                    packageInfo.packageName,
                    packageInfo.packageName
                ).replace("\\s".toRegex(), "")
            } else {
                packageInfo.packageName
            }

            val apkFiles = getApks(context, packageInfo.packageName)
                ?: return false

            if (saveAsZipFile) {
                var fileName = "$appName-$appVersionNumber"
                var appFileDocument: DocumentFile?
                val success: Boolean
                if (apkFiles.size == 1 && doNotCompressSingleFiles) {
                    fileName = "$fileName.apk"
                    appFileDocument = backupsFolderDocument.findFile(fileName)
                    if (appFileDocument == null) {
                        appFileDocument = backupsFolderDocument.createFile(APK_FILE_MIME_TYPE, fileName)
                    }
                    success = FileUtilities.copyFile(
                        context,
                        apkFiles.values.iterator().next(),
                        appFileDocument!!
                    )
                } else {
                    val extension = if (useApkmExtension) ".apkm" else ".zip"
                    fileName = "$fileName$extension"
                    appFileDocument = backupsFolderDocument.findFile(fileName)
                    if (appFileDocument == null) {
                        appFileDocument = backupsFolderDocument.createFile(ZIP_FILE_MIME_TYPE, fileName)
                    }
                    success = compressFiles(context, appFileDocument!!, apkFiles)
                }
                if (!success) {
                    appFileDocument.delete()
                    return false
                }
            } else {
                var appFolderDocument = backupsFolderDocument.findFile(appName)
                if (appFolderDocument == null) {
                    appFolderDocument = backupsFolderDocument.createDirectory(appName)
                }
                var appVersionDocument = appFolderDocument?.findFile(appVersionNumber)
                if (appVersionDocument == null) {
                    appVersionDocument = appFolderDocument?.createDirectory(appVersionNumber)
                }
                if (appVersionDocument != null) {
                    deleteFiles(appVersionDocument)
                    val success = saveFiles(context, appVersionDocument, apkFiles)
                    if (!success) {
                        deleteFiles(appVersionDocument)
                        appVersionDocument.delete()
                        val files = appFolderDocument!!.listFiles()
                        if (files.isEmpty()) appFolderDocument.delete()
                        return false
                    }
                } else {
                    return false
                }
            }
            setBackupDone(packageInfo)
            setAppUpdated(packageInfo)
            return true
        } catch (e: Exception) {
            LogUtilities.show(MainService::class.java, e)
            return false
        }
    }

    fun getLastBackupVersionNumberKey(packageName: String) : String {
        return "${packageName}-$LAST_BACKUP_VERSION_NUMBER_SUFFIX"
    }

    fun getLastBackupTimeKey(packageName: String) : String {
        return "${packageName}-$LAST_BACKUP_TIME_SUFFIX"
    }
    fun getCanAutomaticallyBackupAppKey(packageName: String) : String {
        return "$packageName-$CAN_AUTOMATICALLY_BACKUP_SUFFIX"
    }

    fun getLastAppVersionNumberKey(packageName: String) : String {
        return "${packageName}-$LAST_APP_VERSION_NUMBER_SUFFIX"
    }
    fun getLastAppUpdateTimeKey(packageName: String) : String {
        return "$packageName-$LAST_APP_UPDATE_TIME_SUFFIX"
    }

    fun getLastAppUpdateNotificationTimesKey(packageName: String) : String {
        return "$packageName-$LAST_APP_UPDATE_TIMES_NOTIFIED_SUFFIX"
    }
}
