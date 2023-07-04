package com.example.myapplication.ui.calendar

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileOutputStream
import com.example.myapplication.databinding.FragmentFeedBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar

data class Post(
    var tripName: String,
    var location: String,
    var date: String,
    var imgList: MutableList<Uri>, // Nullable 제거
    var contactList: MutableList<String>,
    var note: String
)


class CalendarFragment : Fragment() {
    private var _binding: FragmentFeedBinding? = null
    private var currentPost: Post? = null
    private val fileName = "posts.json"
    private val binding get() = _binding!!
    private val gson = Gson()
    private val REQUEST_CODE_GALLERY = 1001

    private lateinit var postAdapter: PostAdapter

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val postListView: ListView = binding.postListView
        val fab_newPost: FloatingActionButton = binding.newPost

        val fileName = "posts.json"
        copyAssetsToFile(requireContext(), fileName)

        // TODO: addition (in fab)
        // TODO: deletion (add delete button)

//        var today: String = getToday()
        var postList: MutableList<Post> = readFromFile(fileName)
        var sortedPosts: MutableList<Post> = getPostSorted(postList)

        postAdapter = PostAdapter(requireContext(), sortedPosts)
        postListView.adapter = postAdapter
        postListView.visibility = if (sortedPosts.isEmpty()) View.GONE else View.VISIBLE


        /////////////////////edit////////////////////////
        postAdapter.setOnTextEditListener { post, textView ->
            val newText = textView.text.toString()
            val position = postList.indexOf(post)
            if (position != -1) {
                when (textView.tag) {
                    "tripName" -> postList[position].tripName = newText
                    "location" -> postList[position].location = newText
                    "date" -> {
                        postList[position].date = newText
                        sortedPosts = getPostSorted(postList)
                    }
                    "note" -> postList[position].note = newText
                }
                writeToFile(fileName, postList)
                postAdapter.updateData(sortedPosts)
            }
        }

        var currentScrollPosition = 0

        postListView.setOnScrollListener(object : AbsListView.OnScrollListener {
            override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {
                // Handle scroll state changes if needed
            }

            override fun onScroll(view: AbsListView, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int) {
                // Update the current scroll position
                currentScrollPosition = postListView.firstVisiblePosition
            }
        })

        postListView.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                postListView.post {
                    postListView.setSelection(currentScrollPosition)  // Scroll to the current scroll position
                }
            }
        }


        /////////////////////add photo////////////////////////
        postAdapter.setOnAddPhotoListener { post ->
            currentPost = post
            val galleryIntent = Intent(Intent.ACTION_GET_CONTENT)
            galleryIntent.type = "image/*"
            galleryIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            try {
                startActivityForResult(galleryIntent, REQUEST_CODE_GALLERY)
            } catch (e: ActivityNotFoundException) {
                // Handle the case when no gallery app is available
            }
        }


        return root
    }

    private fun readFromFile(fileName: String): MutableList<Post> {
        val file = File(requireContext().filesDir, fileName)
        val json = file.readText()
        return gson.fromJson(json, object : TypeToken<MutableList<Post>>() {}.type)
    }

    private fun writeToFile(fileName: String, data: MutableList<Post>) {
        val file = File(requireContext().filesDir, fileName)
        val json = gson.toJson(data)
        file.writeText(json)
        printJsonFileData(fileName)
    }

    private fun getPostSorted(postList: MutableList<Post>): MutableList<Post> {
        val sdf = SimpleDateFormat("MM/dd/yyyy")

        postList.sortBy { post ->
            try {
                sdf.parse(post.date)
            } catch (e: ParseException) {
                Log.d("my_log", "date format error: $e")
                null
            }
        }
        return postList
    }

    private fun copyAssetsToFile(context: Context, fileName: String) {
        val file = File(context.filesDir, fileName)
        if (!file.exists()) {
            val assetManager = context.assets
            val inputStream = assetManager.open(fileName)
            val outputStream = FileOutputStream(file)

            inputStream.copyTo(outputStream)

            inputStream.close()
            outputStream.close()
        }
    }

    private fun getToday(): String {
        val calendar = Calendar.getInstance()
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        val month = calendar.get(Calendar.MONTH)
        val year = calendar.get(Calendar.YEAR)
        return "${month + 1}/${dayOfMonth}/${year}"
    }

    private fun printJsonFileData(fileName: String) {
        val file = File(requireContext().filesDir, fileName)
        val json = file.readText()
        Log.d("JSON File", json)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_GALLERY && resultCode == Activity.RESULT_OK) {
            val clipData = data?.clipData
            val uri = data?.data
            val selectedUris = mutableListOf<Uri>()

            if (clipData != null) {
                for (i in 0 until clipData.itemCount) {
                    val item = clipData.getItemAt(i)
                    val itemUri = item.uri
                    selectedUris.add(itemUri)
                }
            } else if (uri != null) {
                selectedUris.add(uri)
            }

            currentPost?.let { postToUpdate ->
                postToUpdate.imgList.addAll(selectedUris)
                postAdapter.notifyDataSetChanged()
            }
        }
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
