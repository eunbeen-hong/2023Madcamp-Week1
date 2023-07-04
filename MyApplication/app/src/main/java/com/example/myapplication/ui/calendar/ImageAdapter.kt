package com.example.myapplication.ui.calendar

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R

class ImageAdapter(private val context: Context, private val imageList: MutableList<Uri>) : RecyclerView.Adapter<ImageAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val imageView = ImageView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        return ViewHolder(imageView)

    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Glide.with(context).load(imageList[position]).into(holder.imageView)
    }

    override fun getItemCount() = imageList.size

    inner class ViewHolder(itemView: ImageView) : RecyclerView.ViewHolder(itemView) {
        val imageView = itemView
    }
}
