package com.readyplayerai.gainsgoblin.adapters

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.appcompat.widget.PopupMenu
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

import com.readyplayerai.gainsgoblin.R
import com.readyplayerai.gainsgoblin.model.WorkoutSet
import com.readyplayerai.gainsgoblin.ui.AddExerciseActivity

import java.util.ArrayList

// Adapter for WorkoutSet Class
class AddExerciseWorkoutSetAdapter(private val ct: Context?, private val Workout_Sets: ArrayList<WorkoutSet>?) : RecyclerView.Adapter<AddExerciseWorkoutSetAdapter.MyViewHolder>() {

    @NonNull
    override fun onCreateViewHolder(@NonNull parent: ViewGroup, viewType: Int): MyViewHolder {
        val inflater = LayoutInflater.from(this.ct)
        val view = inflater.inflate(R.layout.workout_set_row, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(@NonNull holder: MyViewHolder, position: Int) {
        // Double -> String
        holder.tv_weight.text = Workout_Sets!![position].weight.toString()

        // Double -> Integer -> String
        holder.tv_reps.text = Workout_Sets!![position].reps!!.toInt().toString()

        // Updates Edit Texts and Buttons when clicked
        holder.cardView.setOnClickListener { view ->
            updateView(position)
        }

        holder.cardView.setOnLongClickListener { view ->
            AddExerciseActivity.Clicked_Set = position
            showSetPopupMenu(holder, view, position)
            true
        }
    }

    // Notify AddExerciseActivity of the clicked position
    fun updateView(position: Int) {
        AddExerciseActivity.bt_clear.text = "Clear"
        AddExerciseActivity.bt_save.text = "Save"

        // Updates the position of the user selected set in AddExerciseActivity
        AddExerciseActivity.Clicked_Set = position

        // Updates ets buttons and sets in AddExerciseActivity
        AddExerciseActivity.UpdateViewOnClick()
    }

    // To Do: Implement Delete functionality
    private fun showSetPopupMenu(holder: MyViewHolder, view: View, position: Int) {
        val popupMenu = PopupMenu(view.context, view, Gravity.NO_GRAVITY, androidx.appcompat.R.attr.actionOverflowMenuStyle, 0)

        popupMenu.inflate(R.menu.set_add_exercise_activity_menu)
        popupMenu.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.delete) {
                println("Delete Set Clicked")
                AddExerciseActivity.deleteSet(view.context)
            } else if (item.itemId == R.id.edit) {
                println("Edit Set Clicked")

                // To Do:
                // Keep Set highlighted in Recyclerview
                // Save ---> Update and change color
                // Notify data changed in adapter

                AddExerciseActivity.editSet(holder, view, position)
            }
            false
        }
        popupMenu.show()
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