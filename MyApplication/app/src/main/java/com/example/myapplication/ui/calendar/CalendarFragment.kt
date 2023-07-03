package com.example.myapplication.ui.calendar

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.FragmentCalendarBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar
import kotlin.math.log

data class Task(val name: String, var completed: Boolean)
data class Collection(val date: String, var tasks: MutableList<Task>)

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
        val taskListView: RecyclerView = binding.tasksList
        taskListView.layoutManager = LinearLayoutManager(requireContext())
        val fileName = "tasks.json"
        copyAssetsToFile(requireContext(), fileName)

        var userCollection: MutableList<Collection> = readFromFile(fileName)
        var selectedDate: String = getToday()
        var sortedTasks: MutableList<Task> = getDailyTasksSorted(userCollection, selectedDate)
        var taskAdapter = TaskAdapter(sortedTasks)
        taskAdapter.setOnItemRemovedListener { removedTask ->
            try {
                val selectedCollection = userCollection.find { it.date == selectedDate }
                selectedCollection?.let { collection ->
                    val index = collection.tasks.indexOf(removedTask)
                    if (index != -1) {
                        collection.tasks.removeAt(index)
                        if (collection.tasks.isEmpty()) {
                            userCollection.remove(collection)
                        }
                        Log.d("JSON File", "setOnItemRemovedListener")
                        writeToFile(fileName, userCollection)
                        sortedTasks = getDailyTasksSorted(userCollection, selectedDate)
                        taskAdapter.updateData(sortedTasks)
                        taskAdapter.notifyDataSetChanged()
                    }
                }
            } catch (e: Exception) {
                Log.v("JSON File", "setOnItemRemovedListener $e")
            }
        }
        taskAdapter.setOnCheckedChangeListener { task, isChecked ->
            task.completed = isChecked
            Log.d("JSON File", "setOnCheckedChangeListener")
            writeToFile(fileName, userCollection)
            sortedTasks = getDailyTasksSorted(userCollection, selectedDate)
            taskAdapter.updateData(sortedTasks)
            taskAdapter.notifyDataSetChanged()
        }

        taskListView.adapter = taskAdapter
        initializeItemTouchHelper(taskAdapter)


        taskListView.visibility = if (sortedTasks.isEmpty()) View.GONE else View.VISIBLE

        binding.selectedDate.text = selectedDate

        binding.calendar.setOnDateChangeListener { _, year, month, dayOfMonth ->
            try{
            selectedDate = "${month + 1}/${dayOfMonth}/${year}"
            binding.selectedDate.text = selectedDate

            sortedTasks = getDailyTasksSorted(userCollection, selectedDate)
            taskAdapter.updateData(sortedTasks)
            taskAdapter.notifyDataSetChanged()
            Log.d("JSON File", "setOnDateChangeListener")
                Log.d("JSON File", "sortedTasks; $sortedTasks")
            writeToFile(fileName, userCollection)

            taskListView.visibility =
                if (sortedTasks.isEmpty()) View.GONE else View.VISIBLE // TODO: sortedTasks 말고 내부 확인?

//            initializeItemTouchHelper(taskAdapter)

            } catch (e: Exception) {
                Log.v("JSON File", "setOnDateChangeListener $e")
            }
        }

        ///////////////////////add new task/////////////////////////

        val newTextTask: EditText = binding.newTask

        newTextTask.setOnEditorActionListener { _, actionId, event ->
            try {
                if (actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                    val userInputName: String = newTextTask.text.toString()

                    if (userInputName.isNotEmpty()) {
                        val userInputTask = Task(userInputName, false)
                        userCollection = readFromFile(fileName)

                        val existingDate: Collection? =
                            userCollection.find { it.date == selectedDate }

                        if (existingDate != null) {
                            existingDate.tasks.add(userInputTask)
                        } else {
                            val newCollection =
                                Collection(selectedDate, mutableListOf(userInputTask))
                            userCollection.add(newCollection)
                        }

                        Log.d("JSON File", "setOnEditorActionListener")
                        writeToFile(fileName, userCollection)

                        sortedTasks = getDailyTasksSorted(userCollection, selectedDate)
//                        taskAdapter = TaskAdapter(sortedTasks)
//                        taskListView.adapter = taskAdapter
                        taskAdapter.updateData(sortedTasks)
                        taskAdapter.notifyDataSetChanged()

                        taskListView.visibility =
                            if (sortedTasks.isEmpty()) View.GONE else View.VISIBLE // TODO: sortedTasks 말고 내부 확인?

                    }

                    newTextTask.text.clear()
                    true
                } else {
                    Log.v("JSON File", "here?")
                    false
                }
            } catch (e: Exception) {
                Log.v("JSON File", "setOnEditorActionListener $e")
            }
            true
        }

        return root
    }

    private fun initializeItemTouchHelper(taskAdapter: TaskAdapter) {
        val itemTouchHelperCallback = ItemTouchHelperCallback(taskAdapter)
        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(binding.tasksList)
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
        printJsonFileData(fileName)
    }

    private fun getDailyTasksSorted(userCollection: MutableList<Collection>, date: String): MutableList<Task> {
        val matchingDate: Collection? = userCollection.find { it.date == date }
        val completeTasks: MutableList<Task> = matchingDate?.tasks?.filter { it.completed }?.map { it }?.toMutableList() ?: mutableListOf()
        val incompleteTasks: MutableList<Task> = matchingDate?.tasks?.filter { !it.completed }?.map { it }?.toMutableList() ?: mutableListOf()
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

    private fun getToday(): String {
        val calendar = Calendar.getInstance()
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        val month = calendar.get(Calendar.MONTH)
        val year = calendar.get(Calendar.YEAR)
        return "${month + 1}/${dayOfMonth}/${year}"
    }

    private fun printJsonFileData(fileName: String) {
        val file = File(requireContext().filesDir, fileName)
        val json = file.readText()
        Log.d("JSON File", json)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
