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
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception

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
        val taskListView: RecyclerView = binding.tasksList
        taskListView.layoutManager = LinearLayoutManager(requireContext())

        lateinit var selectedDate: String

        val fileName = "tasks.json"
        copyAssetsToFile(requireContext(), fileName)

        val userCollection: MutableList<Collection> = readFromFile(fileName)

        binding.calendar.setOnDateChangeListener { _, year, month, dayOfMonth ->
            selectedDate = "${month + 1}/${dayOfMonth}/${year}"
            binding.selectedDate.text = selectedDate

            val sortedTasks: MutableList<Task> = getDailyTasksSorted(userCollection, selectedDate)

            taskListView.visibility = if (sortedTasks.isEmpty()) View.GONE else View.VISIBLE
            try {
                val taskAdapter = TaskAdapter(sortedTasks)
                taskAdapter.setOnItemRemovedListener { removedTask ->
                    taskAdapter.removeItem(removedTask)
                    Log.d("Test", "removed item: $removedTask")
                }
                taskListView.adapter = taskAdapter

                val itemTouchHelperCallback = ItemTouchHelperCallback(taskAdapter)
                val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
                itemTouchHelper.attachToRecyclerView(binding.tasksList)
            } catch (e: Exception) {
                Log.e("Test","Error: ${e.message}")
                e.printStackTrace()
            }
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

                    val sortedTasks: MutableList<Task> = getDailyTasksSorted(userCollection, selectedDate)
                    val taskAdapter = TaskAdapter(sortedTasks)
                    taskAdapter.setOnItemRemovedListener { removedTask ->
                        taskAdapter.removeItem(removedTask)
                        Log.d("Test", "removed item: $removedTask")
                    }
                    taskListView.adapter = taskAdapter
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

}

/*
interface ItemTouchHelperAdapter {
fun onItemMove(fromPosition: Int, toPosition: Int): Boolean
fun onItemDismiss(position: Int)
}
class TaskAdapter(private val tasks: MutableList<Task>) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>(), ItemTouchHelperAdapter {

private val removedItems = mutableListOf<Task>()
private var onItemRemoved: ((Task) -> Unit)? = null

fun setOnItemRemovedListener(listener: (Task) -> Unit) {
onItemRemoved = listener
}

override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
val itemView = LayoutInflater.from(parent.context).inflate(R.layout.task_item, parent, false)
return TaskViewHolder(itemView)
}

override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
val task = tasks[position]
holder.bind(task)
}

override fun getItemCount(): Int {
return tasks.size
}

fun removeItem(removedTask: Task) {
val position = tasks.indexOf(removedTask)
if (position >= itemCount) return
val item = tasks[position]
removedItems.add(item)
val actualList = tasks.filterNot { removedItems.contains(it) }
tasks.clear()
tasks.addAll(actualList)
notifyDataSetChanged()
}

override fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
return false
}

override fun onItemDismiss(position: Int) {
tasks.removeAt(position)
notifyItemRemoved(position)
}

inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
private val taskTextView: TextView = itemView.findViewById((R.id.textItem))

init {
val swipeCallback = object :
ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
override fun onMove(
recyclerView: RecyclerView,
viewHolder: ViewHolder,
target: RecyclerView.ViewHolder
): Boolean {
return false
}

override fun onSwiped(viewHolder: ViewHolder, direction: Int) {
val position = viewHolder.adapterPosition
val task = tasks[position]
removeItem(task)
onItemRemoved?.invoke(task)
}
}

val itemTouchHelper = ItemTouchHelper(swipeCallback)
itemTouchHelper.attachToRecyclerView(itemView.findViewById<RecyclerView>(R.id.tasksList))
}

fun bind(task: Task) {
taskTextView.text = task.name
}
}

}
class ItemTouchHelperCallback(private val adapter: ItemTouchHelperAdapter) : ItemTouchHelper.Callback() {

override fun isLongPressDragEnabled(): Boolean {
return false
}

override fun isItemViewSwipeEnabled(): Boolean {
return true
}

override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: ViewHolder): Int {
val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
val swipeFlags = ItemTouchHelper.START or ItemTouchHelper.END
return makeMovementFlags(dragFlags, swipeFlags)
}

override fun onMove(
recyclerView: RecyclerView,
viewHolder: ViewHolder,
target: ViewHolder
): Boolean {
return adapter.onItemMove(viewHolder.adapterPosition, target.adapterPosition)
}

override fun onSwiped(viewHolder: ViewHolder, direction: Int) {
adapter.onItemDismiss(viewHolder.adapterPosition)
}
}
*/