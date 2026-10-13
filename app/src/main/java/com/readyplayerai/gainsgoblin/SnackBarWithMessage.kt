package com.readyplayerai.gainsgoblin

import android.app.Activity
import android.content.Context
import android.view.View
import com.google.android.material.snackbar.Snackbar

class SnackBarWithMessage(private val mContext: Context) {
    fun showSnackbar(message: String) {
        // Create the Snackbar
        val snackbar = Snackbar.make(
            (mContext as Activity).findViewById(android.R.id.content),
            message,
            Snackbar.LENGTH_LONG
        )

        // Set an action for the Snackbar
        snackbar.setAction("Dismiss") { snackbar.dismiss() }

        // Show the Snackbar
        snackbar.show()
    }
}