package com.example.myapplication.ui.calendar

import com.example.myapplication.R
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CalendarView
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.databinding.FragmentCalendarBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class Task(val name: String, val completed: Boolean)
data class Date(val date: String, val tasks: List<Task>)

class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
//        val calendarViewModel =
//                ViewModelProvider(this).get(CalendarViewModel::class.java)

        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val selectedDateTextView: TextView = binding.selectedDate
        val taskListView: ListView = binding.tasksList

        val gson = Gson()
        val jsonFileString = context?.assets?.open("tasks.json")?.bufferedReader().use { it?.readText() }
        val dates: List<Date> = gson.fromJson(jsonFileString, object : TypeToken<List<Date>>() {}.type)

        val calendarView: CalendarView = binding.calendar
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val selectedDate = "${month + 1}/${dayOfMonth}/${year}"
            selectedDateTextView.text = selectedDate

            val matchingDate: Date? = dates.find { it.date == selectedDate }
            val completeTasks: List<String> = matchingDate?.tasks?.filter { it.completed }?.map { it.name } ?: emptyList()
            val incompleteTasks: List<String> = matchingDate?.tasks?.filter { !it.completed }?.map { it.name } ?: emptyList()
            val sortedTasks: List<String> = incompleteTasks + completeTasks

            if (sortedTasks.isEmpty()) {
                taskListView.visibility = View.GONE
            } else {
                taskListView.visibility = View.VISIBLE
                val tasksAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, sortedTasks)
                taskListView.adapter = tasksAdapter
            }


        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}