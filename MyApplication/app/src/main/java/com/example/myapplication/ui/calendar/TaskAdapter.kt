package com.example.myapplication.ui.calendar

import android.app.Activity
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ActionMenuView
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.google.gson.Gson
import org.w3c.dom.Text
import java.io.File

class TaskAdapter(private var tasks: MutableList<Task>, private val activity: Activity) : BaseAdapter() {
    private var onItemRemoved: ((Task) -> Unit)? = null
    private var onCheckedChangeListener : ((Task, Boolean) -> Unit)? = null

    fun setOnItemRemovedListener(listener: (Task) -> Unit) {
        onItemRemoved = listener
    }

    fun setOnCheckedChangeListener (listener: (Task, Boolean) -> Unit) {
        onCheckedChangeListener  = listener
    }

    fun updateData(newTasks: MutableList<Task>) {
        tasks.clear()
        tasks.addAll(newTasks)
        notifyDataSetChanged()
    }


    override fun getCount(): Int {
        return tasks.size
    }

    override fun getItem(position: Int): Any {
        return tasks[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    private fun removeItem(removedTask: Task) {
        val position = tasks.indexOf(removedTask)
        if (position != -1) {
            tasks.removeAt(position)
            onItemRemoved?.invoke(removedTask)
            notifyDataSetChanged()
        }
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view = convertView ?: LayoutInflater.from(parent.context).inflate(R.layout.task_item, parent, false)
        val task = tasks[position]

        val checkButton: ImageButton = view.findViewById(R.id.checkButton)
        val taskText: TextView = view.findViewById(R.id.taskText)
        val deleteButton: ImageButton = view.findViewById(R.id.deleteButton)


        taskText.text = task.name
        taskText.tag = position

        if (task.completed) {
            checkButton.setImageResource(R.drawable.check_true)
        } else {
            checkButton.setImageResource(R.drawable.check_false)
        }


//        textCheckbox.setOnCheckedChangeListener { _, isChecked ->
//            if (textCheckbox.tag == position) {
//                Log.d("JSON File", "setOnClickListener (ta) $task")
//                task.completed = isChecked
//                onCheckedChangeListener?.invoke(task, isChecked)
//            }
//        }


        checkButton.setOnClickListener{
            Log.d("JSON File", "setOnClickListener (ta) $task")
            task.completed = !task.completed
            if (task.completed) {
                checkButton.setImageResource(R.drawable.check_true)
            } else {
                checkButton.setImageResource(R.drawable.check_false)
            }
            onCheckedChangeListener ?.invoke(task, task.completed)
        }

        deleteButton.setOnClickListener {
            Log.d("JSON File", "setOnClickListener (ta)")
            removeItem(task)
        }

        return view
    }
}