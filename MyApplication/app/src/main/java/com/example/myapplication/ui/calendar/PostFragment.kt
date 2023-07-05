package com.example.myapplication.ui.post

import android.app.Activity
import android.content.ActivityNotFoundException
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileOutputStream
import com.example.myapplication.databinding.FragmentPostBinding
import java.text.ParseException
import java.text.SimpleDateFormat
import com.example.myapplication.ui.home.Person
import com.example.myapplication.ui.home.HomeFragment.PersonAdapter

class Post(
    var title: String,
    var location: String,
    var date: String,
    var imgList: MutableList<String>,
    var note: String,
    var contactList: MutableList<Person>
)
//    var contactList: MutableList<String>,
//    var contactImgList: MutableList<String> = mutableListOf() // 빈 리스트로 초기화


class PostFragment : Fragment() {
    private var _binding: FragmentPostBinding? = null
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
        _binding = FragmentPostBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val postListView: ListView = binding.postListView

        val fileName2 = "numbers.json" // 이전 Fragment에서 사용한 파일 이름을 동일하게 사용해야 합니다.
        val people: MutableList<Person> = readFromFile(fileName2, object : TypeToken<MutableList<Person>>() {})

        copyAssetsToFile(requireContext(), fileName)

        postList = readFromFile(fileName, object : TypeToken<MutableList<Post>>() {})
        val sortedPosts: MutableList<Post> = getPostSorted(postList)

        postAdapter = PostAdapter(requireContext(), sortedPosts)
        postListView.adapter = postAdapter
        postListView.visibility = if (sortedPosts.isEmpty()) View.GONE else View.VISIBLE

        /////////////////////new post////////////////////////
        binding.newPost.setOnClickListener {
            showNewPostDialog(fileName, postAdapter, people)
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

        /////////////////////add contact////////////////////////
        var selectedPeople = mutableListOf<Person>()
        postAdapter.setOnAddContactListener { layout, post ->
            currentPost = post
            showContactsDialog(people, selectedPeople, layout)
        }

        /////////////////////contact details////////////////////////
        postAdapter.setOnContactDetailListener { person ->
            showContactDetailsDialog(person)
        }

        return root
    }

    private fun <T> readFromFile(fileName: String, typeToken: TypeToken<MutableList<T>>): MutableList<T> {
        val file = File(requireContext().filesDir, fileName)
        val json = file.readText()
        return gson.fromJson(json, typeToken.type)
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

    private fun getPostSorted(postList: MutableList<Post>): MutableList<Post> {
        val sdf = SimpleDateFormat("MM/dd/yyyy")

        return postList.sortedByDescending { post ->
            try {
                sdf.parse(post.date)
            } catch (e: ParseException) {
                Log.d("my_log", "date format error: $e")
                null
            }
        }.reversed().toMutableList()
    }

    private fun writeToFile(fileName: String, data: MutableList<Post>) {
        val file = File(requireContext().filesDir, fileName)
        val json = gson.toJson(data)
        file.writeText(json)
        printJsonFileData(fileName)
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
            val selectedUris = mutableListOf<String>()

            if (clipData != null) {
                for (i in 0 until clipData.itemCount) {
                    val item = clipData.getItemAt(i)
                    val itemUri = item.uri
                    selectedUris.add(itemUri.toString())
                }
            } else if (uri != null) {
                selectedUris.add(uri.toString())
            }

            currentPost?.let { currentPost ->
                currentPost.imgList.addAll(selectedUris)
                writeToFile("posts.json", getPostSorted(postList))
                postAdapter.notifyDataSetChanged()
            }
        }
    }

    private val Int.dp: Int
        get() = (this * Resources.getSystem().displayMetrics.density + 0.5f).toInt()

    private fun showContactsDialog(people: MutableList<Person>, selectedPeople: MutableList<Person>, contactListLayout: LinearLayout) {
        val builder = AlertDialog.Builder(contactListLayout.context)
        val view = layoutInflater.inflate(R.layout.dialog_contacts, null)
        val listView = view.findViewById<RecyclerView>(R.id.contacts_list)

        val layoutManager = LinearLayoutManager(contactListLayout.context)
        listView.layoutManager = layoutManager

        val adapter = PersonAdapter(people) { person ->
            selectedPeople.add(person)
            Toast.makeText(contactListLayout.context, "${person.name} selected", Toast.LENGTH_SHORT).show()

            if (!person.imageUri.isNullOrEmpty()) {
                // Create a new CardView
                val cardView = CardView(contactListLayout.context).apply {
                    layoutParams = ViewGroup.LayoutParams(50.dp, 50.dp) // replace 50.dp with your desired dp size
                    radius = 70f // half of the size to make a circle
                    useCompatPadding = true
                    preventCornerOverlap = false
                    cardElevation = 12f
                }

                // Create a new ImageView and add it to CardView
                val imageView = ImageView(contactListLayout.context).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    scaleType = ImageView.ScaleType.CENTER_CROP
                }

                cardView.addView(imageView)

                // Load the image into ImageView using Glide
                Glide.with(this@PostFragment)
                    .load(Uri.parse(person.imageUri))
                    .override(50.dp, 50.dp) // replace 50.dp with your desired dp size
                    .centerCrop() // apply circle crop
                    .into(imageView)

                // Add the CardView to contactListLayout
                contactListLayout.addView(cardView)
            } else {
                // Create a new CardView
                val cardView = CardView(contactListLayout.context).apply {
                    layoutParams = ViewGroup.LayoutParams(50.dp, 50.dp) // replace 50.dp with your desired dp size
                    radius = 70f // half of the size to make a circle
                    useCompatPadding = true
                    preventCornerOverlap = false
                    cardElevation = 12f
                }

                // Create a new ImageView and add it to CardView
                val imageView = ImageView(contactListLayout.context).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    scaleType = ImageView.ScaleType.CENTER_CROP
                }

                cardView.addView(imageView)

                imageView.setImageResource(R.drawable.ic_name)

                // Add the CardView to contactListLayout
                contactListLayout.addView(cardView)
            }

            currentPost?.let { currentPost ->
                currentPost.contactList.addAll(selectedPeople)
                writeToFile("post.json", postList)
                postAdapter.notifyDataSetChanged()
            }

        }

        listView.adapter = adapter

        builder.setView(view)
        builder.setPositiveButton("선택 완료", null) // 추가
        builder.create().show()
    }

    private fun showNewPostDialog(fileName: String, postAdapter: PostAdapter, people: MutableList<Person>) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("새로운 블로그 작성하기")

        val view = layoutInflater.inflate(R.layout.dialog_new_post, null)
        builder.setView(view)

        builder.setPositiveButton("추가", null)
        builder.setNegativeButton("취소", null)

        val dialog = builder.create()

        val selectedPeople = mutableListOf<Person>()
        val contactListLayout = view.findViewById<LinearLayout>(R.id.contactList2)
        val addImageButton: ImageButton = view.findViewById(R.id.photo_addbutton)
        val addContactButton = view.findViewById<ImageButton>(R.id.contact_addbutton)

        var newPost = Post(
            "",
            "",
            "",
            mutableListOf(),
            "",
            mutableListOf()
        )

        currentPost = newPost

        addImageButton.setOnClickListener {
            val galleryIntent = Intent(Intent.ACTION_GET_CONTENT)
            galleryIntent.type = "image/*"
            galleryIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            try {
                startActivityForResult(galleryIntent, REQUEST_CODE_GALLERY)
            } catch (e: ActivityNotFoundException) {
                // Handle the case when no gallery app is available
            }
        }
        addContactButton.setOnClickListener {
            showContactsDialog(people, selectedPeople, contactListLayout)
        }

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                newPost.title = view.findViewById<EditText>(R.id.new_title).text.toString().let {
                    it.ifBlank { "제목을 입력하세요" }
                }
                newPost.date = view.findViewById<EditText>(R.id.new_date).text.toString().let {
                    it.ifBlank { "날짜를 입력하세요" }
                }
                newPost.location = view.findViewById<EditText>(R.id.new_location).text.toString().let {
                    it.ifBlank { "장소를 입력하세요" }
                }
                newPost.note = view.findViewById<EditText>(R.id.new_note).text.toString().let {
                    it.ifBlank { "추억을 기록하세요" }
                }

                newPost.contactList = selectedPeople

                currentPost = newPost
                postList = readFromFile(fileName, object : TypeToken<MutableList<Post>>() {})
                postList.add(newPost)
                writeToFile(fileName, postList)
                postAdapter.updateData(postList)

                dialog.dismiss()
            }
        }

        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_border)
        dialog.show()
    }

    private fun showContactDetailsDialog(person: Person) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("세부 정보")

        val view = LayoutInflater.from(context).inflate(R.layout.readonly_detail_dialog, null)
        val profileImage = view.findViewById<ImageView>(R.id.profileImage)
        val gohomeButtom = view.findViewById<Button>(R.id.gohome)
        builder.setView(view)

        // 기본 이미지는 ic_name으로 설정
        val drawableId = resources.getIdentifier("ic_name", "drawable", requireContext().packageName)
        profileImage.setImageResource(drawableId)

        if (!person.imageUri.isNullOrEmpty()) {
            profileImage.setImageURI(Uri.parse(person.imageUri))
        }

        val nameTextView = view.findViewById<TextView>(R.id.nameTextView)
        val numberTextView = view.findViewById<TextView>(R.id.numberTextView)
        val emailTextView = view.findViewById<TextView>(R.id.emailTextView)
        val instagramTextView = view.findViewById<TextView>(R.id.instagramTextView)
        val githubTextView = view.findViewById<TextView>(R.id.githubTextView)
        val dialog = builder.create()

        nameTextView.text = "이름: ${person.name}"
        numberTextView.text = "전화번호: ${person.number}"
        emailTextView.text = "E-mail: ${person.email}"
        instagramTextView.text = "Instagram ID: ${person.instagram}"
        githubTextView.text = "Github ID: ${person.github}"
        builder.setPositiveButton("닫기", null)

        gohomeButtom.setOnClickListener {
            dialog.dismiss()
        }

        // Instagram 클릭 시 Instagram App으로 이동
        instagramTextView.setOnClickListener {
            val username = person.instagram
            val uri = Uri.parse("https://instagram.com/_u/$username")
            val instagram = Intent(Intent.ACTION_VIEW, uri)

            instagram.setPackage("com.instagram.android")

            val packageManager = requireContext().packageManager
            val activities = packageManager.queryIntentActivities(instagram, 0)

            if (activities.isNotEmpty()) {
                startActivity(instagram)
            } else {
                // Instagram App이 없을 경우 웹 사이트로 이동
                val webIntent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(webIntent)
            }
        }

        // Github 클릭 시 Github App으로 이동
        githubTextView.setOnClickListener {
            val username = person.github
            val uri = Uri.parse("https://github.com/$username")
            val github = Intent(Intent.ACTION_VIEW, uri)

            try {
                startActivity(github)
            } catch (e: Exception) {
                // URL로 연결할 수 없을 경우의 예외 처리
                e.printStackTrace()
            }
        }

        // 전화번호 클릭 시 클릭한 번호가 입력된 키패드로 이동
        numberTextView.setOnClickListener {
            val phoneNumber = numberTextView.text.toString()
            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
            startActivity(dialIntent)
        }

        if (person.imageUri != null) {
            profileImage.setImageURI(Uri.parse(person.imageUri))
        }

        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_border)
        dialog.show()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}