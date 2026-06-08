package com.ryosoftware.utilities

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object FileUtilities {

    fun copyFile(source: File, destination: File): Boolean {
        return try {
            FileInputStream(source).use { input ->
                FileOutputStream(destination).use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } > 0) output.write(buffer, 0, read)
                }
            }
            true
        } catch (e: Exception) {
            LogUtilities.show(FileUtilities::class.java, e)
            false
        }
    }

    fun copyFile(context: Context, source: File, destination: DocumentFile): Boolean {
        return try {
            FileInputStream(source).use { input ->
                context.contentResolver.openOutputStream(destination.uri)!!.use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } > 0) output.write(buffer, 0, read)
                }
            }
            true
        } catch (e: Exception) {
            LogUtilities.show(FileUtilities::class.java, e)
            false
        }
    }

    fun getFileName(pathname: String): String {
        val index = pathname.lastIndexOf('/')
        return if (index < 0) pathname else pathname.substring(index + 1)
    }

    fun getFileExtension(pathname: String): String? {
        val index = pathname.lastIndexOf('.')
        return if (index < 0) null else pathname.substring(index + 1)
    }

    fun extractFileExtension(pathname: String): String {
        val index = pathname.lastIndexOf('.')
        return if (index < 0) pathname else pathname.substring(0, index)
    }
}
