package com.readyplayerai.gainsgoblin.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView

import com.readyplayerai.gainsgoblin.R

import java.util.ArrayList

// Adapter for WorkoutDay Class
class StringAdapter(ct: Context?, StringList: ArrayList<String>?) : RecyclerView.Adapter<StringAdapter.MyViewHolder>() {

    private val ct = ct
    private val StringList = ArrayList(StringList)

    @NonNull
    override fun onCreateViewHolder(@NonNull parent: ViewGroup, viewType: Int): MyViewHolder {
        val inflater = LayoutInflater.from(this.ct)
        val view = inflater.inflate(R.layout.personal_record_row, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(@NonNull holder: MyViewHolder, position: Int) {
        holder.tv_personal_record.text = StringList[position]
    }

    override fun getItemCount(): Int {
        return StringList.size
    }

    inner class MyViewHolder(@NonNull itemView: View) : RecyclerView.ViewHolder(itemView) {
        var tv_personal_record: TextView = itemView.findViewById(R.id.tv_personal_record)
    }
}