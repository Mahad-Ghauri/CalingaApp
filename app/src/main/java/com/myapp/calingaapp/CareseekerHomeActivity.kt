package com.myapp.calingaapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.TextView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class CareseekerHomeActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var calingaProAdapter: CalingaProAdapter
    private lateinit var editTextSearch: EditText
    private val calingaProList = ArrayList<CalingaPro>()
    private val filteredList = ArrayList<CalingaPro>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_careseeker_home)

        // Set patient info (in a real app, this would come from a database or intent)
        findViewById<TextView>(R.id.textViewPatientName).text = "Maria Santos"
        findViewById<TextView>(R.id.textViewPatientAge).text = "67 years old"
        findViewById<TextView>(R.id.textViewPatientAddress).text = "123 Oak Street, Los Angeles, CA 90001"

        // Initialize search bar
        editTextSearch = findViewById(R.id.editTextSearch)
        editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterProfessionals(s.toString())
            }
        })

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.recyclerViewCaregivers)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Add sample data
        populateCalingaProList()
        filteredList.addAll(calingaProList)

        // Set up adapter
        calingaProAdapter = CalingaProAdapter(filteredList)
        recyclerView.adapter = calingaProAdapter

        // Set up click listeners
        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener {
            // Handle filter click
        }
    }

    private fun filterProfessionals(query: String) {
        filteredList.clear()
        
        if (query.isEmpty()) {
            filteredList.addAll(calingaProList)
        } else {
            val lowerCaseQuery = query.lowercase()
            for (pro in calingaProList) {
                if (pro.name.lowercase().contains(lowerCaseQuery) || 
                    pro.tier.lowercase().contains(lowerCaseQuery) || 
                    pro.address.lowercase().contains(lowerCaseQuery)) {
                    filteredList.add(pro)
                }
            }
        }
        
        calingaProAdapter.notifyDataSetChanged()
    }

    private fun populateCalingaProList() {
        // Add the sample data provided
        calingaProList.add(CalingaPro("Emma Brown", "Basic", "100 Market Street, San Francisco, CA 94103", 20))
        calingaProList.add(CalingaPro("John Reyes", "CNA", "456 Palm Street, San Diego, CA 92101", 22))
        calingaProList.add(CalingaPro("Angela Cruz", "LVN", "789 Ocean Avenue, Long Beach, CA 90802", 28))
        calingaProList.add(CalingaPro("David Molina", "RN", "321 Sunset Drive, Sacramento, CA 95814", 35))
        calingaProList.add(CalingaPro("Kristine Gomez", "NP", "654 Mission Street, San Francisco, CA 94105", 45))
        calingaProList.add(CalingaPro("Mark Bautista", "PT", "987 Vine Hill Road, Fremont, CA 94539", 40))
        calingaProList.add(CalingaPro("Jennifer De Guzman", "Basic", "111 Palm Grove Lane, Anaheim, CA 92801", 21))
        calingaProList.add(CalingaPro("Michael Chua", "CNA", "222 Sunrise Blvd, Pasadena, CA 91103", 23))
        calingaProList.add(CalingaPro("Camille Navarro", "RN", "333 Laurel Canyon Road, Burbank, CA 91505", 34))
        calingaProList.add(CalingaPro("Bryan Dela Cruz", "NP", "444 Golden State Avenue, Bakersfield, CA 93301", 46))
    }
}

// Data class for CALINGApro items
data class CalingaPro(
    val name: String,
    val tier: String,
    val address: String,
    val rate: Int
)
