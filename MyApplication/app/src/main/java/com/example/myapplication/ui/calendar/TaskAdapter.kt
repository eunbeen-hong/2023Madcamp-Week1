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
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.google.gson.Gson
import java.io.File

class TaskAdapter(private var tasks: MutableList<Task>, private val activity: Activity) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>(), ItemTouchHelperAdapter {
    private val removedItems = mutableListOf<Task>()
    private var onItemRemoved: ((Task) -> Unit)? = null
    private var onCheckedChangeListener: ((Task, Boolean) -> Unit)? = null

    fun setOnItemRemovedListener(listener: (Task) -> Unit) {
        onItemRemoved = listener
    }

    fun setOnCheckedChangeListener(listener: (Task, Boolean) -> Unit) {
        onCheckedChangeListener = listener
    }

    fun updateData(newTasks: MutableList<Task>) {
        tasks = newTasks
        notifyDataSetChanged()
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
        // TODO
//        if (position >= itemCount) return tasks
//        val item = tasks[position]
//        removedItems.add(item)
//        val actualList = tasks.filterNot { removedItems.contains(it) }
//        tasks.clear()
//        tasks.addAll(actualList)
//        notifyDataSetChanged()
//        return actualList.toMutableList()
        if (position != -1) {
            tasks.removeAt(position)
            onItemRemoved?.invoke(removedTask)
            notifyItemRemoved(position)
        }
    }

    override fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
        return false
    }

    override fun onItemDismiss(position: Int) {
        val removedTask = tasks[position]
        tasks.removeAt(position)
        onItemRemoved?.invoke(removedTask)
        notifyItemRemoved(position)
    }

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val taskCheckboxView: CheckBox = itemView.findViewById((R.id.checkItem))

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
                    Toast.makeText(itemView.context, "Removed!", Toast.LENGTH_SHORT).show()
                    val position = viewHolder.adapterPosition
                    val task = tasks[position]
                    removeItem(task)
                }

                override fun onChildDraw(
                    c: Canvas,
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    dX: Float,
                    dY: Float,
                    actionState: Int,
                    isCurrentlyActive: Boolean
                ) {
                    val itemView = viewHolder.itemView
                    val itemHeight = itemView.bottom - itemView.top
                    val background: Drawable = ColorDrawable(Color.RED)
                    val inHeight = 24 // Replace 24 with the desired height of the icon
                    val inWidth = 24 // Replace 24 with the desired width of the icon

                    background.setBounds(
                        itemView.right + dX.toInt(),
                        itemView.top,
                        itemView.right,
                        itemView.bottom
                    )
                    background.draw(c)

                    val iconTop = itemView.top + (itemHeight - inHeight) / 2
                    val iconMargin = (itemHeight - inHeight) / 2
                    val iconLeft = itemView.right - iconMargin - inWidth
                    val iconRight = itemView.right - iconMargin
                    val iconBottom = iconTop + inHeight

                    val icon =
                        itemView.context.getDrawable(R.drawable.ic_dashboard_black_24dp) // Replace `your_icon_resource` with the actual resource ID of your icon
                    icon?.let {
                        it.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                        it.draw(c)
                    }

                    super.onChildDraw(
                        c,
                        recyclerView,
                        viewHolder,
                        dX,
                        dY,
                        actionState,
                        isCurrentlyActive
                    )
                }
            }

            val itemTouchHelper = ItemTouchHelper(swipeCallback)
            itemTouchHelper.attachToRecyclerView(itemView.findViewById(R.id.tasksList))
        }

        fun bind(task: Task) {
            taskCheckboxView.text = task.name
            taskCheckboxView.isChecked = task.completed

            taskCheckboxView.setOnCheckedChangeListener { _, isChecked ->
                task.completed = isChecked
                activity.runOnUiThread {
                    notifyItemChanged(adapterPosition)
                }
                onCheckedChangeListener?.invoke(task, isChecked)
            }
        }
    }
}