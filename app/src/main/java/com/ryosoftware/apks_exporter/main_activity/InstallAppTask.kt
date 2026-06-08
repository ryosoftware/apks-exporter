package com.ryosoftware.apks_exporter.main_activity

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import com.ryosoftware.apks_exporter.InstallCompletedReceiver
import com.ryosoftware.utilities.LogUtilities
import com.ryosoftware.utilities.ZipFileUtilities
import kotlinx.coroutines.*
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

class InstallAppTask(
    private val context: Context,
    private val uri: Uri,
    private val listener: Listener?
) {
    interface Listener {
        fun onInstallAppTaskStarted()
        fun onInstallAppTaskFinished(success: Boolean)
    }

    private var index: Long = 0
    private var job: Job? = null

    private fun getUniqueSessionFilename(): String {
        index++
        return index.toString()
    }

    fun execute() {
        listener?.onInstallAppTaskStarted()
        job = CoroutineScope(Dispatchers.Main).launch {
            delay(100)
            val success = withContext(Dispatchers.IO) {
                doInBackground()
            }
            listener?.onInstallAppTaskFinished(success)
        }
    }

    private fun doInBackground(): Boolean {
        try {
            val uriPath = uri.path
            if (uriPath != null) {
                val lowerPath = uriPath.lowercase()
                if (lowerPath.endsWith(".apk")) {
                    installApp(uri)
                    return true
                } else if (lowerPath.endsWith(".zip") || lowerPath.endsWith(".apkm")) {
                    val externalFilesDir = context.getExternalFilesDir("tmp_install")
                    if (externalFilesDir != null) {
                        val previousInstallTmpFiles = externalFilesDir.listFiles()
                        if (previousInstallTmpFiles != null) {
                            for (file in previousInstallTmpFiles) file.delete()
                        }
                        if (ZipFileUtilities.unpackZip(context, uri, externalFilesDir.path, null)) {
                            val apks = mutableListOf<Any>()
                            val currentInstallTmpFiles = externalFilesDir.listFiles()
                            if (currentInstallTmpFiles != null) {
                                for (file in currentInstallTmpFiles) {
                                    if (file.path.lowercase().endsWith(".apk")) apks.add(file)
                                }
                            }
                            if (apks.isNotEmpty()) {
                                installApp(apks)
                                return true
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            LogUtilities.show(this, e)
        }
        return false
    }

    private fun writeStreamToSession(inputStream: InputStream, session: PackageInstaller.Session) {
        session.openWrite(getUniqueSessionFilename(), 0, -1).use { outputStream ->
            val buffer = ByteArray(65536)
            var count: Int
            while (inputStream.read(buffer).also { count = it } != -1) {
                outputStream.write(buffer, 0, count)
            }
            session.fsync(outputStream)
        }
    }

    private fun addApkToSession(session: PackageInstaller.Session, apkUri: Uri) {
        context.contentResolver.openInputStream(apkUri)?.use { inputStream ->
            writeStreamToSession(inputStream, session)
        }
    }

    private fun addApkToSession(session: PackageInstaller.Session, file: File) {
        FileInputStream(file).use { inputStream ->
            writeStreamToSession(inputStream, session)
        }
    }

    @SuppressLint("RequestInstallPackagesPolicy")
    private fun installApp(apkReferences: List<Any>) {
        val packageInstaller = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        val sessionId = packageInstaller.createSession(params)
        var session: PackageInstaller.Session? = null
        try {
            session = packageInstaller.openSession(sessionId)
            for (apkReference in apkReferences) {
                when (apkReference) {
                    is File -> addApkToSession(session, apkReference)
                    is Uri -> addApkToSession(session, apkReference)
                }
            }
            val intent = Intent(context, InstallCompletedReceiver::class.java).apply {
                action = InstallCompletedReceiver.ACTION_INSTALL_COMPLETED
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                INSTALL_COMPLETED_PENDING_INTENT,
                intent,
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            session.commit(pendingIntent.intentSender)
        } catch (e: Exception) {
            session?.abandon()
            throw e
        } finally {
            session?.close()
        }
    }

    private fun installApp(apk: Uri) {
        installApp(listOf(apk))
    }

    companion object {
        private const val INSTALL_COMPLETED_PENDING_INTENT = 1001
    }
}
