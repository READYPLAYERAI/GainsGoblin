package com.readyplayerai.gainsgoblin.adapters

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Filter
import android.widget.Filterable
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast

import androidx.annotation.NonNull
import androidx.appcompat.widget.PopupMenu
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

import com.readyplayerai.gainsgoblin.model.Exercise
import com.readyplayerai.gainsgoblin.R
import com.readyplayerai.gainsgoblin.ui.AddExerciseActivity
import com.readyplayerai.gainsgoblin.ui.MainActivity

import java.util.ArrayList
import java.util.Iterator
import java.util.Locale

// Adapter for Exercise Class
class ExerciseAdapter(ct: Context?, Exercises: ArrayList<Exercise>?) : RecyclerView.Adapter<ExerciseAdapter.MyViewHolder>() , Filterable {

    private val ct = ct
    private val Exercises = ArrayList(Exercises) // If you this is changed to: this.Exercises = Exercises; then on search diary activity will not recognize known exercises
    private val Exercises_Full = ArrayList(Exercises)

    @NonNull
    override fun onCreateViewHolder(@NonNull parent: ViewGroup, viewType: Int): MyViewHolder {
        val inflater = LayoutInflater.from(this.ct)
        val view = inflater.inflate(R.layout.exercise_row, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(@NonNull holder: MyViewHolder, position: Int) {
        // Change TextView text
        holder.tv_exercise_name.text = Exercises!![position].Name
        holder.tv_exercise_bodypart.text = Exercises!![position].BodyPart

        // Goto Add Exercise Activity
        holder.cardview_exercise_1.setOnClickListener { view ->
            val `in` = Intent(ct, AddExerciseActivity::class.java)
            `in`.putExtra("exercise", holder.tv_exercise_name.text.toString())
            ct!!.startActivity(`in`)
        }
    }

    fun deleteExercise(exercise_name: String?) {
        val exerciseIterator = this.Exercises!!.iterator()
        while (exerciseIterator.hasNext()) {
            val current_exercise = exerciseIterator.next()
            if (current_exercise.Name == exercise_name) {
                exerciseIterator.remove()
                notifyDataSetChanged()
            }
        }
    }

    override fun getItemCount(): Int {
        return this.Exercises!!.size
    }

    // Implement Filterable Methods
    override fun getFilter(): Filter {
        return exampleFilter
    }

    private inner class ExerciseAdapterFilter : Filter() {
        // Return our filtered results
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val Exercises_Filtered = ArrayList<Exercise>()

            // Don't filter anything
            if (constraint == null || constraint.length == 0) {
                Exercises_Filtered.addAll(Exercises_Full!!)
            }
            // Something was typed so filter results
            else {
                val filterPattern = constraint.toString().lowercase(Locale.getDefault()).trim()

                for (i in 0 until Exercises!!.size) {
                    // If search patterns is contained in exercise name then show it
                    if (Exercises!![i].Name!!.lowercase(Locale.getDefault()).contains(filterPattern)) {
                        Exercises_Filtered.add(Exercises!![i])
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
            Exercises!!.clear()
            Exercises!!.addAll(results.values as ArrayList<Exercise>) // Add only the filtered items
            notifyDataSetChanged() // Update Recycler View
        }
    }

    private val exampleFilter = ExerciseAdapterFilter()

    inner class MyViewHolder(@NonNull itemView: View) : RecyclerView.ViewHolder(itemView), View.OnLongClickListener, PopupMenu.OnMenuItemClickListener, AdapterView.OnItemSelectedListener {
        var tv_exercise_name: TextView = itemView.findViewById(R.id.tv_date)
        var tv_exercise_bodypart: TextView = itemView.findViewById(R.id.exercise_bodypart)
        var cardview_exercise_1: CardView = itemView.findViewById(R.id.cardview_exercise_1)

        // For PopUp Menu
        init {
            cardview_exercise_1.setOnLongClickListener(this)
        }

        var exercise_name: String? = null
        var new_exercise_category: String? = null
        var current_exercise_category_position: Int = 0

        // Triggered when someone long presses on cardview_exercise_1
        override fun onLongClick(view: View): Boolean {
            showPopupMenu(view)
            return true
        }

        private fun showPopupMenu(view: View) {
            val popupMenu = PopupMenu(view.getContext(), view)
            popupMenu.inflate(R.menu.exercises_activity_floating_context_menu)
            popupMenu.setOnMenuItemClickListener(this)
            popupMenu.show()
        }

        override fun onMenuItemClick(item: MenuItem): Boolean {
            // Edit Exercise
            if (item.getItemId() == R.id.edit) {
                val position = adapterPosition
                System.out.println("Edit")

                // Prepare to show exercise dialog box
                val inflater = LayoutInflater.from(ct)
                val view = inflater.inflate(R.layout.edit_exercise_dialog, null)
                val alertDialog = AlertDialog.Builder(ct).setView(view).create()

                // Find views
                val bt_save = view.findViewById<Button>(R.id.bt_login_signup)
                val bt_cancel = view.findViewById<Button>(R.id.bt_cancel)
                val et_exercise_name = view.findViewById<EditText>(R.id.et_exercise_name)
                val spinner = view.findViewById<Spinner>(R.id.spinner)

                // Setup Spinner Stuff
                val adapter = ArrayAdapter.createFromResource(ct!!, R.array.Categories, android.R.layout.simple_spinner_item)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinner.adapter = adapter
                spinner.setOnItemSelectedListener(this)

                // Get Array from xml
                val listValue = ct!!.getResources().getStringArray(R.array.Categories)

                // Find Current Category position
                for (i in 0 until listValue.size) {
                    if (listValue[i] == Exercises!![position].BodyPart) {
                        current_exercise_category_position = i
                        System.out.println(listValue[i])
                    }
                }

                // Set edit text and spinner initial values
                exercise_name = Exercises!![position].Name
                et_exercise_name.setText(exercise_name!!)
                spinner.setSelection(current_exercise_category_position)

                // Dismiss Dialog Box
                bt_cancel.setOnClickListener { view ->
                    alertDialog.dismiss()
                }

                // Replace all instances with new exercise
                bt_save.setOnClickListener { view ->
                    // Get User defined new name
                    val new_exercise_name = et_exercise_name.text.toString()

                    // Call Edit Exercise if user gave reasonable input
                    if (new_exercise_category != null && !new_exercise_category!!.isEmpty() && new_exercise_category!!.length > 0 && new_exercise_name != null && !new_exercise_name.isEmpty() && new_exercise_name.length > 0) {
                        locallyUpdateExercise(new_exercise_name)
                        alertDialog.dismiss()
                    }
                    // Tell user to stop fucking around
                    else {
                        Toast.makeText(ct!!, "Please choose an apropriate name", Toast.LENGTH_SHORT).show()
                    }
                }

                alertDialog.show()
                return true
            }

            // Delete Exercise
            else if (item.getItemId() == R.id.delete) {
                val position = adapterPosition

                // Prepare to show exercise dialog box
                val inflater = LayoutInflater.from(ct)
                val view = inflater.inflate(R.layout.delete_exercise_dialog, null)
                val alertDialog = AlertDialog.Builder(ct).setView(view).create()

                val bt_yes = view.findViewById<Button>(R.id.bt_yes3)
                val bt_no = view.findViewById<Button>(R.id.bt_no3)

                bt_no.setOnClickListener { view ->
                    alertDialog.dismiss()
                }

                bt_yes.setOnClickListener { view ->
                    locallyDeleteExercise(position, ct!!)
                    alertDialog.dismiss()
                }

                alertDialog.show()
                return true
            }
            return false
        }

        fun locallyUpdateExercise(new_exercise_name: String) {
            MainActivity.dataStorage.editExercise(exercise_name, new_exercise_name, new_exercise_category)
            MainActivity.dataStorage.saveWorkoutData(ct!!)
            MainActivity.dataStorage.saveKnownExerciseData(ct!!)
            notifyDataSetChanged()
        }

        fun locallyDeleteExercise(position: Int, ct: Context) {
            // Delete Exercise from MainActivity Data Structures
            MainActivity.dataStorage.deleteExercise(Exercises!![position].Name)

            // Delete Exercise from Adapter's local data structure
            deleteExercise(Exercises!![position].Name)

            // Save Results
            MainActivity.dataStorage.saveKnownExerciseData(ct)
            MainActivity.dataStorage.saveWorkoutData(ct)
        }

        // Spinner On Item Selected Methods
        override fun onItemSelected(adapterView: AdapterView<*>, view: View, i: Int, l: Long) {
            new_exercise_category = adapterView.getItemAtPosition(i).toString()
        }

        override fun onNothingSelected(adapterView: AdapterView<*>) {
            new_exercise_category = ""
        }
    }
}