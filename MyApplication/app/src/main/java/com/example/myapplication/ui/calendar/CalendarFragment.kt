package com.example.myapplication.ui.calendar

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ListView
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

data class Post(var tripName: String, var location: String, var date: String,
                var imgList: MutableList<Uri>, var contactList: MutableList<String>, var note: String)

class CalendarFragment : Fragment() {
    private var _binding: FragmentFeedBinding? = null
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
        val fab: FloatingActionButton = binding.fabPost

        val fileName = "posts.json"
        copyAssetsToFile(requireContext(), fileName)

        // TODO: edit, add, delete

        var today: String = getToday()
        var postList: MutableList<Post> = readFromFile(fileName)
        var sortedPosts: MutableList<Post> = getPostSorted(postList)

        postAdapter = PostAdapter(requireContext(), sortedPosts)
        postListView.adapter = postAdapter
        postListView.visibility = if (sortedPosts.isEmpty()) View.GONE else View.VISIBLE

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

//        /////////////////////////change complete status/////////////////////////
//        taskAdapter.setOnCheckedChangeListener { task, isChecked ->
//            task.completed = isChecked
//            writeToFile(fileName, userCollection)
//            sortedTasks = getDailyTasksSorted(userCollection, selectedDate)
//            taskAdapter.updateData(sortedTasks)
//        }


//        /////////////////////////remove task/////////////////////////
//        taskAdapter.setOnItemRemovedListener { removedTask ->
//            val selectedCollection = userCollection.find {it.date == selectedDate}
//            selectedCollection?.let {collection ->
//                val index = collection.tasks.indexOf(removedTask)
//                if (index != -1)
//                {
//                    collection.tasks.removeAt(index)
//                    if(collection.tasks.isEmpty()) {
//                        userCollection.remove(collection)
//                    }
//                    writeToFile(fileName, userCollection)
//                    sortedTasks = getDailyTasksSorted(userCollection, selectedDate)
//                    taskAdapter.updateData(sortedTasks)
//                }
//            }
//        }


//        /////////////////////////select date/////////////////////////
//        binding.calendar.setOnDateChangeListener { _, year, month, dayOfMonth ->
//            selectedDate = "${month + 1}/${dayOfMonth}/${year}"
//            binding.selectedDate.text = selectedDate
//
//            sortedTasks = getDailyTasksSorted(userCollection, selectedDate)
//            taskAdapter.updateData(sortedTasks)
//            taskAdapter.notifyDataSetChanged()
//            taskListView.visibility =
//                if (sortedTasks.isEmpty()) View.GONE else View.VISIBLE
//        }

//        /////////////////////////add new task/////////////////////////
//        newTextTask.setOnEditorActionListener { _, actionId, event ->
//
//            if (actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
//                val userInputName: String = newTextTask.text.toString()
//
//                if (userInputName.isNotEmpty()) {
//                    val userInputTask = Task(userInputName, false)
//                    userCollection = readFromFile(fileName)
//
//                    val existingDate: Collection? =
//                        userCollection.find { it.date == selectedDate }
//
//                    if (existingDate != null) {
//                        existingDate.tasks.add(userInputTask)
//                    } else {
//                        val newCollection =
//                            Collection(selectedDate, mutableListOf(userInputTask))
//                        userCollection.add(newCollection)
//                    }
//
//                    writeToFile(fileName, userCollection)
//
//                    sortedTasks = getDailyTasksSorted(userCollection, selectedDate)
//                    taskAdapter.updateData(sortedTasks)
////                    taskAdapter.notifyDataSetChanged()
//
//                    taskListView.visibility =
//                        if (sortedTasks.isEmpty()) View.GONE else View.VISIBLE // TODO: sortedTasks 말고 내부 확인?
//
//                }
//
//                newTextTask.text.clear()
//                true
//            } else {
////                false // false: 재입력시 입력칸 다시 클릭
//                true
//            }
//        }

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
            val imageList = mutableListOf<Uri>()

            val clipData = data?.clipData
            if (clipData != null) {
                // Multiple images were selected
                for (i in 0 until clipData.itemCount) {
                    val imageUri = clipData.getItemAt(i).uri
                    imageList.add(imageUri)
                }
            } else {
                // Single image was selected
                val imageUri = data?.data
                if (imageUri != null) {
                    imageList.add(imageUri)
                }
            }

            postAdapter.updateImageList(currentPosition, imageList)
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
