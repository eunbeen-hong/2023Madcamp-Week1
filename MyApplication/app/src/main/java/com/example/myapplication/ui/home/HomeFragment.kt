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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val fileName = "numbers.json"
        copyAssetToFile(requireContext(), fileName)

        val people = readFromFile(fileName)
        val listView: ListView = binding.listView
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, people.map { "${it.name} : ${it.number}" })
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

        builder.setPositiveButton("확인") { _, _ ->
            val name = view.findViewById<EditText>(R.id.name).text.toString()
            val number = view.findViewById<EditText>(R.id.number).text.toString()
            val email = view.findViewById<EditText>(R.id.email).text.toString()
            val instagram = view.findViewById<EditText>(R.id.instagram).text.toString()
            val github = view.findViewById<EditText>(R.id.github).text.toString()

            val newPerson = Person(name, number, email, instagram, github)
            val people = readFromFile(fileName)
            people.add(newPerson)
            writeToFile(fileName, people)

            adapter.clear() // 기존 데이터 제거
            adapter.addAll(people.map { "${it.name} : ${it.number}" }) // 새로운 데이터 추가
            adapter.notifyDataSetChanged() // ListView 갱신
        }

        builder.setNegativeButton("취소") { dialog, _ -> dialog.cancel() }

        builder.show()
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

        nameTextView.text = "이름: ${person.name}"
        numberTextView.text = "전화번호: ${person.number}"
        emailTextView.text = "E-mail: ${person.email}"
        instagramTextView.text = "Instagram ID: ${person.instagram}"
        githubTextView.text = "Github ID: ${person.github}"

        deleteButton.setOnClickListener {
            deleteContact(person) // <- adapter parameter removed
            builder.create().dismiss() // or use `dialog` variable if you declared it.
        }

        builder.setPositiveButton("닫기") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
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
        val people = readFromFile(fileName)
        people.remove(person)
        writeToFile(fileName, people)
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

        builder.setPositiveButton("검색") { _, _ ->
            val selectedSearchBy = searchBySpinner.selectedItem.toString()
            val enteredSearchText = searchText.text.toString()

            val searchResults = searchContacts(enteredSearchText, selectedSearchBy)
            showSearchResultsDialog(searchResults)
        }

        builder.setNegativeButton("취소") { dialog, _ -> dialog.cancel() }

        builder.show()
    }

    // 연락처 검색 결과 다이얼로그
    private fun showSearchResultsDialog(searchResults: List<Person>) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("검색 결과")

        val names = searchResults.map { it.name }.toTypedArray()

        builder.setItems(names) { _, which ->
            val selectedPerson = searchResults[which]
            showDetailsDialog(selectedPerson)
        }

        builder.setPositiveButton("닫기") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }
}
