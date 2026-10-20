package com.readyplayerai.gainsgoblin.adapters

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.readyplayerai.gainsgoblin.R
import com.readyplayerai.gainsgoblin.model.WorkoutExercise

import java.util.ArrayList

// Adapter for WorkoutExercise Class
class ViewPagerExerciseAdapter(
    ct: Context?,
    Exercises: ArrayList<WorkoutExercise>?,
    private val onExerciseClick: ((Int) -> Unit)? = null,
    private val categoryFor: ((String) -> String?)? = null
) : RecyclerView.Adapter<ViewPagerExerciseAdapter.MyViewHolder>() {

    private val ct = ct
    private val Exercises = ArrayList(Exercises)

    @NonNull
    override fun onCreateViewHolder(@NonNull parent: ViewGroup, viewType: Int): MyViewHolder {
        val inflater = LayoutInflater.from(this.ct)
        val view = inflater.inflate(R.layout.view_pager_exercise_row, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(@NonNull holder: MyViewHolder, position: Int) {
        // Change TextView text
        holder.tv_exercise_name.text = Exercises[position].Exercise

        // Recycler View Stuff
        // Change RecyclerView items
        val workoutSetAdapter = WorkoutSetAdapter(ct, Exercises[position].Sets)
        holder.recyclerView.adapter = workoutSetAdapter
        holder.recyclerView.layoutManager = LinearLayoutManager(ct)

        // Navigate to AddActivity (behavior supplied by the Activity)
        holder.cardview_exercise2.setOnClickListener { view ->
            onExerciseClick?.invoke(position)
        }

        // Change exercise color accordingly
        setCategoryIconTint(holder, Exercises[position].Exercise!!)
    }

    // Simple
    fun setCategoryIconTint(holder: MyViewHolder, exercise_name: String) {
        val exercise_category = categoryFor?.invoke(exercise_name) ?: ""

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

    override fun getItemCount(): Int {
        return this.Exercises.size
    }

    inner class MyViewHolder(@NonNull itemView: View) : RecyclerView.ViewHolder(itemView) {
        var tv_exercise_name: TextView = itemView.findViewById(R.id.tv_date)
        var recyclerView: RecyclerView = itemView.findViewById(R.id.recycler_view_day)
        var blue_line: View = itemView.findViewById(R.id.blue_line)
        var cardview_exercise2: CardView = itemView.findViewById(R.id.cardview_exercise_history)
        var imageView: ImageView = itemView.findViewById(R.id.imageView3)
    }
}
