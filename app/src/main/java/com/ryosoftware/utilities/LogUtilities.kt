package com.ryosoftware.utilities

import android.annotation.SuppressLint
import android.util.Log
import java.io.BufferedWriter
import java.io.FileWriter

object LogUtilities {

    @SuppressLint("SdCardPath")
    private val LOG_FILE: String? = null

    const val DEBUG_NONE = 0
    const val DEBUG_ERRORS = 1
    const val DEBUG_INFO = 2
    const val DEBUG_ALL = 99

    var tag: String = ""

    var logMode: Int = DEBUG_NONE

    private fun minimizeClassName(className: String): String {
        val index = className.lastIndexOf('.')
        return if (index < 0) className else className.substring(index + 1)
    }

    fun show(caller: Any, explanation: String?, e: Throwable) {
        if (logMode >= DEBUG_ERRORS) {
            val callerTag = minimizeClassName(caller.javaClass.name)
            if (explanation != null) Log.e(tag, "$callerTag: $explanation")
            Log.e(tag, "$callerTag: ${e.message}")
            e.printStackTrace()
        }
    }

    fun show(caller: Class<*>, explanation: String?, e: Throwable) {
        if (logMode >= DEBUG_ERRORS) {
            val callerTag = minimizeClassName(caller.name)
            if (explanation != null) Log.e(tag, "$callerTag: $explanation")
            Log.e(tag, "$callerTag: ${e.message}")
            e.printStackTrace()
        }
    }

    fun show(caller: Any, e: Throwable) = show(caller, null, e)
    fun show(caller: Class<*>, e: Throwable) = show(caller, null, e)

    fun show(caller: Any, description: String) {
        if (logMode >= DEBUG_INFO) {
            Log.d(tag, "${minimizeClassName(caller.javaClass.name)}: $description")
        }
    }

    fun show(caller: Class<*>, description: String) {
        if (logMode >= DEBUG_INFO) {
            Log.d(tag, "${minimizeClassName(caller.name)}: $description")
        }
    }
}
