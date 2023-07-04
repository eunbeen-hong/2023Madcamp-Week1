package com.example.myapplication.ui.calendar

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.SpannableStringBuilder
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R

class PostAdapter(private val context: Context, private var posts: MutableList<Post>) : BaseAdapter() {
    private var onItemRemoved: ((Post) -> Unit)? = null
    private var onTextEditListener : ((Post, TextView) -> Unit)? = null
    private val REQUEST_CODE_GALLERY = 1001

    fun setOnItemRemovedListener(listener: (Post) -> Unit) {
        onItemRemoved = listener
    }

    fun setOnTextEditListener (listener: (Post, TextView) -> Unit) {
        onTextEditListener = listener
    }

    fun updateData(newPosts: MutableList<Post>) {
        posts.clear()
        posts.addAll(newPosts)
        notifyDataSetChanged()
    }

    fun updateImageList(position: Int, imageList: MutableList<Uri>) {
        if (position >= 0 && position < posts.size) {
            val post = posts[position]
            post.imgList.clear()
            post.imgList.addAll(imageList)
            notifyDataSetChanged()
        }
    }

    override fun getCount(): Int {
        return posts.size
    }

    override fun getItem(position: Int): Any {
        return posts[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    private fun removeItem(removedPost: Post) {
        val position = posts.indexOf(removedPost)
        if (position != -1) {
            posts.removeAt(position)
            onItemRemoved?.invoke(removedPost)
            notifyDataSetChanged()
        }
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view = convertView ?: LayoutInflater.from(parent.context).inflate(R.layout.post, parent, false)

        val post = posts[position]

        val travelName: TextView = view.findViewById(R.id.travelName)
        val location: TextView = view.findViewById(R.id.location)
        val date: EditText = view.findViewById(R.id.date)
        val imageList: RecyclerView = view.findViewById(R.id.imageList)
        val contactList: RecyclerView = view.findViewById(R.id.contactList)
        val note: TextView = view.findViewById(R.id.note)
        val addPhotoButton: Button = view.findViewById(R.id.add_photo)

        travelName.text = post.tripName; travelName.tag = "tripName"
        location.text = post.location; location.tag = "location"
        date.text = SpannableStringBuilder(post.date); date.tag = "date"
        note.text = post.note; note.tag = "note"

        val imageAdapter = ImageAdapter(post.imgList)
        val imageLayoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        imageList.layoutManager = imageLayoutManager
        imageList.adapter = imageAdapter

        val contactAdapter = ContactAdapter(post.contactList)
        val contactLayoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        contactList.layoutManager = contactLayoutManager
        contactList.adapter = contactAdapter

        travelName.setOnClickListener {
            enterEditMode(post, travelName)
        }
        location.setOnClickListener {
            enterEditMode(post, location)
        }
        date.setOnClickListener {
            enterEditMode(post, date)
        }
        note.setOnClickListener {
            enterEditMode(post, note)
        }

        addPhotoButton.setOnClickListener {
            val galleryIntent = Intent(Intent.ACTION_GET_CONTENT)
            galleryIntent.type = "image/*"
            galleryIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)

            try {
                startActivityForResult(context as Activity, galleryIntent, REQUEST_CODE_GALLERY, null)
            } catch (e: ActivityNotFoundException) {
                // Handle the case when no gallery app is available
            }
        }

        return view
    }

    private fun enterEditMode(post: Post, textView: TextView) {
        textView.setOnClickListener(null)
        textView.isClickable = false
        textView.isFocusable = true
        textView.isFocusableInTouchMode = true
        textView.requestFocus()
        textView.setOnEditorActionListener { textView, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                exitEditMode(post, textView)
                true
            } else {
                false
            }
        }
    }

    private fun exitEditMode(post: Post, textView: TextView) {
        textView.setOnClickListener {
            enterEditMode(post, textView)
        }
        textView.isClickable = true
        textView.isFocusable = false
        textView.isFocusableInTouchMode = false

        val newText = textView.text.toString()
        val position = posts.indexOf(post)
        if (position != -1) {
            when (textView.tag) {
                "tripName" -> posts[position].tripName = newText
                "location" -> posts[position].location = newText
                "date" -> posts[position].date = newText
                "note" -> posts[position].note = newText
            }
            onTextEditListener ?.invoke(post, textView)
        }
    }
}