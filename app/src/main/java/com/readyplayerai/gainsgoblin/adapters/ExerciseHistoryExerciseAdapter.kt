package com.readyplayerai.gainsgoblin.adapters

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import androidx.annotation.NonNull
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.readyplayerai.gainsgoblin.R
import com.readyplayerai.gainsgoblin.model.WorkoutExercise

import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date
import kotlin.math.roundToInt

// Adapter for WorkoutExercise Class
class ExerciseHistoryExerciseAdapter(ct: Context?, Exercises: ArrayList<WorkoutExercise>?) : RecyclerView.Adapter<ExerciseHistoryExerciseAdapter.MyViewHolder>() {

    private val ct = ct
    private val Exercises = ArrayList(Exercises)

    @NonNull
    override fun onCreateViewHolder(@NonNull parent: ViewGroup, viewType: Int): MyViewHolder {
        val inflater = LayoutInflater.from(this.ct)
        val view = inflater.inflate(R.layout.exercise_history_exercise_row, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(@NonNull holder: MyViewHolder, position: Int) {
        var date1: Date? = null

        try {
            date1 = SimpleDateFormat("yyyy-MM-dd").parse(Exercises!![position].Date!!)
        } catch (e: ParseException) {
            e.printStackTrace()
        }

        val dateFormat = SimpleDateFormat("EEEE, MMM dd")
        val strDate = dateFormat.format(date1)

        // Change TextView text
        holder.tv_date.text = strDate

        // Recycler View Stuff
        // Change RecyclerView items
        val workoutSetAdapter = WorkoutSetAdapter(ct, Exercises!![position].Sets)
        holder.recyclerView.adapter = workoutSetAdapter
        holder.recyclerView.layoutManager = LinearLayoutManager(ct)

        holder.cardview_exercise_history.setOnClickListener { view ->
            showExerciseHistoryStatsDialog(position)
        }
    }

    // Blatant copy of Fitnotes but ohh well ;)
    fun showExerciseHistoryStatsDialog(position: Int) {
        // Prepare to show exercise dialog box
        val inflater = LayoutInflater.from(ct)
        val view = inflater.inflate(R.layout.exercise_history_stats_dialog, null)
        val alertDialog = AlertDialog.Builder(ct).setView(view).create()

        // Get TextViews
        val totalsets = view.findViewById<TextView>(R.id.volume)
        val totalreps = view.findViewById<TextView>(R.id.totalreps)
        val totalvolume = view.findViewById<TextView>(R.id.totalvolume)
        val maxweight = view.findViewById<TextView>(R.id.maxweight)
        val maxreps = view.findViewById<TextView>(R.id.maxreps)
        val maxsetvolume = view.findViewById<TextView>(R.id.maxsetvolume)
        val name = view.findViewById<TextView>(R.id.tv_date)
        val onerepmax = view.findViewById<TextView>(R.id.onerepmax)

        // Set Values

        // Double -> Integer
        val sets = Exercises!![position].TotalSets!!.roundToInt()
        val reps = Exercises!![position].TotalReps!!.roundToInt()
        val max_reps = Exercises!![position].MaxReps!!.roundToInt()

        totalsets.text = sets.toString()
        totalreps.text = reps.toString()
        maxreps.text = max_reps.toString()

        totalvolume.text = Exercises!![position].Volume.toString()
        maxweight.text = Exercises!![position].MaxWeight.toString()
        onerepmax.text = Exercises!![position].EstimatedOneRepMax.toString()
        name.text = Exercises!![position].Exercise
        maxsetvolume.text = Exercises!![position].MaxSetVolume.toString()

        // Show Exercise Dialog Box
        alertDialog.show()
    }

    override fun getItemCount(): Int {
        return this.Exercises!!.size
    }

    inner class MyViewHolder(@NonNull itemView: View) : RecyclerView.ViewHolder(itemView) {
        var tv_date: TextView = itemView.findViewById(R.id.tv_date)
        var recyclerView: RecyclerView = itemView.findViewById(R.id.recycler_view_day)
        var cardview_exercise_history: CardView = itemView.findViewById(R.id.cardview_exercise_history)
    }
}