package com.readyplayerai.gainsgoblin.adapters

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageButton
import android.widget.TextView

import androidx.annotation.NonNull
import androidx.appcompat.widget.PopupMenu
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

import com.readyplayerai.gainsgoblin.ExercisePersonalStats
import com.readyplayerai.gainsgoblin.R

import java.util.ArrayList
import java.util.Iterator
import java.util.Locale
import kotlin.math.floor

// Adapter for WorkoutExercise Class
class ExerciseStatsAdapter(
    ct: Context?,
    ExerciseStats: ArrayList<ExercisePersonalStats>?,
    private val onFavoriteToggle: ((Int) -> Unit)? = null,
    private val categoryFor: ((String) -> String?)? = null
) : RecyclerView.Adapter<ExerciseStatsAdapter.MyViewHolder>() , Filterable {

    private val ct = ct
    private val exercisePersonalStats = ArrayList(ExerciseStats)
    private val exercisePersonalStatsFull = ArrayList(ExerciseStats)

    @NonNull
    override fun onCreateViewHolder(@NonNull parent: ViewGroup, viewType: Int): MyViewHolder {
        val inflater = LayoutInflater.from(this.ct)
        val view = inflater.inflate(R.layout.exercise_stats_row, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(@NonNull holder: MyViewHolder, position: Int) {
        val exercise_name = exercisePersonalStats!![position].exerciseName!!
        val exercise_category = exercisePersonalStats!![position].exerciseCategory!!
        val max_weight = exercisePersonalStats!![position].maxWeight.toString()
        val max_reps = exercisePersonalStats!![position].maxReps.toString()
        val max_set_volume = exercisePersonalStats!![position].maxSetVolume.toString()
        val estimated_1rm = floor(exercisePersonalStats!![position].estimated1RM!!).toString()
        val actual_1rm = exercisePersonalStats!![position].actual1RM.toString()
        val isFavorite = exercisePersonalStats!![position].favorite

        val max_volume_set = exercisePersonalStats!![position].maxVolumeSet
        val max_reps_set = exercisePersonalStats!![position].maxRepsSet
        val max_weight_set = exercisePersonalStats!![position].maxWeightSet

        holder.tv_exercise_name.text = exercise_name
        holder.tv_exercise_category.text = exercise_category

        // Best weight set
        holder.tv_maxweight.text = max_weight_set!!.weight.toString()
        holder.tv_maxweight_reps.text = max_weight_set!!.reps.toString()

        // Best reps set
        holder.tv_maxreps_weight.text = max_reps_set!!.weight.toString()
        holder.tv_maxreps_reps.text = max_reps_set!!.reps.toString()

        // Best Volume set
        holder.tv_maxsetvolume_weight.text = max_volume_set!!.weight.toString()
        holder.tv_maxsetvolume_reps.text = max_volume_set!!.reps.toString()

        // if 1rm was performed
        if (actual_1rm == "0.0") {
            holder.tv_est_actual_1rm.text = "Estimated 1RM"
            holder.tv_estimated_1rm.text = estimated_1rm
        } else {
            holder.tv_est_actual_1rm.text = "1RM"
            holder.tv_estimated_1rm.text = actual_1rm
        }

        // By default cards are expanded
        holder.moreButton.setImageResource(R.drawable.ic_expand_more_24px)
        holder.cardview_stats.visibility = View.VISIBLE
        holder.blue_line.visibility = View.VISIBLE

        holder.cardview_viewpager.setOnClickListener { view ->
            // Shrink Card
            if (holder.cardview_stats.visibility == View.VISIBLE) {
                holder.moreButton.setImageResource(R.drawable.ic_expand_less_24px)
                holder.cardview_stats.visibility = View.GONE
                holder.blue_line.visibility = View.INVISIBLE
            }
            // Expand Card
            else {
                holder.moreButton.setImageResource(R.drawable.ic_expand_more_24px)
                holder.cardview_stats.visibility = View.VISIBLE
                holder.blue_line.visibility = View.VISIBLE
            }
        }

        holder.cardview_viewpager.setOnLongClickListener { view ->
            System.out.println("Long Clicked on " + exercise_name)
            showPopupMenu(holder, view, position)
            true
        }

        setCategoryIconTint(holder, exercise_name, isFavorite, position)
    }

    fun deleteExercise(exercise_name: String?) {
        val exerciseIterator = this.exercisePersonalStats!!.iterator()
        while (exerciseIterator.hasNext()) {
            val current_exercise = exerciseIterator.next()
            if (current_exercise.exerciseName == exercise_name) {
                exerciseIterator.remove()
                notifyDataSetChanged()
            }
        }

        // Keep the full list in sync so deleted exercises don't reappear in search results
        val exerciseIteratorFull = this.exercisePersonalStatsFull.iterator()
        while (exerciseIteratorFull.hasNext()) {
            val current_exercise = exerciseIteratorFull.next()
            if (current_exercise.exerciseName == exercise_name) {
                exerciseIteratorFull.remove()
            }
        }
    }

    private fun showPopupMenu(holder: ExerciseStatsAdapter.MyViewHolder, view: View, position: Int) {
        val popupMenu = PopupMenu(view.getContext(), view, Gravity.NO_GRAVITY, androidx.appcompat.R.attr.actionOverflowMenuStyle, 0)

        popupMenu.inflate(R.menu.exercise_personal_record_floating_context_menu)
        popupMenu.setOnMenuItemClickListener(PopupMenu.OnMenuItemClickListener { item ->
            if (item.getItemId() == R.id.charts) {
                System.out.println("Charts Clicked")
            } else if (item.getItemId() == R.id.favorite) {
                System.out.println("Favorite Clicked")

                onFavoriteToggle?.invoke(position)
            }
            false
        })
        popupMenu.show()
    }

    /** Exercise name at [position] in the (possibly filtered) list; null when out of range. */
    fun nameAt(position: Int): String? = exercisePersonalStats.getOrNull(position)?.exerciseName

    // Simple
    fun setCategoryIconTint(holder: ExerciseStatsAdapter.MyViewHolder, exercise_name: String, isFavorite: Boolean?, position: Int) {
        val exercise_category = categoryFor?.invoke(exercise_name) ?: ""

        if (isFavorite != true) {
            holder.favoriteButton.setImageResource(R.drawable.ic_brightness_1_fill1_wght400_grad0_opsz20)

            // Set Category Color
            if (exercise_category == "Shoulders") {
                holder.favoriteButton.setColorFilter(Color.argb(255, 0, 116, 189)) // Primary Color
            } else if (exercise_category == "Back") {
                holder.favoriteButton.setColorFilter(Color.argb(255, 40, 176, 192))
            } else if (exercise_category == "Chest") {
                holder.favoriteButton.setColorFilter(Color.argb(255, 92, 88, 157))
            } else if (exercise_category == "Biceps") {
                holder.favoriteButton.setColorFilter(Color.argb(255, 255, 50, 50))
            } else if (exercise_category == "Triceps") {
                holder.favoriteButton.setColorFilter(Color.argb(255, 204, 154, 0))
            } else if (exercise_category == "Legs") {
                holder.favoriteButton.setColorFilter(Color.argb(255, 212, 25, 97))
            } else if (exercise_category == "Abs") {
                holder.favoriteButton.setColorFilter(Color.argb(255, 255, 153, 171))
            } else {
                holder.favoriteButton.setColorFilter(Color.argb(255, 52, 58, 64)) // Grey AF
            }
        } else {
            holder.favoriteButton.setImageResource(R.drawable.ic_star_fill0_wght400_grad0_opsz24)
            holder.favoriteButton.setColorFilter(Color.argb(255, 0xff, 0xd6, 0x24)) // Yellowish
        }
    }

    override fun getItemCount(): Int {
        return this.exercisePersonalStats!!.size
    }

    // Implement Filterable Methods
    override fun getFilter(): Filter {
        return exampleFilter
    }

    private inner class ExerciseStatsFilter : Filter() {
        // Return our filtered results
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val Exercises_Filtered = ArrayList<ExercisePersonalStats>()

            // Don't filter anything
            if (constraint == null || constraint.length == 0) {
                Exercises_Filtered.addAll(exercisePersonalStatsFull)
            }
            // Something was typed so filter results
            else {
                val filterPattern = constraint.toString().lowercase(Locale.getDefault()).trim()

                for (i in 0 until exercisePersonalStatsFull.size) {
                    // If search patterns is contained in exercise name then show it
                    if (exercisePersonalStatsFull[i].exerciseName!!.lowercase(Locale.getDefault()).contains(filterPattern)) {
                        Exercises_Filtered.add(exercisePersonalStatsFull[i])
                    }
                }
            }

            // Return results object to the publishResults method below
            val results = FilterResults()
            results.values = Exercises_Filtered
            return results
        }

        // Publish Results to the UI thread
        override fun publishResults(constraint: CharSequence?, results: FilterResults) {
            exercisePersonalStats!!.clear()
            exercisePersonalStats!!.addAll(results.values as ArrayList<ExercisePersonalStats>) // Add only the filtered items
            notifyDataSetChanged() // Update Recycler View
        }
    }

    private val exampleFilter = ExerciseStatsFilter()

    inner class MyViewHolder(@NonNull itemView: View) : RecyclerView.ViewHolder(itemView) {
        var tv_exercise_name: TextView = itemView.findViewById(R.id.tv_exercise_name)
        var tv_exercise_category: TextView = itemView.findViewById(R.id.exerciseCategory)
        var tv_maxweight: TextView = itemView.findViewById(R.id.tv_maxweight)
        var tv_maxweight_reps: TextView = itemView.findViewById(R.id.tv_maxweight_reps)
        var tv_maxreps_weight: TextView = itemView.findViewById(R.id.tv_maxreps_weight)
        var tv_maxreps_reps: TextView = itemView.findViewById(R.id.tv_maxreps_reps)
        var tv_maxsetvolume: TextView = itemView.findViewById(R.id.tv_max_set_volume)
        var tv_maxsetvolume_weight: TextView = itemView.findViewById(R.id.tv_max_set_volume_weight)
        var tv_maxsetvolume_reps: TextView = itemView.findViewById(R.id.tv_max_set_volume_reps)
        var tv_estimated_1rm: TextView = itemView.findViewById(R.id.tv_estimated_1rm)
        var tv_est_actual_1rm: TextView = itemView.findViewById(R.id.tv_est_actual_1rm)
        var cardview_viewpager: CardView = itemView.findViewById(R.id.cardview_viewpager)
        var cardview_stats: CardView = itemView.findViewById(R.id.cardview_stats)
        var favoriteButton: ImageButton = itemView.findViewById(R.id.favoriteButton)
        var moreButton: ImageButton = itemView.findViewById(R.id.moreButton)
        var blue_line: View = itemView.findViewById(R.id.blue_line)
    }
}