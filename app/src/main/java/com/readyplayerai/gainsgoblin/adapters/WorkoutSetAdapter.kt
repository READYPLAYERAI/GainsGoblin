package com.readyplayerai.gainsgoblin.adapters

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

import com.readyplayerai.gainsgoblin.R
import com.readyplayerai.gainsgoblin.model.WorkoutSet

import java.util.ArrayList
import kotlin.math.roundToInt

// Adapter for WorkoutSet Class
class WorkoutSetAdapter(private val ct: Context?, private val Workout_Sets: ArrayList<WorkoutSet>?) : RecyclerView.Adapter<WorkoutSetAdapter.MyViewHolder>() {

    @NonNull
    override fun onCreateViewHolder(@NonNull parent: ViewGroup, viewType: Int): MyViewHolder {
        val inflater = LayoutInflater.from(this.ct)
        val view = inflater.inflate(R.layout.workout_set_row, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(@NonNull holder: MyViewHolder, position: Int) {
        holder.tv_weight.text = Workout_Sets!![position].weight.toString()

        // Double -> Integer
        val reps = Workout_Sets!![position].reps!!.roundToInt()
        holder.tv_reps.text = reps.toString()

        // Shows Set Stats when Clicked
        holder.cardView.setOnClickListener { view ->
            showSetDialog(position)
        }
    }

    fun showSetDialog(position: Int) {
        // Prepare to show exercise dialog box
        val inflater = LayoutInflater.from(ct)
        val view = inflater.inflate(R.layout.set_dialog, null)
        val alertDialog = AlertDialog.Builder(ct).setView(view).create()

        val volume = view.findViewById<TextView>(R.id.volume)
        val onerepmax = view.findViewById<TextView>(R.id.onerepmax)
        val reps = view.findViewById<TextView>(R.id.reps)
        val kg = view.findViewById<TextView>(R.id.tv_date)

        // Double -> Integer
        val repetitions = Workout_Sets!![position].reps!!.roundToInt()
        reps.text = repetitions.toString()

        volume.text = Workout_Sets!![position].getVolume().toString()
        val est1rm = kotlin.math.floor(Workout_Sets!![position].getEplayOneRepMax()!!)
        onerepmax.text = est1rm.toString()

        kg.text = Workout_Sets!![position].weight.toString()

        // Show Exercise Dialog Box
        alertDialog.show()
    }

    override fun getItemCount(): Int {
        return Workout_Sets!!.size
    }

    inner class MyViewHolder(@NonNull itemView: View) : RecyclerView.ViewHolder(itemView) {
        var tv_reps: TextView = itemView.findViewById(R.id.set_reps)
        var tv_weight: TextView = itemView.findViewById(R.id.tv_date)
        var cardView: CardView = itemView.findViewById(R.id.cardview_set)
    }
}