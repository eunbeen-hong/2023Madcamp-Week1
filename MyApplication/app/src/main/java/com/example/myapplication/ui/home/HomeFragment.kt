package com.example.myapplication.ui.home

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
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

data class Person(
    val name: String,
    val number: String,
    val email: String,
    val instagram: String,
    val github: String,
    val imageUri: String?
)

data class PersonItem(
    val imageUri: String?,
    val name: String,
    val number: String
)

class PersonItemAdapter(context: Context, private val items: List<PersonItem>) :
    ArrayAdapter<PersonItem>(context, 0, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.dialog_list_item, parent, false)

        val item = items[position]

        val imageView = view.findViewById<ImageView>(R.id.imageView22)
        val textView = view.findViewById<TextView>(R.id.textView22)
        val textView3 = view.findViewById<TextView>(R.id.textView33)

        if (!item.imageUri.isNullOrEmpty()) {
            try {
                val imageUri = Uri.parse(item.imageUri)
                imageView.setImageURI(imageUri)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            // Set default image from drawable
            val drawableId = context.resources.getIdentifier("ic_name", "drawable", context.packageName)
            imageView.setImageResource(drawableId)
        }

        textView.text = item.name
        textView3.text = item.number

        return view
    }
}

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private var currentPerson: Person? = null
    private var currentDialog: AlertDialog? = null
    private var currentImageView: ImageView? = null
    private var selectedImageUri: Uri? = null
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
        val recyclerView: RecyclerView = binding.recyclerView // 변경점 2: listView를 recyclerView로 변경
        adapter = PersonAdapter(people) { person ->
            showDetailsDialog(person)
        }
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext()) // 변경점 3: layoutManager 설정

        binding.addContact.setOnClickListener {
            showAddContactDialog(fileName, adapter)
        }

        binding.searchContact.setOnClickListener {
            showSearchDialog()
        }

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
            // Dialog를 다시 보여주는 대신, 이미지뷰만 업데이트한다.
            currentImageView?.setImageURI(data?.data)
        }
    }

    private fun copyAssetToFile(context: Context, fileName: String) {
        val file = File(context.filesDir, fileName)
        if (file.exists()) {
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
                    // Set default image from drawable
                    val drawableId = view.resources.getIdentifier("ic_name", "drawable", view.context.packageName)
                    imageView.setImageResource(drawableId)
                }
                view.setOnClickListener { onClick(person) }
            }
        }

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




    // 연락처 추가
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
                val number = if (view.findViewById<EditText>(R.id.number).text.toString().isBlank()) "none" else view.findViewById<EditText>(R.id.number).text.toString()
                val email = if (view.findViewById<EditText>(R.id.email).text.toString().isBlank()) "none" else view.findViewById<EditText>(R.id.email).text.toString()
                val instagram = if (view.findViewById<EditText>(R.id.instagram).text.toString().isBlank()) "none" else view.findViewById<EditText>(R.id.instagram).text.toString()
                val github = if (view.findViewById<EditText>(R.id.github).text.toString().isBlank()) "none" else view.findViewById<EditText>(R.id.github).text.toString()

                val newPerson = Person(name, number, email, instagram, github, getSelectedImageUriAsString())
                people = readFromFile(fileName)
                people.add(newPerson)
                writeToFile(fileName, people)

                adapter.updateData(people)

                dialog.dismiss()
            }
        }

        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_border) // 배경 설정

        dialog.show() // AlertDialog 보여주기
    }

    // 세부 정보
    private fun showDetailsDialog(person: Person) {
        currentPerson = person
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("세부 정보")

        val dialogView = LayoutInflater.from(context).inflate(R.layout.person_details_dialog, null)
        val setToBasicButton = dialogView.findViewById<Button>(R.id.setToBasic)
        val profileImage = dialogView.findViewById<ImageView>(R.id.profileImage)
        builder.setView(dialogView)
        currentImageView = profileImage

        // Set default image from drawable
        val drawableId = resources.getIdentifier("ic_name", "drawable", requireContext().packageName)
        profileImage.setImageResource(drawableId)

        // If user selected an image, set it
        if (selectedImageUri != null) {
            profileImage.setImageURI(selectedImageUri)
        }
        // Or if there is a saved image URI for the person, set that
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

        // Update the TextViews
        nameTextView.text = "이름: ${person.name}"
        numberTextView.text = "전화번호: ${person.number}"
        emailTextView.text = "E-mail: ${person.email}"
        instagramTextView.text = "Instagram ID: ${person.instagram}"
        githubTextView.text = "Github ID: ${person.github}"

        builder.setPositiveButton("닫기", null)

        val dialog = builder.create()

        deleteButton.setOnClickListener {
            deleteContact(person)
            dialog.dismiss()
            adapter.notifyDataSetChanged()
        }

        setToBasicButton.setOnClickListener {
            // Set the image view to the 'ic_name' drawable
            val drawableId = resources.getIdentifier("ic_name", "drawable", requireContext().packageName)
            profileImage.setImageResource(drawableId)
            // You may also want to update the Person object and its imageUri property
            currentPerson?.let { person ->
                val updatedPerson = person.copy(imageUri = null) // Assuming 'ic_name' is the default image
                updateContact(person, updatedPerson, adapter)
            }
            selectedImageUri = null // Updating selectedImageUri to null as 'ic_name' is now the default image
        }


        // Add an OnClickListener for the 'editButton'
        editButton.setOnClickListener {
            dialog.dismiss()
            showEditContactDialog(person, adapter)
        }

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




    private fun getSelectedImageUriAsString(): String? {
        return selectedImageUri?.toString()
    }


    private fun showEditContactDialog(person: Person, adapter: PersonAdapter) {
        currentPerson = person
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("연락처 수정")

        val view = layoutInflater.inflate(R.layout.dialog_edit_contact, null)
        builder.setView(view)

        // Set existing values
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

        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_border) // 배경 설정
        dialog.show() // AlertDialog 보여주기

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

    private fun deleteContact(person: Person) {
        val fileName = "numbers.json"
        people = readFromFile(fileName)
        people.remove(person)
        writeToFile(fileName, people)
        adapter.updateData(people)
    }

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


    // 연락처 검색 결과
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

        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_border) // 배경 설정

        dialog.show() // AlertDialog 보여주기
    }

    // 연락처 검색 결과
    private fun showSearchResultsDialog(searchResults: List<Person>) {
        val context = context
        if (context != null) {
            val builder = AlertDialog.Builder(context)
            builder.setTitle("검색 결과")

            // Inflate the custom layout
            val view = LayoutInflater.from(context).inflate(R.layout.dialog_list, null)
            val listView = view.findViewById<ListView>(R.id.listView22)

            // Create a list of PersonItem objects
            val personItems = searchResults.map { PersonItem(it.imageUri, it.name, it.number) }

            // Create an adapter
            val adapter = PersonItemAdapter(context, personItems)

            // Set the adapter to the ListView
            listView.adapter = adapter

            // Set the item click listener
            listView.setOnItemClickListener { _, _, position, _ ->
                val selectedPerson = searchResults[position]
                showDetailsDialog(selectedPerson)
            }

            // Set the custom view to the dialog
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