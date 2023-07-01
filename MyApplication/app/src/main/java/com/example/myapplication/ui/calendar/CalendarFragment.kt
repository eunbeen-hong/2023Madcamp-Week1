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
import android.widget.ListAdapter
import android.widget.ListView
import android.widget.SearchView
import android.widget.TextView
import androidx.annotation.CallSuper
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
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
        val taskListView: RecyclerView = binding.tasksList
        taskListView.layoutManager = LinearLayoutManager(requireContext())
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
            taskListView.adapter = TaskAdapter(sortedTasks)
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
                    } else {
                        val newDate = Collection(selectedDate, listOf(userInputTask))
                        userCollection.add(newDate)
                    }

                    writeToFile(fileName, userCollection)

                    val sortedTasks: MutableList<String> = getDailyTasksSorted(userCollection, selectedDate)
                    taskListView.adapter = TaskAdapter(sortedTasks)
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
        return gson.fromJson(json, Array<Collection>::class.java).toMutableList()
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

class TaskAdapter(private val tasks: List<String>) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
        return TaskViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        holder.bind(task)
    }

    override fun getItemCount(): Int {
        return tasks.size
    }

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private  val taskTestView: TextView = itemView.findViewById(android.R.id.text1)

        fun bind(task: String) {
            taskTestView.text = task
        }
    }
}



//    fun removeItem(position: Int): T? {
//        if (position >= itemCount) return null
//        valitem = currentList[position]
//        removedItems.add(item)
//        val actualList = currentList - removedItems
//        if(actualList.isEmpty()) removedItems.clear()
//        submit(actualList, true)
//        return item
//    }
//
//    private fun submit(list: List<T>?, isLocalSubmit: Boolean) {
//        if(!isLocalSubmit) removedItems.clear()
//        super.submitList(list)
//    }
//
//    @CallSuper
//    override fun submitList(list: List<T>?) {
//        submit(list, false)
//    }
//abstract class  ListAdapterSwipeable<T, VH: RecyclerView.ViewHolder> (
//    diffCallback: DiffUtil.ItemCallback<T>
//        ): ListAdapter<T, VH>(diffCallback) {
//            ...
//        }
//
//class ItemSwipeHandler<T>(
//    private val adapter: ListAdapterSwipeable<T, *>,
//    private val onItemRemoved: ((item: T) -> Unit)? = null
//): ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
//
//}
//
//override fun onMove(
//    recyclerView: RecyclerView,
//    viewHolder: RecyclerView.ViewHolder
//): Boolean = false
//
//override fun onSwipe(viewHolder: ViewHolder, direction: Int) {
//    val position = viewHolder.adapterPosition
//    val item = adapter.removeItem(positoin) ?: return
//    onItemRemoved?.invoke(item)
//}
//
//val swipeableAdapter = SomeAdapter() // adapter must inherit ListAdapterSwipeable
//val recyclerView = findViewById(R.id.recyclerView).apply {
//    adapter = swipeableAdapter
//}
//ItemTouchHelper(ItemSwipeHandler(someAdapter) { Log.d("test", "removedotem: $it") }).attachToRecyclerView(recyclerView)
