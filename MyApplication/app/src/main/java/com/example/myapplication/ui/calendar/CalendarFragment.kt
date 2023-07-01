package com.example.myapplication.ui.calendar

import android.content.Context
import com.example.myapplication.R
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CalendarView
import android.widget.EditText
import android.widget.ListView
import android.widget.SearchView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.databinding.FragmentCalendarBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileOutputStream

data class Task(val name: String, var completed: Boolean)
data class Collection(val date: String, var tasks: List<Task>)

class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!
    private val gson = Gson()

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {

        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        val root: View = binding.root
        val selectedDateTextView: TextView = binding.selectedDate
        val taskListView: ListView = binding.tasksList
//        val calendarView: CalendarView = binding.calendar

        lateinit var selectedDate: String

        val fileName = "tasks.json"
        copyAssetsToFile(requireContext(), fileName)

        val userCollection: MutableList<Collection> = readFromFile(fileName)

        binding.calendar.setOnDateChangeListener { _, year, month, dayOfMonth ->
            selectedDate = "${month + 1}/${dayOfMonth}/${year}"
            selectedDateTextView.text = selectedDate

            val sortedTasks: MutableList<String> = getDailyTasksSorted(userCollection, selectedDate)

            taskListView.visibility = if (sortedTasks.isEmpty()) View.GONE else View.VISIBLE
            taskListView.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, sortedTasks)
        }

        ///////////////////////add new task/////////////////////////

        val newTextTask: EditText = binding.newTask

        newTextTask.setOnEditorActionListener { _, actionId, event ->

            if (actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                val userInputName: String = newTextTask.text.toString()

                if (userInputName.isNotEmpty()) {
                    val userInputTask = Task(userInputName, false)
                    val userCollection = readFromFile(fileName)

                    val existingDate: Collection? = userCollection.find<Collection> { it.date == selectedDate }

                    if (existingDate != null) {
                        existingDate.tasks = existingDate.tasks + userInputTask
                        println("exist!")
                    } else {
                        val newDate = Collection(selectedDate, listOf(userInputTask))
                        userCollection.add(newDate)
                        println("make new")
                    }

                    writeToFile(fileName, userCollection)

                    val sortedTasks: MutableList<String> = getDailyTasksSorted(userCollection, selectedDate)
                    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, sortedTasks)
                    taskListView.adapter = adapter
                    adapter.notifyDataSetChanged()
                }

                newTextTask.text.clear()
                true
            } else {
                false
            }
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun readFromFile(fileName: String): MutableList<Collection> {
        val file = File(requireContext().filesDir, fileName)
        val json = file.readText()
        return gson.fromJson(json, object : TypeToken<MutableList<Collection>>() {}.type)
    }

    private fun writeToFile(fileName: String, data: MutableList<Collection>) {
        val file = File(requireContext().filesDir, fileName)
        val json = gson.toJson(data)
        file.writeText(json)
    }

    private fun getDailyTasksSorted(userCollection: MutableList<Collection>, date: String): MutableList<String> {
        val matchingDate: Collection? = userCollection.find { it.date == date }
        val completeTasks: MutableList<String> = matchingDate?.tasks?.filter { it.completed }?.map { it.name }?.toMutableList() ?: mutableListOf()
        val incompleteTasks: MutableList<String> = matchingDate?.tasks?.filter { !it.completed }?.map { it.name }?.toMutableList() ?: mutableListOf()
        return (incompleteTasks + completeTasks).toMutableList()
    }

    private fun copyAssetsToFile(context: Context, fileName: String) {
        val file = File(context.filesDir, fileName)
        if (file.exists()) {
            return
        }

        val assetManager = context.assets
        val inputStream = assetManager.open(fileName)
        val outputStream = FileOutputStream(file)

        inputStream.copyTo(outputStream)

        inputStream.close()
        outputStream.close()
    }

}
