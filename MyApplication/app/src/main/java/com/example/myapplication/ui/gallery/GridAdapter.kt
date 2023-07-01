package com.example.myapplication.ui.gallery

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import com.bumptech.glide.Glide

/*
class GridAdapter(val context: Context, val PictureList: List<Int>) : BaseAdapter() {
    override fun getCount(): Int {
        return PictureList.size
    }

    override fun getItem(position: Int): Any {
        return PictureList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val imageView: ImageView = ImageView(context)
        imageView.setImageResource(PictureList[position])

//        imageView.setOnClickListener {
//
//        }
        return imageView
    }

} */

class GridAdapter(private val context: Context, private val addressList: ArrayList<String>) : BaseAdapter() {
    // in base adapter class we are creating variables
    // for layout inflater, course image view and course text view.
    private var layoutInflater: LayoutInflater? = null
    private lateinit var courseIV: ImageView

    // addressList 길이 반환
    override fun getCount(): Int {
        return addressList.size
    }

    // grid view의 item 반환 (해당 position의 사진 주소)
    override fun getItem(position: Int): Any {
        return addressList[position]
    }

    // grid view의 item의 id 반환 (이 method는 사용 안하는 듯)
    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    // in below function we are getting individual item of grid view.
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val imageView = ImageView(context)
        val display = context.resources.displayMetrics
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
        imageView.layoutParams = LinearLayout.LayoutParams(display.widthPixels/3,display.widthPixels/3)
        Glide.with(context).load(addressList[position]).into(imageView)
        return imageView
    }


}