package com.readyplayerai.gainsgoblin.adapters

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView

import androidx.annotation.NonNull
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.readyplayerai.gainsgoblin.R
import com.readyplayerai.gainsgoblin.model.WorkoutExercise
import com.readyplayerai.gainsgoblin.ui.AddExerciseActivity
import com.readyplayerai.gainsgoblin.ui.MainActivity

import java.util.ArrayList
import kotlin.math.roundToInt

// Adapter for WorkoutExercise Class
class DiaryExerciseAdapter(ct: Context?, Exercises: ArrayList<WorkoutExercise>?) : RecyclerView.Adapter<DiaryExerciseAdapter.MyViewHolder>() {

    private val ct = ct
    private val Exercises = ArrayList(Exercises)

    @NonNull
    override fun onCreateViewHolder(@NonNull parent: ViewGroup, viewType: Int): MyViewHolder {
        val inflater = LayoutInflater.from(this.ct)
        val view = inflater.inflate(R.layout.diary_exercise_row, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(@NonNull holder: MyViewHolder, position: Int) {
        // Get exercise name
        val exercise_name = Exercises!![position].Exercise!!

        // Change TextView text
        holder.tv_exercise_name.text = exercise_name
        val sets = Exercises!![position].TotalSets!!.roundToInt()
        holder.sets.text = sets.toString()

        // Set bubble icon tint based on exercise category
        setCategoryIconTint(holder, exercise_name)

        // Set PR icon tint
        val Records = initializePersonalRecordIcon(holder, position)

        // Show the comment icon or not
        initializeCommentButton(holder, position)

        holder.comment_button.setOnClickListener { v ->
            showCommentDialog(position)
        }

        holder.pr_button.setOnClickListener { view ->
            showVolumePRDialog(Records)
        }

        holder.cardView.setOnClickListener { view ->
            navigateToExercise(position)
        }
    }

    // Sets the PR icon color accordingly
    fun initializePersonalRecordIcon(holder: MyViewHolder, position: Int): ArrayList<String> {
        // Count the number of PRs
        var records = 0

        // Records
        val Records = ArrayList<String>()

        // Set Volume PR Icon if exercise was a Volume PR
        if (Exercises!![position].VolumePR) {
            holder.pr_button.visibility = View.VISIBLE
            records++
            Records.add("Volume PR")
        }
        if (Exercises!![position].ActualOneRepMaxPR) {
            holder.pr_button.visibility = View.VISIBLE
            records++
            Records.add("One Rep Max PR")
        }
        if (Exercises!![position].EstimatedOneRepMaxPR) {
            holder.pr_button.visibility = View.VISIBLE
            records++
            Records.add("Estimated One Rep Max PR")
        }
        if (Exercises!![position].MaxRepsPR) {
            holder.pr_button.visibility = View.VISIBLE
            records++
            Records.add("Maximum Repetitions PR")
        }
        if (Exercises!![position].MaxWeightPR) {
            holder.pr_button.visibility = View.VISIBLE
            records++
            Records.add("Maximum Weight PR")
        }
        if (Exercises!![position].HTLT) {
            holder.pr_button.visibility = View.VISIBLE
            records++
            Records.add("Harder Than Last Time")
        } else {
            holder.pr_button.visibility = View.GONE
        }

        // When having multiple PRs
        if (records > 1) {
            holder.pr_button.setImageResource(R.drawable.ic_whatshot_24px)
            holder.pr_button.visibility = View.VISIBLE
            holder.pr_button.setColorFilter(Color.argb(255, 255, 0, 0))
        }

        return Records
    }

    // Set the comment button accordingly
    fun initializeCommentButton(holder: MyViewHolder, position: Int) {
        val Comment = Exercises!![position].Comment

        if (Comment == null) {
            holder.comment_button.visibility = View.GONE
        } else if (Comment == "") {
            holder.comment_button.visibility = View.GONE
        } else if (Comment!!.isEmpty()) {
            holder.comment_button.visibility = View.GONE
        } else {
            holder.comment_button.visibility = View.VISIBLE
        }
    }

    fun showVolumePRDialog(Records: ArrayList<String>?) {
        // Prepare to show exercise dialog box
        val inflater = LayoutInflater.from(ct)
        val view = inflater.inflate(R.layout.personal_record_dialog, null)
        val alertDialog = AlertDialog.Builder(ct).setView(view).create()

        // Set TextView text accordingly
        val tv_record = view.findViewById<TextView>(R.id.tv_record)

        if (Records!!.size > 1) {
            tv_record.text = "Multiple Records"
        } else {
            tv_record.text = "Record"
        }

        // Initialize personal record recyclerview
        val recyclerViewPR = view.findViewById<RecyclerView>(R.id.recyclerViewPR)
        val stringAdapter = StringAdapter(ct, Records)
        recyclerViewPR.adapter = stringAdapter
        recyclerViewPR.layoutManager = LinearLayoutManager(ct)

        // Show Exercise Dialog Box
        alertDialog.show()
    }

    fun showCommentDialog(position: Int) {
        // Prepare to show exercise dialog box
        val inflater = LayoutInflater.from(ct)
        val view = inflater.inflate(R.layout.show_exercise_comment_dialog, null)
        val alertDialog = AlertDialog.Builder(ct).setView(view).create()
        val tv_exercise_comment = view.findViewById<TextView>(R.id.tv_exercise_comment)
        tv_exercise_comment.text = Exercises!![position].Comment!!

        // Show Exercise Dialog Box
        alertDialog.show()
    }

    // Simple
    fun setCategoryIconTint(holder: MyViewHolder, exercise_name: String) {
        val exercise_category = MainActivity.dataStorage.getExerciseCategory(exercise_name)

        if (exercise_category == "Shoulders") {
            holder.imageView.setColorFilter(Color.argb(255, 0, 116, 189)) // Primary Color
        } else if (exercise_category == "Back") {
            holder.imageView.setColorFilter(Color.argb(255, 40, 176, 192))
        } else if (exercise_category == "Chest") {
            holder.imageView.setColorFilter(Color.argb(255, 92, 88, 157))
        } else if (exercise_category == "Biceps") {
            holder.imageView.setColorFilter(Color.argb(255, 255, 50, 50))
        } else if (exercise_category == "Triceps") {
            holder.imageView.setColorFilter(Color.argb(255, 204, 154, 0))
        } else if (exercise_category == "Legs") {
            holder.imageView.setColorFilter(Color.argb(255, 212, 25, 97))
        } else if (exercise_category == "Abs") {
            holder.imageView.setColorFilter(Color.argb(255, 255, 153, 171))
        } else {
            holder.imageView.setColorFilter(Color.argb(255, 52, 58, 64)) // Grey AF
        }
    }

    fun navigateToExercise(position: Int) {
        val `in` = Intent(ct, AddExerciseActivity::class.java)
        `in`.putExtra("exercise", Exercises!![position].Exercise)
        MainActivity.dateSelected = Exercises!![position].Date // this is required by AddExerciseActivity
        System.out.println(Exercises!![position].Exercise)
        System.out.println(MainActivity.dateSelected)
        ct!!.startActivity(`in`)
    }

    // Blatant copy of FitNotes but ohh well ;)
    fun showExerciseDialog(position: Int) {
        // Prepare to show exercise dialog box
        val inflater = LayoutInflater.from(ct)
        val view = inflater.inflate(R.layout.exercise_dialog, null)
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
        val actualonerepmax = view.findViewById<TextView>(R.id.tv_maxweight)
        val bt_close = view.findViewById<Button>(R.id.bt_close)
        val bt_edit_exercise = view.findViewById<Button>(R.id.bt_edit_exercise)

        // Set Values

        // Double -> Integer
        val sets = Exercises!![position].TotalSets!!.roundToInt()
        val reps = Exercises!![position].TotalReps!!.roundToInt()
        val max_reps = Exercises!![position].MaxReps!!.roundToInt()

        totalsets.text = sets.toString()
        totalreps.text = reps.toString()
        maxreps.text = max_reps.toString()

        // Double
        totalvolume.text = Exercises!![position].Volume.toString()
        maxweight.text = Exercises!![position].MaxWeight.toString()
        onerepmax.text = Exercises!![position].EstimatedOneRepMax.toString()
        actualonerepmax.text = Exercises!![position].ActualOneRepMax.toString()
        name.text = Exercises!![position].Exercise
        maxsetvolume.text = Exercises!![position].MaxSetVolume.toString()

        // Navigate to AddExercise Activity
        bt_edit_exercise.setOnClickListener { view ->
            val `in` = Intent(ct, AddExerciseActivity::class.java)
            `in`.putExtra("exercise", Exercises!![position].Exercise)
            MainActivity.dateSelected = Exercises!![position].Date // this is required by AddExerciseActivity
            System.out.println(Exercises!![position].Exercise)
            System.out.println(MainActivity.dateSelected)
            ct!!.startActivity(`in`)
        }

        bt_close.setOnClickListener { view ->
            // Dismiss Exercise Dialog Box
            alertDialog.dismiss()
        }

        // Show Exercise Dialog Box
        alertDialog.show()
    }

    override fun getItemCount(): Int {
        return this.Exercises!!.size
    }

    inner class MyViewHolder(@NonNull itemView: View) : RecyclerView.ViewHolder(itemView) {
        var tv_exercise_name: TextView = itemView.findViewById(R.id.day)
        var cardView: CardView = itemView.findViewById(R.id.cardview_exercise)
        var sets: TextView = itemView.findViewById(R.id.sets)
        var imageView: ImageView = itemView.findViewById(R.id.imageView)
        var pr_button: ImageButton = itemView.findViewById(R.id.pr_button)
        var comment_button: ImageButton = itemView.findViewById(R.id.comment_button)
    }
}