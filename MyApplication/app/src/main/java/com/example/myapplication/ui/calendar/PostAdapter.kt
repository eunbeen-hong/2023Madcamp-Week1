package com.example.myapplication.ui.calendar

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.Image
import android.net.Uri
import android.text.SpannableStringBuilder
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.ScrollCaptureCallback
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import org.w3c.dom.Text

class PostAdapter(private val context: Context, private var posts: MutableList<Post>) : BaseAdapter() {
    private var onItemRemoved: ((Post) -> Unit)? = null
    private var onTextEditListener : ((Post, TextView) -> Unit)? = null
    private var onAddPhotoListener : ((Post) -> Unit)? = null
    private val REQUEST_CODE_GALLERY = 1001

    fun setOnItemRemovedListener(listener: (Post) -> Unit) {
        onItemRemoved = listener
    }

    fun setOnTextEditListener (listener: (Post, TextView) -> Unit) {
        onTextEditListener = listener
    }

    fun setOnAddPhotoListener (listener: (Post) -> Unit) {
        onAddPhotoListener = listener
    }

    fun updateData(newPosts: MutableList<Post>) {
        posts.clear()
        posts.addAll(newPosts)
        notifyDataSetChanged()
    }

    fun updateImageList(currentPost: Post?, imageList: MutableList<Uri>) {
        if (currentPost != null) {
            currentPost.imgList.clear()
            currentPost.imgList.addAll(imageList)
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
        val imageList: LinearLayout = view.findViewById(R.id.imageList)
        val contactList: LinearLayout = view.findViewById(R.id.contactList)
        val note: TextView = view.findViewById(R.id.note)
        val addPhotoButton: Button = view.findViewById(R.id.add_photo)

        travelName.text = post.tripName; travelName.tag = "tripName"
        location.text = post.location; location.tag = "location"
        date.text = SpannableStringBuilder(post.date); date.tag = "date"
        note.text = post.note; note.tag = "note"

        /////////////////////images////////////////////////
        for (uri in post.imgList) {
            val imageView = ImageView(context)
            imageView.setImageURI(uri)

            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            imageView.layoutParams = layoutParams

            imageList.addView(imageView)
        }

        /////////////////////contacts////////////////////////
        for (contact in post.contactList) {
            val contactView = LayoutInflater.from(context).inflate(R.layout.contact_item, contactList, false) as LinearLayout
//            val contactTextView = contactView.findViewById<TextView>(R.id.contactName) // 이름 안뜨게 변경
            val contactImageButton = contactView.findViewById<ImageButton>(R.id.contactImage)

            // TODO: show contact info
////             contactTextView.text = contact.name // 이름 안뜨게 변경
//            contactImageButton = contact.image

            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            contactView.layoutParams = layoutParams

            contactList.addView(contactView)

            // TODO: when clicked, go to contact detail
            contactImageButton.setOnClickListener {
                // ?
            }
        }

        /////////////////////edit////////////////////////
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

        /////////////////////add photo////////////////////////
        addPhotoButton.setOnClickListener {
            addPhoto(post)
        }

        return view
    }

    private fun addPhoto(post: Post) {
        val galleryIntent = Intent(Intent.ACTION_GET_CONTENT)
        galleryIntent.type = "image/*"
        galleryIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)

        try {
//            startActivityForResult(context as Activity, galleryIntent, REQUEST_CODE_GALLERY, null)
            val activity = context as Activity
            activity.startActivityForResult(galleryIntent, REQUEST_CODE_GALLERY)
            onAddPhotoListener ?.invoke(post)
        } catch (e: ActivityNotFoundException) {
            // Handle the case when no gallery app is available
        }
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