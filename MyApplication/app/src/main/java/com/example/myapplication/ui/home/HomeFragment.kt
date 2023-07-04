package com.example.myapplication.ui.home

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentHomeBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.*


// 개인 정보 data class
data class Person(
    val name: String,
    val number: String,
    val email: String,
    val instagram: String,
    val github: String,
    val imageUri: String?
)


// 검색 결과에 사용하는 data class
data class PersonItem(
    val imageUri: String?,
    val name: String,
    val number: String
)


// 검색했을 때 나오는 Dialog 관리
class PersonItemAdapter(context: Context, private val items: List<PersonItem>) :
    ArrayAdapter<PersonItem>(context, 0, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.dialog_list_item, parent, false)

        val item = items[position]

        val imageView = view.findViewById<ImageView>(R.id.imageView22)
        val textView = view.findViewById<TextView>(R.id.textView22)
        textView.text = item.name
        val textView3 = view.findViewById<TextView>(R.id.textView33)
        textView3.text = item.number

        if (!item.imageUri.isNullOrEmpty()) {
            try {
                val imageUri = Uri.parse(item.imageUri)
                imageView.setImageURI(imageUri)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            // 기본 이미지는 ic_name으로 설정
            val drawableId = context.resources.getIdentifier("ic_name", "drawable", context.packageName)
            imageView.setImageResource(drawableId)
        }
        return view
    }
}

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private var currentPerson: Person? = null
    private var currentDialog: AlertDialog? = null
    private var currentImageView: ImageView? = null
    private var selectedImageUri: Uri? = null

    private val binding get() = _binding!!
    private val gson = Gson()

    private lateinit var people: MutableList<Person>
    private lateinit var adapter: PersonAdapter

    private val IMAGE_PICK_CODE = 1000

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val fileName = "numbers.json"
        copyAssetToFile(requireContext(), fileName)

        people = readFromFile(fileName)
        val recyclerView: RecyclerView = binding.recyclerView
        adapter = PersonAdapter(people) { person ->
            showDetailsDialog(person)
        }
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        binding.addContact.setOnClickListener {
            showAddContactDialog(fileName, adapter)
        }
        binding.searchContact.setOnClickListener {
            showSearchDialog()
        }

        // FloatingButton 관리
        binding.fab.setOnClickListener {
            if (binding.addContact.visibility == View.VISIBLE) {
                binding.addContact.visibility = View.GONE
                binding.searchContact.visibility = View.GONE
            } else {
                binding.addContact.visibility = View.VISIBLE
                binding.searchContact.visibility = View.VISIBLE
            }
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == IMAGE_PICK_CODE) {
            currentPerson?.let { person ->
                val updatedPerson = person.copy(imageUri = data?.data?.toString()) // toString 메소드 사용
                updateContact(person, updatedPerson, adapter)
            }

            currentImageView?.setImageURI(data?.data)
        }
    }

    private fun copyAssetToFile(context: Context, fileName: String) {
        val file = File(context.filesDir, fileName)
        if (file.exists()) { // 만약 file이 존재하면 Json을 복사하지 않음
            return
        }

        val assetManager = context.assets
        val inputStream = assetManager.open(fileName)
        val outputStream = FileOutputStream(file)

        inputStream.copyTo(outputStream)

        inputStream.close()
        outputStream.close()
    }

    private fun writeToFile(fileName: String, people: List<Person>) {
        val file = File(requireContext().filesDir, fileName)
        val json = gson.toJson(people)
        file.writeText(json)
    }

    private fun readFromFile(fileName: String): MutableList<Person> {
        val file = File(requireContext().filesDir, fileName)
        val json = file.readText()
        return gson.fromJson(json, object : TypeToken<MutableList<Person>>() {}.type)
    }


    // 메인 Fragment에 떠 있는 연락처 리스트 관리 (RecyclerView)
    class PersonAdapter(private var people: List<Person>, private val onClick: (Person) -> Unit) :
        RecyclerView.Adapter<PersonAdapter.PersonViewHolder>() {

        class PersonViewHolder(private val view: View, private val onClick: (Person) -> Unit) :
            RecyclerView.ViewHolder(view) {
            private val textView1: TextView = view.findViewById(R.id.text)
            private val textView2: TextView = view.findViewById(R.id.text2)
            private val imageView: ImageView = view.findViewById(R.id.profileImage)

            fun bind(person: Person) {
                textView1.text = "${person.name}"
                textView2.text = "${person.number}"
                if (!person.imageUri.isNullOrEmpty()) {
                    try {
                        val imageUri = Uri.parse(person.imageUri)
                        imageView.setImageURI(imageUri)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    // 기본 이미지는 ic_name으로 설정
                    val drawableId = view.resources.getIdentifier("ic_name", "drawable", view.context.packageName)
                    imageView.setImageResource(drawableId)
                }
                view.setOnClickListener { onClick(person) }
            }
        }

        // 업데이트하면 화면에 바로 반영되도록 하는 함수
        fun updateData(newPeople: List<Person>) {
            this.people = newPeople
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PersonViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_person, parent, false)
            return PersonViewHolder(view, onClick)
        }

        override fun onBindViewHolder(holder: PersonViewHolder, position: Int) {
            holder.bind(people[position])
        }

        override fun getItemCount() = people.size
    }




    // 연락처 추가 함수
    private fun showAddContactDialog(fileName: String, adapter: PersonAdapter) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("새로운 연락처를 추가하세요!")

        val view = layoutInflater.inflate(R.layout.dialog_add_contact, null)
        builder.setView(view)

        builder.setPositiveButton("확인", null)
        builder.setNegativeButton("취소", null)

        val dialog = builder.create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val name = if (view.findViewById<EditText>(R.id.name).text.toString().isBlank()) "none" else view.findViewById<EditText>(R.id.name).text.toString()
                val numberEditText = view.findViewById<EditText>(R.id.number)
                val number = if (numberEditText.text.toString().isBlank()) "none" else formatPhoneNumber(numberEditText.text.toString())
                val email = if (view.findViewById<EditText>(R.id.email).text.toString().isBlank()) "none" else view.findViewById<EditText>(R.id.email).text.toString()
                val instagram = if (view.findViewById<EditText>(R.id.instagram).text.toString().isBlank()) "none" else view.findViewById<EditText>(R.id.instagram).text.toString()
                val github = if (view.findViewById<EditText>(R.id.github).text.toString().isBlank()) "none" else view.findViewById<EditText>(R.id.github).text.toString()


                // 내부 저장소 업데이트
                val newPerson = Person(name, number, email, instagram, github, getSelectedImageUriAsString())
                people = readFromFile(fileName)
                people.add(newPerson)
                writeToFile(fileName, people)
                adapter.updateData(people)

                dialog.dismiss()
            }
        }

        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_border)
        dialog.show()
    }


    // 휴대폰 번호에 '-' 넣기 위한 함수
    private fun formatPhoneNumber(number: String): String {
        val digits = number.replace("-", "")
        val formattedNumber = StringBuilder()
        var segmentStart = 0
        val segmentLengths = intArrayOf(3, 4, 4)

        for ((index, length) in segmentLengths.withIndex()) {
            val segmentEnd = segmentStart + length
            if (digits.length >= segmentEnd) {
                val segment = digits.substring(segmentStart, segmentEnd)
                formattedNumber.append(segment)
                if (index < segmentLengths.lastIndex) {
                    formattedNumber.append("-")
                }
                segmentStart = segmentEnd
            } else {
                formattedNumber.append(digits.substring(segmentStart))
                break
            }
        }
        return formattedNumber.toString()
    }

    // 세부 개인 정보
    private fun showDetailsDialog(person: Person) {
        currentPerson = person
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("세부 정보")

        val dialogView = LayoutInflater.from(context).inflate(R.layout.person_details_dialog, null)
        val setToBasicButton = dialogView.findViewById<Button>(R.id.setToBasic)
        val profileImage = dialogView.findViewById<ImageView>(R.id.profileImage)
        builder.setView(dialogView)
        currentImageView = profileImage

        // 기본 이미지는 ic_name으로 설정
        val drawableId = resources.getIdentifier("ic_name", "drawable", requireContext().packageName)
        profileImage.setImageResource(drawableId)

        if (selectedImageUri != null) {
            profileImage.setImageURI(selectedImageUri)
        }
        else if (!person.imageUri.isNullOrEmpty()) {
            try {
                val imageUri = Uri.parse(person.imageUri)
                profileImage.setImageURI(imageUri)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val nameTextView = dialogView.findViewById<TextView>(R.id.nameTextView)
        val numberTextView = dialogView.findViewById<TextView>(R.id.numberTextView)
        val emailTextView = dialogView.findViewById<TextView>(R.id.emailTextView)
        val instagramTextView = dialogView.findViewById<TextView>(R.id.instagramTextView)
        val githubTextView = dialogView.findViewById<TextView>(R.id.githubTextView)
        val deleteButton = dialogView.findViewById<Button>(R.id.deleteButton)
        val editButton = dialogView.findViewById<Button>(R.id.editButton)
        val dialog = builder.create()

        nameTextView.text = "이름: ${person.name}"
        numberTextView.text = "전화번호: ${person.number}"
        emailTextView.text = "E-mail: ${person.email}"
        instagramTextView.text = "Instagram ID: ${person.instagram}"
        githubTextView.text = "Github ID: ${person.github}"
        builder.setPositiveButton("닫기", null)

        deleteButton.setOnClickListener {
            deleteContact(person)
            dialog.dismiss()
            adapter.notifyDataSetChanged()
        }

        editButton.setOnClickListener {
            dialog.dismiss()
            showEditContactDialog(person, adapter)
        }

        setToBasicButton.setOnClickListener {
            // 기본 이미지는 ic_name으로 설정
            val drawableId = resources.getIdentifier("ic_name", "drawable", requireContext().packageName)
            profileImage.setImageResource(drawableId)

            currentPerson?.let { person ->
                val updatedPerson = person.copy(imageUri = null)
                updateContact(person, updatedPerson, adapter)
            }
            selectedImageUri = null
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

        // 프로필 이미지 클릭 시 갤러리로 이동
        profileImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, IMAGE_PICK_CODE)
        }

        if (selectedImageUri != null) {
            profileImage.setImageURI(selectedImageUri)
        }

        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_border)
        dialog.show()
        currentDialog = dialog
        dialog.show()
    }

    // 선택한 이미지의 Uri를 반환
    private fun getSelectedImageUriAsString(): String? {
        return selectedImageUri?.toString()
    }

    // 연락처 수정
    private fun showEditContactDialog(person: Person, adapter: PersonAdapter) {
        currentPerson = person
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("연락처 수정")

        val view = layoutInflater.inflate(R.layout.dialog_edit_contact, null)
        builder.setView(view)

        view.findViewById<EditText>(R.id.name).setText(person.name)
        view.findViewById<EditText>(R.id.number).setText(person.number)
        view.findViewById<EditText>(R.id.email).setText(person.email)
        view.findViewById<EditText>(R.id.instagram).setText(person.instagram)
        view.findViewById<EditText>(R.id.github).setText(person.github)

        builder.setPositiveButton("확인", null)
        builder.setNegativeButton("취소", null)

        val dialog = builder.create()
        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val updatedName = view.findViewById<EditText>(R.id.name).text.toString()
                val updatedNumber = view.findViewById<EditText>(R.id.number).text.toString()
                val updatedEmail = view.findViewById<EditText>(R.id.email).text.toString()
                val updatedInstagram = view.findViewById<EditText>(R.id.instagram).text.toString()
                val updatedGithub = view.findViewById<EditText>(R.id.github).text.toString()

                val updatedPerson = Person(updatedName, updatedNumber, updatedEmail, updatedInstagram, updatedGithub, getSelectedImageUriAsString())
                updateContact(person, updatedPerson, adapter)

                dialog.dismiss()
            }
        }

        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_border)
        dialog.show()
        currentDialog = dialog
        dialog.show()
    }


    // 연락처 검색
    private fun searchContacts(searchText: String, searchBy: String): List<Person> {
        val allContacts = readFromFile("numbers.json")

        return when (searchBy) {
            "Name" -> allContacts.filter { it.name.contains(searchText, ignoreCase = true) }
            "Number" -> allContacts.filter { it.number.contains(searchText) }
            else -> emptyList()
        }
    }

    // 연락처 삭제 함수
    private fun deleteContact(person: Person) {
        val fileName = "numbers.json"
        people = readFromFile(fileName)
        people.remove(person)
        writeToFile(fileName, people)
        adapter.updateData(people)
    }

    // 연락처 업데이트 함수
    private fun updateContact(oldPerson: Person, newPerson: Person, adapter: PersonAdapter) {
        val fileName = "numbers.json"
        people = readFromFile(fileName)
        val index = people.indexOf(oldPerson)
        if (index != -1) {
            people[index] = newPerson
            writeToFile(fileName, people)
            adapter.updateData(people)
        }
    }

    // 연락처 검색 Dialog
    private fun showSearchDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("검색")

        val view = layoutInflater.inflate(R.layout.search_dialog, null)
        builder.setView(view)

        val searchBySpinner = view.findViewById<Spinner>(R.id.searchBySpinner)
        val searchText = view.findViewById<EditText>(R.id.searchEditText)

        val searchOptions = arrayOf("Name", "Number")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, searchOptions)
        searchBySpinner.adapter = adapter

        builder.setPositiveButton("검색", null)
        builder.setNegativeButton("취소", null)

        val dialog = builder.create()
        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val selectedSearchBy = searchBySpinner.selectedItem.toString()
                val enteredSearchText = searchText.text.toString()

                val searchResults = searchContacts(enteredSearchText, selectedSearchBy)
                showSearchResultsDialog(searchResults)

                dialog.dismiss()
            }
        }

        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_border)
        dialog.show()
    }

    // 연락처 검색 결과를 보여주는 Dialog
    private fun showSearchResultsDialog(searchResults: List<Person>) {
        val context = context
        if (context != null) {
            val builder = AlertDialog.Builder(context)
            builder.setTitle("검색 결과")

            val view = LayoutInflater.from(context).inflate(R.layout.dialog_list, null)
            val listView = view.findViewById<ListView>(R.id.listView22)

            val personItems = searchResults.map { PersonItem(it.imageUri, it.name, it.number) }

            val adapter = PersonItemAdapter(context, personItems)

            listView.adapter = adapter

            listView.setOnItemClickListener { _, _, position, _ ->
                val selectedPerson = searchResults[position]
                showDetailsDialog(selectedPerson)
            }

            builder.setView(view)
            builder.setPositiveButton("닫기", null)

            val dialog = builder.create()
            dialog.setOnShowListener {
                val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                positiveButton.setOnClickListener {
                    dialog.dismiss()
                }
            }

            dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_border)
            dialog.show()
        }
    }
}