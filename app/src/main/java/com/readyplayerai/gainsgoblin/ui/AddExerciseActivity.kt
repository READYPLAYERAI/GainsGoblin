package com.readyplayerai.gainsgoblin.ui

import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast

import com.readyplayerai.gainsgoblin.KeyboardHider
import com.readyplayerai.gainsgoblin.SnackBarWithMessage
import com.readyplayerai.gainsgoblin.adapters.AddExerciseWorkoutSetAdapter
import com.readyplayerai.gainsgoblin.adapters.ExerciseHistoryExerciseAdapter
import com.readyplayerai.gainsgoblin.R
import com.readyplayerai.gainsgoblin.model.WorkoutDay
import com.readyplayerai.gainsgoblin.model.WorkoutExercise
import com.readyplayerai.gainsgoblin.model.WorkoutSet
import com.readyplayerai.gainsgoblin.ui.charts.ChartPoint
import com.readyplayerai.gainsgoblin.ui.charts.LineChartView
import com.readyplayerai.gainsgoblin.ui.charts.OnPointSelectedListener

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AddExerciseActivity : AppCompatActivity() {

    // Helper Data Structures
    @JvmField var recyclerView: RecyclerView? = null

    // Add Exercise Activity Specifics
    var plus_reps: ImageButton? = null
    var minus_reps: ImageButton? = null
    var plus_weight: ImageButton? = null
    var minus_weight: ImageButton? = null

    // For Alarm
    @JvmField var START_TIME_IN_MILLIS: Long = 180000L
    @JvmField var countDownTimer: CountDownTimer? = null
    @JvmField var TimerRunning: Boolean = false
    @JvmField var TimeLeftInMillis: Long = 180000L

    // Timer Dialog Components
    @JvmField var et_seconds: EditText? = null
    var minus_seconds: ImageButton? = null
    var plus_seconds: ImageButton? = null
    @JvmField var bt_start: Button? = null
    @JvmField var bt_reset: Button? = null

    private var currentDialog: AlertDialog? = null

    // Comment Items
    var bt_save_comment: Button? = null
    var bt_clear_comment: Button? = null
    var et_exercise_comment: EditText? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_exercise)

        // find views
        et_reps = findViewById(R.id.et_reps)
        et_weight = findViewById(R.id.et_seconds)
        plus_reps = findViewById<ImageButton>(R.id.plus_reps)
        minus_reps = findViewById<ImageButton>(R.id.minus_reps)
        plus_weight = findViewById<ImageButton>(R.id.plus_weight)
        minus_weight = findViewById<ImageButton>(R.id.minus_weight)
        bt_clear = findViewById(R.id.bt_clear)
        bt_save = findViewById(R.id.bt_login_signup)

        // Self Explanatory I guess
        initActivity()

        // Self Explanatory I guess
        initrecyclerView()
    }

    // Save / Update
    fun clickSave(view: View?) {
        val keyboardHider = KeyboardHider(this)
        keyboardHider.hideKeyboard()

        // Save Functionality
        if (!(isEditMode!!)) {
            if (getText(et_weight!!).isEmpty() || getText(et_reps!!).isEmpty()) {
                Toast.makeText(getApplicationContext(), "Please write Weight and Reps", Toast.LENGTH_SHORT).show()
            } else {
                // Get user sets && reps
                val reps = java.lang.Double.parseDouble(getText(et_reps!!))
                val weight = java.lang.Double.parseDouble(getText(et_weight!!))

                // Create New Set Object
                val workoutSet = WorkoutSet(MainActivity.dateSelected, exercise_name, MainActivity.dataStorage.getExerciseCategory(exercise_name), reps, weight)

                // Ignore wrong input
                if (reps == 0.0 || weight == 0.0 || reps < 0.0 || weight < 0.0) {
                    Toast.makeText(getApplicationContext(), "Please write correct Weight and Reps", Toast.LENGTH_SHORT).show()
                }
                // Save set
                else {
                    // Find if workout day already exists
                    val position = MainActivity.dataStorage.getDayPosition(MainActivity.dateSelected)

                    // If workout day exists
                    if (position >= 0) {
                        // Find comment of that workout day/exercise
                        val workoutDay = MainActivity.dataStorage.workoutDays[position]

                        for (i in 0 until workoutDay.Sets!!.size) {
                            val workoutSet1 = workoutDay.Sets!![i]

                            if (workoutSet1.exerciseName!!.equals(exercise_name)) {
                                val exerciseComment = workoutSet1.comment
                                if (exerciseComment == "null" || exerciseComment == null) {
                                    workoutSet.comment = ""
                                } else {
                                    workoutSet.comment = exerciseComment
                                }
                            }
                        }

                        addSetExistingWorkoutDay(workoutSet, position)
                    }
                    // If not construct new workout day
                    else {
                        addSetNewWorkoutDay(workoutSet)
                    }
                }
            }

            // Fixed Myria induced bug
            Clicked_Set = Todays_Exercise_Sets.size - 1
        }
        // Update Functionality
        else {
            val to_be_updated_set = Todays_Exercise_Sets[Clicked_Set]

            to_be_updated_set.date
            to_be_updated_set.exerciseName

            // Find the set in main data structure and delete it
            for (i in 0 until MainActivity.dataStorage.workoutDays.size) {
                for (j in 0 until MainActivity.dataStorage.workoutDays[i].Sets!!.size) {
                    if (MainActivity.dataStorage.workoutDays[i].Sets!![j] == to_be_updated_set) {
                        val finalI = i
                        val finalJ = j

                        val reps = java.lang.Double.parseDouble(getText(et_reps!!))
                        val weight = java.lang.Double.parseDouble(getText(et_weight!!))

                        updateSet(finalI, finalJ, reps, weight)
                        break
                    }
                }
            }
        }
    }

    fun showSnackbarMessage(message: String) {
        runOnUiThread {
            val snackBarWithMessage = SnackBarWithMessage(this)
            snackBarWithMessage.showSnackbar(message)
        }
    }

    fun updateSet(finalI: Int, finalJ: Int, reps: Double, weight: Double) {
        MainActivity.dataStorage.workoutDays[finalI].Sets!![finalJ].reps = reps
        MainActivity.dataStorage.workoutDays[finalI].Sets!![finalJ].weight = weight

        // Manually update data because of bad design choices
        MainActivity.dataStorage.workoutDays[finalI].UpdateData()

        // Let the user know I guess
        runOnUiThread {
            showSnackbarMessage("Set Updated")
            updateTodaysExercises()
        }

        bt_save!!.setText("Save")
        bt_clear!!.setText("Clear")
        isEditMode = false
    }

    fun addSetExistingWorkoutDay(workoutSet: WorkoutSet, position: Int) {
        MainActivity.dataStorage.workoutDays[position].addSet(workoutSet)
        updateViewAndShowMessage()
    }

    fun addSetNewWorkoutDay(workoutSet: WorkoutSet) {
        val workoutDay = WorkoutDay()
        workoutDay.addSet(workoutSet)
        MainActivity.dataStorage.workoutDays.add(workoutDay)
        updateViewAndShowMessage()
    }

    fun updateViewAndShowMessage() {
        runOnUiThread {
            updateTodaysExercises()
            showSnackbarMessage("Set Added")
        }
    }

    // Clear
    fun clickClear(view: View?) {
        bt_clear!!.setText("Clear")
        et_reps!!.setText("")
        et_weight!!.setText("")
    }

    // Do I even need to explain this?
    fun clickPlusWeight(view: View?) {
        if (!getText(et_weight!!).isEmpty()) {
            var weight = java.lang.Double.parseDouble(getText(et_weight!!))
            weight = weight + 1.0
            et_weight!!.setText(weight.toString())
        } else {
            et_weight!!.setText("1.0")
        }
    }

    // Do I even need to explain this?
    fun clickPlusReps(view: View?) {
        if (!getText(et_reps!!).isEmpty()) {
            var reps = java.lang.Integer.parseInt(getText(et_reps!!))
            reps = reps + 1
            et_reps!!.setText(reps.toString())
        } else {
            et_reps!!.setText("1")
        }
    }

    // Do I even need to explain this?
    fun clickMinusWeight(view: View?) {
        if (!getText(et_weight!!).isEmpty()) {
            var weight = java.lang.Double.parseDouble(getText(et_weight!!))
            weight = weight - 1.0
            if (weight < 0.0) {
                weight = 0.0
            }
            et_weight!!.setText(weight.toString())
        }
    }

    // Do I even need to explain this?
    fun clickMinusReps(view: View?) {
        if (!getText(et_reps!!).isEmpty()) {
            var reps = java.lang.Integer.parseInt(getText(et_reps!!))
            reps = reps - 1
            if (reps < 0) {
                reps = 0
            }
            et_reps!!.setText(reps.toString())
        }
    }

    // Handles Intent Stuff
    fun initActivity() {
        val intent = getIntent()
        exercise_name = intent.getStringExtra("exercise")
        supportActionBar!!.setTitle(exercise_name)
    }

    // Initialize Recycler View Object
    fun initrecyclerView() {
        // Clear since we don't want duplicates
        Todays_Exercise_Sets.clear()

        // Find Sets for a specific date and exercise
        for (i in 0 until MainActivity.dataStorage.workoutDays.size) {
            // If date matches
            if (MainActivity.dataStorage.workoutDays[i].Date == MainActivity.dateSelected) {
                for (j in 0 until MainActivity.dataStorage.workoutDays[i].Sets!!.size) {
                    // If exercise matches
                    if (exercise_name == MainActivity.dataStorage.workoutDays[i].Sets!![j].exerciseName) {
                        Todays_Exercise_Sets.add(MainActivity.dataStorage.workoutDays[i].Sets!![j])
                    }
                }
            }
        }

        // Find Recycler View Object
        recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
        workoutSetAdapter2 = AddExerciseWorkoutSetAdapter(this, Todays_Exercise_Sets)
        recyclerView!!.setAdapter(workoutSetAdapter2)
        recyclerView!!.setLayoutManager(LinearLayoutManager(this))

        // Set Edit Text values to max set volume if possible
        initEditTexts()

        bt_clear!!.setText("Clear")

        // Initialize Integer position or else we get a crash
        Clicked_Set = Todays_Exercise_Sets.size - 1
    }

    // Set Edit Text values to max set volume if sets exist
    fun initEditTexts() {
        var max_weight = 0.0
        var max_reps = 0
        var max_exercise_volume = 0.0

        // Find Max Weight and Reps for a specific exercise
        for (i in 0 until MainActivity.dataStorage.workoutDays.size) {
            for (j in 0 until MainActivity.dataStorage.workoutDays[i].Sets!!.size) {
                if (MainActivity.dataStorage.workoutDays[i].Sets!![j].getVolume()!! > max_exercise_volume && MainActivity.dataStorage.workoutDays[i].Sets!![j].exerciseName == exercise_name) {
                    max_exercise_volume = MainActivity.dataStorage.workoutDays[i].Sets!![j].getVolume()!!
                    max_reps = java.lang.Math.round(MainActivity.dataStorage.workoutDays[i].Sets!![j].reps!!).toInt()
                    max_weight = MainActivity.dataStorage.workoutDays[i].Sets!![j].weight!!
                }
            }
        }

        // If never performed the exercise leave Edit Texts blank
        if (max_reps == 0 || max_weight == 0.0) {
            et_reps!!.setText("")
            et_weight!!.setText("")
        } else {
            et_reps!!.setText(max_reps.toString())
            et_weight!!.setText(max_weight.toString())
        }
    }

    // Save Changes in main data structure, save data structure in shared preferences
    override fun onStop() {
        super.onStop()
        // Sort Before Saving
        MainActivity.dataStorage.sortWorkoutDaysDate()

        // Actually Save Changes in shared preferences
        MainActivity.dataStorage.saveWorkoutData(getApplicationContext())
    }

    // Menu Stuff
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = getMenuInflater()
        inflater.inflate(R.menu.add_exercise_activity_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(@NonNull item: MenuItem): Boolean {
        // Timer
        if (item.itemId == R.id.timer) {
            setupTimer()
        }
        // Exercise History
        else if (item.itemId == R.id.history) {
            return setupExerciseHistory(item)
        }
        // Exercise Stats Chart
        else if (item.itemId == R.id.graph) {
            // Prepare to show exercise history dialog box
            val inflater = LayoutInflater.from(this)
            val view = inflater.inflate(R.layout.exercise_graph_dialog, null)
            val alertDialog = AlertDialog.Builder(this)
                    .setView(view)
                    .create()

            // Get Chart Object
            val lineChart = view.findViewById<LineChartView>(R.id.lineChart)

            // Create Array List that will hold graph data
            val volumeValues = ArrayList<ChartPoint>()
            val maxWeightValues = ArrayList<ChartPoint>()
            val totalRepsValues = ArrayList<ChartPoint>()

            val workoutDates = ArrayList<String>()
            val workoutMonths = ArrayList<String>()

            var x = 0

            // Get Exercise Volume
            for (i in 0 until MainActivity.dataStorage.workoutDays.size) {
                for (j in 0 until MainActivity.dataStorage.workoutDays[i].Exercises!!.size) {
                    val currentExercise = MainActivity.dataStorage.workoutDays[i].Exercises!![j]

                    if (currentExercise.Exercise == exercise_name) {
                        volumeValues.add(ChartPoint(x.toFloat(), currentExercise.Volume!!.toFloat()))

                        maxWeightValues.add(ChartPoint(x.toFloat(), currentExercise.MaxWeight!!.toFloat()))

                        totalRepsValues.add(ChartPoint(x.toFloat(), currentExercise.TotalReps!!.toFloat()))

                        // Add the date corresponding to the data point
                        workoutDates.add(MainActivity.dataStorage.workoutDays[i].Date!!)

                        // Add the month corresponding to the data point
                        val date = MainActivity.dataStorage.workoutDays[i].Date!!
                        val month = getMonthFromDateString(date)
                        workoutMonths.add(month)

                        x++
                    }
                }
            }

            if (x == 0) {
                val snackBarWithMessage = SnackBarWithMessage(this)
                snackBarWithMessage.showSnackbar("No data found")
                return super.onOptionsItemSelected(item)
            }

            // Chart Data Spinner
            val graph_value_spinner = view.findViewById<Spinner>(R.id.graph_spinner)
            val graph_value_spinner_options = arrayOf("Volume", "Weight", "Reps")
            val adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, graph_value_spinner_options)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            graph_value_spinner.setAdapter(adapter)
            graph_value_spinner.setSelection(0)

            // Chart Timeframe Spinner
            val graph_time_spinner = view.findViewById<Spinner>(R.id.graph_time_spinner)
            val graph_time_spinner_options = arrayOf("All Time", "Last Year", "6 months", "3 months", "1 month")
            val adapter1 = ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, graph_time_spinner_options)
            adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            graph_time_spinner.setAdapter(adapter1)
            graph_time_spinner.setSelection(0)

            graph_value_spinner.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(adapterView: AdapterView<*>?, view: View?, i: Int, l: Long) {
                    val selectedOption1 = graph_time_spinner.getSelectedItem().toString()
                    val selectedOption2 = graph_value_spinner_options[i]

                    setupChartWithOptions(selectedOption1, selectedOption2, volumeValues, maxWeightValues, totalRepsValues, workoutDates, workoutMonths, alertDialog, lineChart)
                }

                override fun onNothingSelected(adapterView: AdapterView<*>?) {
                    // Do nothing
                }
            })

            graph_time_spinner.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(adapterView: AdapterView<*>?, view: View?, i: Int, l: Long) {
                    val selectedOption1 = graph_time_spinner_options[i]
                    val selectedOption2 = graph_value_spinner.getSelectedItem().toString()

                    setupChartWithOptions(selectedOption1, selectedOption2, volumeValues, maxWeightValues, totalRepsValues, workoutDates, workoutMonths, alertDialog, lineChart)
                }

                override fun onNothingSelected(adapterView: AdapterView<*>?) {
                    // Do nothing
                }
            })

            // Default option is volume
            setupLineChart(alertDialog, volumeValues, lineChart, workoutMonths, workoutDates, "kg", false)
        }
        // Exercise Comments
        else if (item.itemId == R.id.comment) {
            // Prepare to show exercise history dialog box
            val inflater = LayoutInflater.from(this)
            val view = inflater.inflate(R.layout.add_exercise_comment_dialog, null)
            val alertDialog = AlertDialog.Builder(this).setView(view).create()

            bt_save_comment = view.findViewById<Button>(R.id.bt_save_comment)
            bt_clear_comment = view.findViewById<Button>(R.id.bt_clear_comment)
            et_exercise_comment = view.findViewById<EditText>(R.id.et_exercise_comment)

            // Check if exercise exists (to show the comment if it has one)
            // Find if workout day already exists
            val exercise_position = MainActivity.dataStorage.getExercisePosition(MainActivity.dateSelected, exercise_name)

            // Exists, then show the comment
            if (exercise_position >= 0) {
                println("We can comment, exercise exists")

                val day_position = MainActivity.dataStorage.getDayPosition(MainActivity.dateSelected)

                val comment = MainActivity.dataStorage.workoutDays[day_position].Exercises!![exercise_position].Comment

                et_exercise_comment!!.setText(comment)
            }

            bt_clear_comment!!.setOnClickListener { v ->
                clearComment()
            }

            bt_save_comment!!.setOnClickListener { v ->
                saveComment(alertDialog)
            }

            // Show Chart Dialog box
            alertDialog.show()
        }

        return super.onOptionsItemSelected(item)
    }

    private fun setupChartWithOptions(selectedOption1: String, selectedOption2: String, volumeValues: ArrayList<ChartPoint>, maxWeightValues: ArrayList<ChartPoint>, totalRepsValues: ArrayList<ChartPoint>, workoutDates: ArrayList<String>, workoutMonths: ArrayList<String>, alertDialog: AlertDialog?, lineChart: LineChartView?) {
        var label = String()
        var actualValues = ArrayList<ChartPoint>()
        var finalValues = ArrayList<ChartPoint>()

        if (selectedOption2 == "Weight") {
            actualValues = maxWeightValues
            label = "kg"
        } else if (selectedOption2 == "Volume") {
            actualValues = volumeValues
            label = "kg"
        } else if (selectedOption2 == "Reps") {
            actualValues = totalRepsValues
            label = "reps"
        }

        var index = -1

        if (selectedOption1 == "All Time") {
            index = 0
        } else if (selectedOption1 == "Last Year") {
            index = getIndexOfLastXMonths(workoutDates, 12)
        } else if (selectedOption1 == "6 months") {
            index = getIndexOfLastXMonths(workoutDates, 6)
        } else if (selectedOption1 == "3 months") {
            index = getIndexOfLastXMonths(workoutDates, 3)
        } else if (selectedOption1 == "1 month") {
            index = getIndexOfLastXMonths(workoutDates, 1)
        }

        // Show all
        if (index == -1) {
            index = actualValues.size
        }

        finalValues = ArrayList(actualValues.subList(index, actualValues.size))
        val finalMonths = ArrayList(workoutMonths.subList(index, workoutMonths.size))
        setupLineChart(alertDialog, finalValues, lineChart, finalMonths, workoutDates, label, true)
    }

    private fun getIndexOfLastXMonths(workoutDates: ArrayList<String>, months: Int): Int {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, -months)
        val oneMonthBefore = calendar.time

        val dateFormat = SimpleDateFormat("yyyy-MM-dd")
        val givenDate = dateFormat.format(oneMonthBefore)
        return findFirstDateAfter(workoutDates, givenDate)
    }

    fun findFirstDateAfter(workoutDates: ArrayList<String>, givenDate: String): Int {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd")
        val givenDateObj: Date?
        try {
            givenDateObj = dateFormat.parse(givenDate)
        } catch (e: ParseException) {
            e.printStackTrace()
            return -1 // If the given date cannot be parsed
        }

        for (i in 0 until workoutDates.size) {
            val currentDateObj: Date?
            try {
                currentDateObj = dateFormat.parse(workoutDates[i])
            } catch (e: ParseException) {
                e.printStackTrace()
                continue
            }

            if (currentDateObj!!.after(givenDateObj)) {
                return i
            }
        }

        return -1 // If no date is found after the given date
    }

    private fun getMonthFromDateString(dateString: String): String {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = sdf.parse(dateString)
            val monthFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
            return monthFormat.format(date)
        } catch (e: ParseException) {
            e.printStackTrace()
            return ""
        }
    }

    fun setupExerciseHistory(item: MenuItem): Boolean {
        // Prepare to show exercise history dialog box
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.exercise_history_dialog, null)
        val alertDialog = AlertDialog.Builder(this).setView(view).create()

        // Declare local data structure
        val allPerformedSessions = ArrayList<WorkoutExercise>()

        // Find all performed sessions of a specific exercise and add them to local data structure
        for (i in MainActivity.dataStorage.workoutDays.size - 1 downTo 0) {
            for (j in 0 until MainActivity.dataStorage.workoutDays[i].Exercises!!.size) {
                if (MainActivity.dataStorage.workoutDays[i].Exercises!![j].Exercise == exercise_name) {
                    allPerformedSessions.add(MainActivity.dataStorage.workoutDays[i].Exercises!![j])
                }
            }
        }

        if (allPerformedSessions.size == 0) {
            val snackBarWithMessage = SnackBarWithMessage(this)
            snackBarWithMessage.showSnackbar("No data found")
            return super.onOptionsItemSelected(item)
        }

        // Set Exercise Name
        val tv_exercise_name = view.findViewById<TextView>(R.id.tv_exercise_name)
        tv_exercise_name.setText(exercise_name)

        // Set Exercise History Recycler View
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView_Exercise_History)
        val workoutExerciseAdapter4 = ExerciseHistoryExerciseAdapter(this, allPerformedSessions)

        // Crash Here
        recyclerView.setAdapter(workoutExerciseAdapter4)
        recyclerView.setLayoutManager(LinearLayoutManager(this))

        alertDialog.show()

        return super.onOptionsItemSelected(item)
    }

    fun setupLineChart(alertDialog: AlertDialog?, volumeValues: ArrayList<ChartPoint>, lineChart: LineChartView?, workoutMonths: ArrayList<String>, workoutDates: ArrayList<String>, label: String, showYaxis: Boolean) {
        lineChart?.setData(volumeValues, workoutMonths)
        lineChart?.setOnPointSelectedListener(object : OnPointSelectedListener {
            override fun onPointSelected(index: Int, x: Float, y: Float) {
                popupChartAlertDialog(index, y, workoutMonths, label)
            }
        })
        alertDialog?.show()
    }

    fun popupChartAlertDialog(xIndex: Int, yValue: Float, workoutMonths: ArrayList<String>, label: String) {
        // Dismiss the current dialog if it is showing
        if (currentDialog != null && currentDialog!!.isShowing()) {
            currentDialog!!.dismiss()
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle(workoutMonths[xIndex])

        val inflater = LayoutInflater.from(this)
        val dialogView = inflater.inflate(R.layout.custom_alert_dialog, null)
        builder.setView(dialogView)

        val messageTextView = dialogView.findViewById<TextView>(R.id.message_text_view)
        messageTextView.setText(yValue.toString() + " " + label)

        val dialog = builder.create()

        // Set custom width (in pixels)
        val customWidth = 600 // You can change this value to your desired width

        // Set the layout parameters
        val layoutParams = WindowManager.LayoutParams()
        layoutParams.copyFrom(dialog.getWindow()!!.getAttributes())
        layoutParams.width = customWidth
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT

        dialog.getWindow()!!.setBackgroundDrawable(ColorDrawable(Color.WHITE))

        // Show the dialog
        dialog.show()

        // Apply the layout parameters
        dialog.getWindow()!!.setAttributes(layoutParams)

        // Set the current dialog to the newly created dialog
        currentDialog = dialog
    }

    fun setupTimer() {
        // Prepare to show timer dialog box
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.timer_dialog, null)
        val alertDialog = AlertDialog.Builder(this).setView(view).create()

        // Get Objects (use view because dialog box from menu)
        et_seconds = view.findViewById<EditText>(R.id.et_seconds)
        minus_seconds = view.findViewById<ImageButton>(R.id.minus_seconds)
        plus_seconds = view.findViewById<ImageButton>(R.id.plus_seconds)
        bt_start = view.findViewById<Button>(R.id.bt_start)
        bt_reset = view.findViewById<Button>(R.id.bt_close)

        // Set default seconds value to 180 i.e 3 minutes
        if (!TimerRunning) {
            // Derive String value from chosen start time
            // et_seconds.setText(String.valueOf((int) START_TIME_IN_MILLIS /1000));
            loadSeconds()
        } else {
            updateCountDownText()
        }

        // Reset Timer Button
        bt_reset!!.setOnClickListener { view ->
            resetTimer()
        }

        // Start Timer Button
        bt_start!!.setOnClickListener { view ->
            if (TimerRunning) {
                pauseTimer()
            } else {
                saveSeconds()
                startTimer()
            }
        }

        // Minus Button
        minus_seconds!!.setOnClickListener { view ->
            if (!getText(et_seconds!!).isEmpty()) {
                var seconds = java.lang.Double.parseDouble(getText(et_seconds!!))
                seconds = seconds - 1.0
                if (seconds < 0.0) {
                    seconds = 0.0
                }
                val seconds_int = seconds.toInt()
                et_seconds!!.setText(seconds_int.toString())
            }
        }

        // Plus Button
        plus_seconds!!.setOnClickListener { view ->
            if (!getText(et_seconds!!).isEmpty()) {
                var seconds = java.lang.Double.parseDouble(getText(et_seconds!!))
                seconds = seconds + 1.0
                if (seconds < 0.0) {
                    seconds = 0.0
                }
                val seconds_int = seconds.toInt()
                et_seconds!!.setText(seconds_int.toString())
            }
        }

        // Show Timer Dialog Box
        alertDialog.show()
    }

    // Makes necessary checks and saves comment
    fun saveComment(alertDialog: AlertDialog?) {
        // Check if exercise exists (cannot comment on non-existant exercise)
        // Find if workout day already exists
        val exercise_position = MainActivity.dataStorage.getExercisePosition(MainActivity.dateSelected, exercise_name)

        if (exercise_position >= 0) {
            println("We can comment, exercise exists")
        } else {
            println("We can't comment, exercise doesn't exist")
            Toast.makeText(getApplicationContext(), "Can't comment without sets", Toast.LENGTH_SHORT).show()
            return
        }

        var comment: String

        if (getText(et_exercise_comment!!).isEmpty()) {
            comment = ""
        } else {
            comment = getText(et_exercise_comment!!) // Get user comment
        }

        // Get the date for today
        val day_position = MainActivity.dataStorage.getDayPosition(MainActivity.dateSelected)

        // Modify the data structure to add the comment
        MainActivity.dataStorage.workoutDays[day_position].Exercises!![exercise_position].Comment = comment

        val finalSize = MainActivity.dataStorage.workoutDays[day_position].Exercises!![exercise_position].Sets!!.size

        // Also modify individual sets
        for (i in 0 until MainActivity.dataStorage.workoutDays[day_position].Exercises!![exercise_position].Sets!!.size) {
            val finalI = i

            val set_to_be_updated = MainActivity.dataStorage.workoutDays[day_position].Exercises!![exercise_position].Sets!![i]
            set_to_be_updated.comment = comment

            updateCommentInSet(day_position, exercise_position, finalI, finalSize, comment)
            alertDialog!!.dismiss()
        }
    }

    fun updateCommentInSet(day_position: Int, exercise_position: Int, finalI: Int, finalSize: Int, comment: String) {
        MainActivity.dataStorage.workoutDays[day_position].Exercises!![exercise_position].Sets!![finalI].comment = comment

        runOnUiThread {
            updateTodaysExercises()
        }

        if (finalI == finalSize - 1) // Show popup only when last set comment is saved
        {
            showSnackbarMessage("Comment Logged")
        }
    }

    // Makes necessary checks and clears comment
    fun clearComment() {
        et_exercise_comment!!.setText("")
    }

    fun startTimer() {
        countDownTimer = object : CountDownTimer(TimeLeftInMillis, 1000L) {
            override fun onTick(MillisUntilFinish: Long) {
                TimeLeftInMillis = MillisUntilFinish
                updateCountDownText()
            }

            override fun onFinish() {
                TimerRunning = false
                bt_start!!.setText("Start")
            }
        }.start()

        TimerRunning = true
        bt_start!!.setText("Pause")
    }

    fun loadSeconds() {
        val sharedPreferences = getSharedPreferences("shared preferences", MODE_PRIVATE)
        val seconds = sharedPreferences.getString("seconds", "180")

        // Change actual values that timer uses
        START_TIME_IN_MILLIS = java.lang.Integer.parseInt(seconds) * 1000L
        TimeLeftInMillis = START_TIME_IN_MILLIS

        et_seconds!!.setText(seconds)
    }

    fun saveSeconds() {
        val sharedPreferences = getSharedPreferences("shared preferences", MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        if (!getText(et_seconds!!).isEmpty()) {
            val seconds = getText(et_seconds!!)

            // Change actual values that timer uses
            START_TIME_IN_MILLIS = java.lang.Integer.parseInt(seconds) * 1000L
            TimeLeftInMillis = START_TIME_IN_MILLIS

            // Save to shared preferences
            editor.putString("seconds", getText(et_seconds!!))
            editor.apply()
        }
    }

    fun pauseTimer() {
        countDownTimer!!.cancel()
        TimerRunning = false
        bt_start!!.setText("Start")
    }

    fun resetTimer() {
        if (TimerRunning) {
            pauseTimer()
            TimeLeftInMillis = START_TIME_IN_MILLIS
            updateCountDownText()
        }
    }

    fun updateCountDownText() {
        val seconds = (TimeLeftInMillis / 1000L).toInt()
        val minutes = (seconds / 60).toInt()
        et_seconds!!.setText(seconds.toString())
    }

    companion object {
        @JvmField var exercise_name: String? = null
        @JvmField var Todays_Exercise_Sets: ArrayList<WorkoutSet> = ArrayList()
        @JvmField var workoutSetAdapter2: AddExerciseWorkoutSetAdapter? = null
        @JvmField var Clicked_Set: Int = 0
        @JvmField var isEditMode: Boolean? = false
        lateinit var et_reps: EditText
        lateinit var et_weight: EditText
        lateinit var bt_save: Button
        lateinit var bt_clear: Button

        private fun getText(et: EditText): String = et.getText()!!.toString()

        @JvmStatic fun deleteSet(ct: Context) {
            // Show confirmation dialog box
            // Prepare to show exercise dialog box
            val inflater = LayoutInflater.from(ct)
            val view1 = inflater.inflate(R.layout.delete_set_dialog, null)
            val alertDialog = AlertDialog.Builder(ct).setView(view1).create()

            val bt_yes = view1.findViewById<Button>(R.id.bt_yes3)
            val bt_no = view1.findViewById<Button>(R.id.bt_no3)

            // Dismiss dialog box
            bt_no.setOnClickListener { view ->
                alertDialog.dismiss()
            }

            // Actually Delete set and update local data structure
            bt_yes.setOnClickListener { view ->
                // Get soon to be deleted set
                val to_be_removed_set = Todays_Exercise_Sets[Clicked_Set]

                // Find the set in main data structure and delete it
                for (i in 0 until MainActivity.dataStorage.workoutDays.size) {
                    if (MainActivity.dataStorage.workoutDays[i].Sets!!.contains(to_be_removed_set)) {
                        val finalI = i

                        deleteSetLogic(ct, finalI, to_be_removed_set)
                        alertDialog.dismiss()
                        break
                    }
                }
            }

            // Show delete confirmation dialog box
            alertDialog.show()
        }

        @JvmStatic fun deleteSetLogic(ct: Context, finalI: Int, to_be_removed_set: WorkoutSet) {
            MainActivity.dataStorage.workoutDays[finalI].removeSet(to_be_removed_set)

            // Cleanup potential days with 0 sets
            for (i in 0 until MainActivity.dataStorage.workoutDays.size) {
                if (MainActivity.dataStorage.workoutDays[i].Sets!!.size == 0) {
                    MainActivity.dataStorage.workoutDays.removeAt(i)
                }
            }

            // Bug: If this last set of the day this is required
            MainActivity.dataStorage.saveWorkoutData(ct)
            MainActivity.dataStorage.saveKnownExerciseData(ct)

            (ct as Activity).runOnUiThread {
                val snackBarWithMessage = SnackBarWithMessage(ct)
                snackBarWithMessage.showSnackbar("Set Deleted")
                updateTodaysExercises()
            }
        }

        @JvmStatic fun editSet(holder: AddExerciseWorkoutSetAdapter.MyViewHolder?, view: View?, position: Int) {
            println("Edit Set on position " + position + " clicked")

            bt_clear!!.setText("Delete")
            bt_save!!.setText("Update")

            // Populate Edit Texts
            Clicked_Set = position
            UpdateViewOnClick()

            isEditMode = true
        }

        // Update this activity when a set is clicked
        @JvmStatic fun UpdateViewOnClick() {
            // Get selected set
            val clicked_set = Todays_Exercise_Sets[Clicked_Set]

            // Update Edit Texts
            et_weight!!.setText(clicked_set.weight!!.toString())
            et_reps!!.setText(clicked_set.reps!!.toInt().toString())
        }

        // Updates Local Data Structure
        @JvmStatic fun updateTodaysExercises() {
            // Clear since we don't want duplicates
            Todays_Exercise_Sets.clear()

            // Find Sets for a specific date and exercise
            for (i in 0 until MainActivity.dataStorage.workoutDays.size) {
                // If date matches
                if (MainActivity.dataStorage.workoutDays[i].Date == MainActivity.dateSelected) {
                    for (j in 0 until MainActivity.dataStorage.workoutDays[i].Sets!!.size) {
                        // If exercise matches
                        if (exercise_name == MainActivity.dataStorage.workoutDays[i].Sets!![j].exerciseName) {
                            Todays_Exercise_Sets.add(MainActivity.dataStorage.workoutDays[i].Sets!![j])
                        }
                    }
                }
            }

            bt_clear!!.setText("Clear")

            // Update Recycler View
            workoutSetAdapter2!!.notifyDataSetChanged()
        }
    }
}