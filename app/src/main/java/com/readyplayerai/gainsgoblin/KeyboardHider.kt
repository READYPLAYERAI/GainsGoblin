package com.readyplayerai.gainsgoblin

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager

class KeyboardHider(private val mContext: Context) {
    fun hideKeyboard() {
        // Get a reference to the input method manager
        val imm = mContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        // Get a reference to the currently focused view
        val focusedView = (mContext as Activity).currentFocus

        // Hide the soft keyboard if shown
        if (focusedView != null && imm.isAcceptingText()) {
            imm.hideSoftInputFromWindow(focusedView.windowToken, 0)
        }
    }
}