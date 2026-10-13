package com.readyplayerai.gainsgoblin

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView

object CustomDialog {
    @JvmStatic
    fun showDialog(context: Context?, layoutResourceId: Int, message: String?, yesButtonText: String?, noButtonText: String?, yesClickListener: View.OnClickListener?, noClickListener: View.OnClickListener?) {
        val inflater = LayoutInflater.from(context)
        val view1 = inflater.inflate(layoutResourceId, null)
        val alertDialog = AlertDialog.Builder(context).setView(view1).create()

        val tv_date = view1.findViewById<TextView>(R.id.tv_date)
        tv_date.text = message

        val bt_yes3 = view1.findViewById<Button>(R.id.bt_yes3)
        bt_yes3.text = yesButtonText
        val bt_no3 = view1.findViewById<Button>(R.id.bt_no3)
        bt_no3.text = noButtonText

        bt_yes3.setOnClickListener { view ->
            alertDialog.dismiss()
            if (yesClickListener != null) {
                yesClickListener.onClick(view)
            }
        }

        bt_no3.setOnClickListener { view ->
            alertDialog.dismiss()
            if (noClickListener != null) {
                noClickListener.onClick(view)
            }
        }

        alertDialog.show()
    }
}