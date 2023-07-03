package com.example.myapplication.ui.calendar

import android.app.Activity
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ActionMenuView
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.google.gson.Gson
import java.io.File

class TaskAdapter(private var tasks: MutableList<Task>, private val activity: Activity) : BaseAdapter() {
    private var onItemRemoved: ((Task) -> Unit)? = null
    private var onCheckedChangeListener: ((Task, Boolean) -> Unit)? = null

    fun setOnItemRemovedListener(listener: (Task) -> Unit) {
        onItemRemoved = listener
    }

    fun setOnCheckedChangeListener(listener: (Task, Boolean) -> Unit) {
        onCheckedChangeListener = listener
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

    override fun getView(position: Int, itemView: View?, parent: ViewGroup): View {
        val view = itemView ?: LayoutInflater.from(parent.context).inflate(R.layout.task_item, parent, false)

        val textCheckbox: CheckBox = view.findViewById(R.id.checkItem)
        val deleteButton: Button = view.findViewById(R.id.deleteButton)

        val task = tasks[0]

        textCheckbox.text = task.name
        textCheckbox.isChecked = task.completed
        textCheckbox.setOnCheckedChangeListener { _, isChecked ->
            task.completed = isChecked
            onCheckedChangeListener?.invoke(task, isChecked)
        }

        view.setOnClickListener {
            task.completed = !task.completed
            textCheckbox.isChecked = task.completed
            onCheckedChangeListener?.invoke(task, task.completed)
        }

        deleteButton.setOnClickListener {
            removeItem(task)
        }

        return view
    }

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val taskCheckboxView: CheckBox = itemView.findViewById(R.id.checkItem)
        fun bind(task: Task) {
            taskCheckboxView.text = task.name
            taskCheckboxView.isChecked = task.completed
            taskCheckboxView.setOnCheckedChangeListener {_, isChecked ->
                task.completed = isChecked
                onCheckedChangeListener?.invoke(task, isChecked)
            }
        }
    }
}