package com.example.myapplication.ui.calendar

import android.app.Activity
import android.content.ActivityNotFoundException
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.myapplication.R
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileOutputStream
import com.example.myapplication.databinding.FragmentFeedBinding
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar

data class Post(var title: String, var location: String, var date: String,
                var imgList: MutableList<Uri>, var contactList: MutableList<String>, var note: String)

class CalendarFragment : Fragment() {
    private var _binding: FragmentFeedBinding? = null
    private var currentPost: Post? = null
    private val fileName = "posts.json"
    private val binding get() = _binding!!
    private val gson = Gson()
    private val REQUEST_CODE_GALLERY = 1001

    private lateinit var postAdapter: PostAdapter
    private lateinit var postList: MutableList<Post>

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val postListView: ListView = binding.postListView

        val fileName = "posts.json"
        copyAssetsToFile(requireContext(), fileName)

//        var today: String = getToday()
        postList = readFromFile(fileName)
        var sortedPosts: MutableList<Post> = getPostSorted(postList)

        postAdapter = PostAdapter(requireContext(), sortedPosts)
        postListView.adapter = postAdapter
        postListView.visibility = if (sortedPosts.isEmpty()) View.GONE else View.VISIBLE


        /////////////////////add post////////////////////////
        binding.newPost.setOnClickListener {
            showNewPostDialog(fileName, postAdapter)
        }

        /////////////////////delete post////////////////////////
        postAdapter.setOnDeletePostListener { post ->
            postList.remove(post)
            writeToFile(fileName, postList)
            postAdapter.updateData(postList)
        }

        /////////////////////edit////////////////////////
        postAdapter.setOnEditListener { posts ->
            postList.clear()
            postList.addAll(posts)
            writeToFile(fileName, postList)
            postAdapter.updateData(postList)
        }

        // FIXME
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

        return postList.sortedByDescending { post ->
            try {
                sdf.parse(post.date)
            } catch (e: ParseException) {
                Log.d("my_log", "date format error: $e")
                null
            }
        }.toMutableList()
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

//            currentPost?.let { postToUpdate ->
//                postToUpdate.imgList.addAll(selectedUris)
//                postAdapter.notifyDataSetChanged()
//            }
            currentPost?.let { currentPost ->
                currentPost.imgList.addAll(selectedUris)
//                writeToFile(fileName, getPostSorted(postList))
                postAdapter.notifyDataSetChanged()
            }
        }
    }

    // TODO: 홍은빈
    // 사진 넘어갈때 인스타 아래처럼 몇번째 사진인지? 그런거 할 수 잇으면
    // 이미지 추가하고 저장 안됨 (writeToFile 해야함)

    private fun showNewPostDialog(fileName: String, postAdapter: PostAdapter) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("make new post!")

        val view = layoutInflater.inflate(R.layout.dialog_new_post, null)
        builder.setView(view)

        builder.setPositiveButton("add", null)
        builder.setNegativeButton("cancel", null)

        val dialog = builder.create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val title = view.findViewById<EditText>(R.id.new_title).text.toString().let {
                    it.ifBlank { "none" }
                }
                val date = view.findViewById<EditText>(R.id.new_date).text.toString().let {
                    it.ifBlank { "none" }
                }
                val location = view.findViewById<EditText>(R.id.new_title).text.toString().let {
                    it.ifBlank { "none" }
                }
                val note = view.findViewById<EditText>(R.id.new_note).text.toString().let {
                    it.ifBlank { "none" }
                }

                val newPost = Post(title, location, date, mutableListOf(), mutableListOf(), note)
                postList = readFromFile(fileName)
                postList.add(newPost)
                writeToFile(fileName, postList)
                postAdapter.updateData(postList)

                dialog.dismiss()
            }
        }

        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_border)
        dialog.show()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
