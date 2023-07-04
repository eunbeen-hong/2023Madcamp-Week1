package com.example.myapplication.ui.calendar

import android.content.Context
import android.graphics.Bitmap
import android.app.AlertDialog
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
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.example.myapplication.R
import android.graphics.BitmapFactory
import android.os.AsyncTask
import java.io.InputStream

class LoadImageTask(private val imageView: ImageView) : AsyncTask<Uri, Void, Bitmap?>() {
    override fun doInBackground(vararg uris: Uri): Bitmap? {
        var inputStream: InputStream? = null
        try {
            inputStream = imageView.context.contentResolver.openInputStream(uris[0])
            return BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            inputStream?.close()
        }
        return null
    }

    override fun onPostExecute(bitmap: Bitmap?) {
        imageView.setImageBitmap(bitmap)
    }
}

class PostAdapter(private val context: Context, private var posts: MutableList<Post>) : BaseAdapter() {
    private var onItemRemoved: ((Post) -> Unit)? = null
    private var onTextEditListener : ((Post, TextView) -> Unit)? = null
    private var onEditListener : ((MutableList<Post>) -> Unit)? = null
    private var onAddPhotoListener : ((Post) -> Unit)? = null
    private var onDeletePostListener : ((Post) -> Unit)? = null
    private val REQUEST_CODE_GALLERY = 1001

    fun setOnDeletePostListener(listener: (Post) -> Unit) {
        onDeletePostListener = listener
    }

    fun setOnEditListener (listener: (MutableList<Post>) -> Unit) {
        onEditListener = listener
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
        val deleteButton: ImageButton = view.findViewById(R.id.deleteButton)
        val editButton: ImageButton = view.findViewById(R.id.editButton)

        travelName.text = post.title; travelName.tag = "title"
        location.text = post.location; location.tag = "location"
        date.text = SpannableStringBuilder(post.date); date.tag = "date"
        note.text = post.note; note.tag = "note"

        val imageCount = post.imgList.size
        Toast.makeText(context, "Image Count: $imageCount", Toast.LENGTH_SHORT).show()
        imageList.removeAllViews()

        /////////////////////delete post////////////////////////
        deleteButton.setOnClickListener {
            confirmDelete(post)
        }

        /////////////////////edit post////////////////////////
        editButton.setOnClickListener {
            showEditDialog(posts, position)
        }

        // FIXME
        /////////////////////images////////////////////////

        for (uri in post.imgList) {
            val imageView = ImageView(context)
            imageView.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 300
            )
            imageView.adjustViewBounds = true
            imageView.scaleType = ImageView.ScaleType.FIT_XY // or ImageView.ScaleType.CENTER_CROP
            imageList.addView(imageView)

            LoadImageTask(imageView).execute(uri)
        }

        /////////////////////contacts////////////////////////
        for (contact in post.contactList) {
            val contactView = LayoutInflater.from(context).inflate(R.layout.contact_item, contactList, false) as LinearLayout
            val contactImageButton = contactView.findViewById<ImageButton>(R.id.contactImage)

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

        // FIXME
        /////////////////////add photo////////////////////////
        addPhotoButton.setOnClickListener {
            addPhoto(post)
        }

        return view
    }

    private fun showEditDialog(posts: MutableList<Post>, position: Int) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Edit Post")

        val view = LayoutInflater.from(context).inflate(R.layout.dialog_edit_post, null)
        builder.setView(view)

        builder.setPositiveButton("confirm", null)
        builder.setNegativeButton("cancel", null)

        view.findViewById<EditText>(R.id.edit_title).setText(posts[position].title)
        view.findViewById<EditText>(R.id.edit_date).setText(posts[position].date)
        view.findViewById<EditText>(R.id.edit_location).setText(posts[position].location)
        view.findViewById<EditText>(R.id.edit_note).setText(posts[position].note)

        val dialog = builder.create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val title = view.findViewById<EditText>(R.id.edit_title).text.toString()
                val date = view.findViewById<EditText>(R.id.edit_date).text.toString()
                val location = view.findViewById<EditText>(R.id.edit_location).text.toString()
                val note = view.findViewById<EditText>(R.id.edit_note).text.toString()

                val editedPost = Post(title, location, date, mutableListOf(), mutableListOf(), note)

                posts[position] = editedPost

                onEditListener ?.invoke(posts)

                dialog.dismiss()
            }
        }

        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_border)
        dialog.show()
    }

    private fun confirmDelete(post: Post) {
        val builderShow = AlertDialog.Builder(context)
        builderShow.setTitle("삭제 확인")
        builderShow.setMessage("정말로 해당 포스트를 삭제하시겠습니까?")
        builderShow.setPositiveButton("삭제") {_, _ ->
            onDeletePostListener ?.invoke(post)
        }
        builderShow.setNegativeButton("취소") {dialog, _ ->
            dialog.dismiss()
        }
        builderShow.create().show()
    }

    // FIXME
    private fun addPhoto(post: Post) {
        onAddPhotoListener ?.invoke(post)
    }
}