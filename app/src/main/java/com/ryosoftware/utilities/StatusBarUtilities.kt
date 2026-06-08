package com.ryosoftware.utilities

import android.annotation.SuppressLint
import android.app.Activity
import android.view.WindowManager

object StatusBarUtilities {
    @SuppressLint("InlinedApi")
    fun setColor(activity: Activity, color: Int) {
        val window = activity.window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.statusBarColor = ColorUtilities.decrease(color, 0x001515)
    }
}
