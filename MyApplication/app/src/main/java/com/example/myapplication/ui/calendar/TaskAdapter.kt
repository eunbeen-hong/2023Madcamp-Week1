package com.example.myapplication.ui.calendar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R

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
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    return false
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
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