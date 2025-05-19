package com.myapp.calingaapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class CaregiverHomeActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var careseekerAdapter: CareseekerAdapter
    private val careseekerList = ArrayList<Careseeker>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_caregiver_home)

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.recyclerViewCareseekers)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Add sample data
        populateCareseekerList()

        // Set up adapter
        careseekerAdapter = CareseekerAdapter(careseekerList)
        recyclerView.adapter = careseekerAdapter

        // Set up click listeners
        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener {
            // Handle search click
        }
    }

    private fun populateCareseekerList() {
        // Add sample careseekers
        careseekerList.add(Careseeker("Maria Santos", 67, "123 Oak Street, Los Angeles, CA 90001"))
        careseekerList.add(Careseeker("Robert Garcia", 72, "456 Pine Avenue, San Diego, CA 92101"))
        careseekerList.add(Careseeker("Elizabeth Chen", 65, "789 Maple Boulevard, San Francisco, CA 94103"))
        careseekerList.add(Careseeker("James Wilson", 80, "101 Cedar Lane, Sacramento, CA 95814"))
        careseekerList.add(Careseeker("Patricia Lopez", 70, "202 Birch Road, Fresno, CA 93721"))
        careseekerList.add(Careseeker("Thomas Brown", 75, "303 Spruce Street, San Jose, CA 95113"))
        careseekerList.add(Careseeker("Margaret Martinez", 68, "404 Redwood Drive, Oakland, CA 94607"))
        careseekerList.add(Careseeker("Michael Johnson", 82, "505 Elm Court, Long Beach, CA 90802"))
        careseekerList.add(Careseeker("Dorothy Taylor", 69, "606 Willow Way, Bakersfield, CA 93301"))
        careseekerList.add(Careseeker("William Davis", 73, "707 Aspen Circle, Anaheim, CA 92805"))
    }
}

// Data class for Careseeker items
data class Careseeker(
    val name: String,
    val age: Int,
    val address: String
)
