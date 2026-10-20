package com.readyplayerai.gainsgoblin.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.readyplayerai.gainsgoblin.R
import com.readyplayerai.gainsgoblin.model.WorkoutDay
import com.readyplayerai.gainsgoblin.model.WorkoutExercise

import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date

class ViewPagerWorkoutDayAdapter(
    ct: Context?,
    infiniteDays: List<WorkoutDay>?,
    realDays: List<WorkoutDay>? = emptyList(),
    private val onExerciseClick: ((List<WorkoutExercise>, Int) -> Unit)? = null,
    private val categoryFor: ((String) -> String?)? = null
) : RecyclerView.Adapter<ViewPagerWorkoutDayAdapter.WorkoutDayViewHolder>() {

    private val Workout_Days = ArrayList(infiniteDays)
    private val ct = ct
    private val Real_Days: List<WorkoutDay> = realDays ?: emptyList()

    @NonNull
    override fun onCreateViewHolder(@NonNull parent: ViewGroup, viewType: Int): WorkoutDayViewHolder {
        return WorkoutDayViewHolder(
                LayoutInflater.from(parent.context).inflate(
                        R.layout.item_view_pager,
                        parent,
                        false
                )
        )
    }

    override fun onBindViewHolder(@NonNull holder: WorkoutDayViewHolder, position: Int) {
        // Set Data Method used to be here :D
        val Date_Str1 = Workout_Days[position].Date

        // Find which exercises were performed that given date
        val Today_Execrises: ArrayList<WorkoutExercise> =
            Real_Days.firstOrNull { it.Date == Date_Str1 }?.Exercises ?: ArrayList()

        // Set Recycler View
        val workoutExerciseAdapter = ViewPagerExerciseAdapter(
            ct,
            Today_Execrises,
            onExerciseClick = { position -> onExerciseClick?.invoke(Today_Execrises, position) },
            categoryFor = categoryFor
        )
        holder.recyclerView_Main.adapter = workoutExerciseAdapter
        holder.recyclerView_Main.layoutManager = LinearLayoutManager(ct)

        // Convert Date To Something Sensible
        try {
            val date1 = SimpleDateFormat("yyyy-MM-dd").parse(Date_Str1!!) //potential exception
            val date2 = SimpleDateFormat("EEEE") as DateFormat
            val date3 = SimpleDateFormat("MMMM dd yyyy") as DateFormat
            val Date_Str2 = date2.format(date1)
            val Date_Str3 = date3.format(date1)
            holder.tv_date.text = Date_Str2
            holder.tv_full_date.text = Date_Str3
        } catch (e: ParseException) {
            e.printStackTrace()
        }

        // Navigate to Exercises Activity with specific date
        holder.date_bg.setOnClickListener { view ->
//                Intent in = new Intent(ct, DayActivity.class);
//                in.putExtra("date", Date_Str1);
//                ct.startActivity(in);
        }

        holder.img_bt_next.setOnClickListener { view ->
            println("Next!")
        }

        holder.img_bt_back.setOnClickListener { view ->
            println("Back!")
        }
    }

    override fun getItemCount(): Int {
        return Workout_Days.size
    }

    // Magic Happens here
    class WorkoutDayViewHolder(@NonNull itemView: View) : RecyclerView.ViewHolder(itemView) {
        var tv_date: TextView = itemView.findViewById(R.id.tv_date)
        var tv_full_date: TextView = itemView.findViewById(R.id.tv_full_date)
        var recyclerView_Main: RecyclerView = itemView.findViewById(R.id.recyclerView_Main)
        var cardview_viewpager: CardView = itemView.findViewById(R.id.cardview_viewpager)
        var date_bg: ConstraintLayout = itemView.findViewById(R.id.date_bg) // Used for navigating to AddExerciseActivity with date
        var img_bt_back: ImageButton = itemView.findViewById(R.id.img_bt_back)
        var img_bt_next: ImageButton = itemView.findViewById(R.id.img_bt_next)
    }
}