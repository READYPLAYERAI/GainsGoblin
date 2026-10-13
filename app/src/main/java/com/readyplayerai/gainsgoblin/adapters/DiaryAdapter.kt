package com.readyplayerai.gainsgoblin.adapters

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView

import androidx.annotation.NonNull
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.readyplayerai.gainsgoblin.R
import com.readyplayerai.gainsgoblin.model.WorkoutDay
import com.readyplayerai.gainsgoblin.ui.DayActivity
import com.readyplayerai.gainsgoblin.ui.MainActivity

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date

// Adapter for WorkoutDay Class
class DiaryAdapter(ct: Context?, Workout_Days: ArrayList<WorkoutDay>?) : RecyclerView.Adapter<DiaryAdapter.MyViewHolder>() {

    private val ct = ct
    private val Workout_Days = ArrayList(Workout_Days)

    @NonNull
    override fun onCreateViewHolder(@NonNull parent: ViewGroup, viewType: Int): MyViewHolder {
        val inflater = LayoutInflater.from(this.ct)
        val view = inflater.inflate(R.layout.diary_row, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(@NonNull holder: MyViewHolder, position: Int) {
        // Set view's data
        val parser = SimpleDateFormat("yyyy-MM-dd")

        // Possible Error
        try {
            val date = parser.parse(Workout_Days!![position].Date!!)
            val formatter1 = SimpleDateFormat("EEEE")
            val formatter2 = SimpleDateFormat("MMMM dd YYYY")

            // Change TextView texts
            holder.tv_day.text = formatter1.format(date)
            holder.date.text = formatter2.format(date)

            // Change RecyclerView items
            val workoutExerciseAdapter = DiaryExerciseAdapter(ct, MainActivity.dataStorage.workoutDays[position].Exercises)
            holder.recyclerView.adapter = workoutExerciseAdapter
            holder.recyclerView.layoutManager = LinearLayoutManager(ct)
        } catch (e: ParseException) {
            e.printStackTrace()
        }

        // Day Dialog
        holder.cardview_diary.setOnClickListener { view ->
            navigateToDay(position)
        }

        // More Button
        holder.expand_button.setOnClickListener { view ->
            System.out.println("More Button")
        }
    }

    fun navigateToDay(position: Int) {
        val `in` = Intent(ct, DayActivity::class.java)
        // Update Date Selected in MainActivity
        MainActivity.dateSelected = Workout_Days!![position].Date
        `in`.putExtra("date", Workout_Days!![position].Date)
        ct!!.startActivity(`in`)
    }

    // Shows Day's Stats
    fun showDayDialog(position: Int) {
        // Prepare to show exercise dialog box
        val inflater = LayoutInflater.from(ct)
        val view = inflater.inflate(R.layout.day_dialog, null)
        val alertDialog = AlertDialog.Builder(ct).setView(view).create()

        // Get TextViews
        val totalsets = view.findViewById<TextView>(R.id.volume)
        val totalreps = view.findViewById<TextView>(R.id.totalreps)
        val totalvolume = view.findViewById<TextView>(R.id.totalvolume)
        val date = view.findViewById<TextView>(R.id.tv_date)
        val totalexercises = view.findViewById<TextView>(R.id.totalexercises)
        val bt_viewDay = view.findViewById<Button>(R.id.bt_start)
        val bt_close = view.findViewById<Button>(R.id.bt_close)

        // Crash Here
        totalsets.text = Workout_Days!![position].Sets!!.size.toString()
        totalreps.text = Workout_Days!![position].Reps.toString()
        totalexercises.text = Workout_Days!![position].Exercises!!.size.toString()
        totalvolume.text = Workout_Days!![position].DayVolume.toString()

        val parser = SimpleDateFormat("yyyy-MM-dd")

        // Possible Error
        try {
            val date_object = parser.parse(Workout_Days!![position].Date!!)
            val formatter = SimpleDateFormat("EEEE, MMM dd YYYY")

            // Format Date like a human being
            date.text = formatter.format(date_object)
        } catch (e: ParseException) {
            e.printStackTrace()
        }

        // Goto Day Activity
        bt_viewDay.setOnClickListener { view ->
            val `in` = Intent(ct, DayActivity::class.java)
            // Update Date Selected in MainActivity
            MainActivity.dateSelected = Workout_Days!![position].Date
            `in`.putExtra("date", Workout_Days!![position].Date)
            ct!!.startActivity(`in`)
        }

        // Delete Day
        bt_close.setOnClickListener { view ->
            System.out.println("Delete Day " + position)
        }

        bt_close.setOnClickListener { view ->
            // Show Exercise Dialog Box
            alertDialog.dismiss()
        }

        // Show Exercise Dialog Box
        alertDialog.show()
    }

    override fun getItemCount(): Int {
        return Workout_Days!!.size
    }

    inner class MyViewHolder(@NonNull itemView: View) : RecyclerView.ViewHolder(itemView) {
        var tv_day: TextView = itemView.findViewById(R.id.day)
        var date: TextView = itemView.findViewById(R.id.tv_date)
        var recyclerView: RecyclerView = itemView.findViewById(R.id.recycler_view_diary)
        var expand_button: ImageButton = itemView.findViewById(R.id.expand_button)
        var cardview_diary: CardView = itemView.findViewById(R.id.cardview_viewpager)
        var blue_line: View = itemView.findViewById(R.id.blue_line)
    }
}