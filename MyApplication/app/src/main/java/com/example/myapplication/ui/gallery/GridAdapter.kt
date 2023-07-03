package com.example.myapplication.ui.gallery

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import com.bumptech.glide.Glide

class GridAdapter(private val context: Context, private val addressList: ArrayList<String>) : BaseAdapter() {
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
        imageView.setPadding(3, 3, 3, 3)
        imageView.layoutParams = LinearLayout.LayoutParams(display.widthPixels/3,display.widthPixels/3)
        Glide.with(context).load(addressList[position]).into(imageView)
        return imageView
    }
}