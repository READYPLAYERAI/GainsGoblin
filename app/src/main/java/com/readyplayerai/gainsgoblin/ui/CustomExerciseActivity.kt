package com.readyplayerai.gainsgoblin.ui

import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast

import com.readyplayerai.gainsgoblin.model.Exercise
import com.readyplayerai.gainsgoblin.R

class CustomExerciseActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {

    var selected_category: String? = null
    var spinner: Spinner? = null
    var et_exercise_name: EditText? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom_exercise)

        // Initialize Spinner Object
        spinner = findViewById<Spinner>(R.id.spinner)
        val adapter = ArrayAdapter.createFromResource(this, R.array.Categories, android.R.layout.simple_spinner_item)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner!!.setAdapter(adapter)
        spinner!!.setOnItemSelectedListener(this)

        // Initialize Edit Text Object
        et_exercise_name = findViewById<EditText>(R.id.et_exercise_name)
    }

    // Menu Stuff
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = getMenuInflater()
        inflater.inflate(R.menu.custom_exercise_activity_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(@NonNull item: MenuItem): Boolean {
        if (item.itemId == R.id.save) {
            if (!MainActivity.dataStorage.doesExerciseExist(et_exercise_name!!.text.toString())) {
                val new_exercise = Exercise(et_exercise_name!!.text.toString(), selected_category!!)
                MainActivity.dataStorage.knownExercises.add(new_exercise)
                MainActivity.dataStorage.saveKnownExerciseData(getApplicationContext())
                Toast.makeText(getApplicationContext(), "Exercise Saved", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, ExercisesActivity::class.java)
                startActivity(intent)
            } else {
                Toast.makeText(getApplicationContext(), "Exercise Already Exists", Toast.LENGTH_SHORT).show()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    // On Selected Stuff (spinner)
    override fun onItemSelected(adapterView: AdapterView<*>?, view: View?, i: Int, l: Long) {
        selected_category = adapterView!!.getItemAtPosition(i).toString()
    }

    override fun onNothingSelected(adapterView: AdapterView<*>?) {
        selected_category = ""
    }
}