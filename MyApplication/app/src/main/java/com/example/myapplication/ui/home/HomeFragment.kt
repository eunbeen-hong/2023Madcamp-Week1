package com.example.myapplication.ui.home

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentHomeBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.*

data class Person(val name: String, val number: String, val email: String, val instagram: String, val github: String)

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val gson = Gson()
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var people: MutableList<Person>
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
        val listView: ListView = binding.listView
        adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, people.map { "${it.name} : ${it.number}" })
        listView.adapter = adapter

        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val person = people[position]
            showDetailsDialog(person)
        }

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


    // 연락처 추가
    private fun showAddContactDialog(fileName: String, adapter: ArrayAdapter<String>) {
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

                val newPerson = Person(name, number, email, instagram, github)
                people = readFromFile(fileName)
                people.add(newPerson)
                writeToFile(fileName, people)

                adapter.clear() // 기존 데이터 제거
                adapter.addAll(people.map { "${it.name} : ${it.number}" }) // 새로운 데이터 추가
                adapter.notifyDataSetChanged() // ListView 갱신

                dialog.dismiss()
            }
        }

        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_border) // 배경 설정

        dialog.show() // AlertDialog 보여주기
    }

    // 세부 정보
    private fun showDetailsDialog(person: Person) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("세부 정보")

        val view = layoutInflater.inflate(R.layout.person_details_dialog, null)
        builder.setView(view)

        val nameTextView = view.findViewById<TextView>(R.id.nameTextView)
        val numberTextView = view.findViewById<TextView>(R.id.numberTextView)
        val emailTextView = view.findViewById<TextView>(R.id.emailTextView)
        val instagramTextView = view.findViewById<TextView>(R.id.instagramTextView)
        val githubTextView = view.findViewById<TextView>(R.id.githubTextView)
        val deleteButton = view.findViewById<Button>(R.id.deleteButton)
        val editButton = view.findViewById<Button>(R.id.editButton)

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

        // Add an OnClickListener for the 'editButton'
        editButton.setOnClickListener {
            dialog.dismiss()
            showEditContactDialog(person, adapter)
        }

        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_border)
        dialog.show()
    }


    private fun showEditContactDialog(person: Person, adapter: ArrayAdapter<String>) {
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

                val updatedPerson = Person(updatedName, updatedNumber, updatedEmail, updatedInstagram, updatedGithub)
                updateContact(person, updatedPerson, adapter)

                dialog.dismiss()
            }
        }

        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_border) // 배경 설정
        dialog.show() // AlertDialog 보여주기
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
        adapter.clear() // 기존 데이터 제거
        adapter.addAll(people.map { "${it.name} : ${it.number}" }) // 새로운 데이터 추가
    }

    private fun updateContact(oldPerson: Person, newPerson: Person, adapter: ArrayAdapter<String>) {
        val fileName = "numbers.json"
        people = readFromFile(fileName)
        val index = people.indexOf(oldPerson)
        if (index != -1) {
            people[index] = newPerson
            writeToFile(fileName, people)
            adapter.clear() // 기존 데이터 제거
            adapter.addAll(people.map { "${it.name} : ${it.number}" }) // 새로운 데이터 추가
            adapter.notifyDataSetChanged() // ListView 갱신
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
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("검색 결과")

        val names = searchResults.map { it.name }.toTypedArray()

        builder.setItems(names) { _, which ->
            val selectedPerson = searchResults[which]
            showDetailsDialog(selectedPerson)
        }

        builder.setPositiveButton("닫기", null)

        val dialog = builder.create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                dialog.dismiss()
            }
        }

        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_border) // 배경 설정
        dialog.show() // AlertDialog 보여주기
    }

}