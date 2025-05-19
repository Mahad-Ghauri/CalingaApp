package com.myapp.calingaapp

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth

class CaregiverHomeActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var recyclerView: RecyclerView
    private lateinit var careseekerAdapter: CareseekerAdapter
    private val careseekerList = ArrayList<Careseeker>()
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_caregiver_home)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        
        // Initialize UI components
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        
        // Set up navigation with caregiver-specific menu
        navigationView.inflateMenu(R.menu.caregiver_drawer_menu)
        navigationView.setNavigationItemSelectedListener(this)
        
        // Set up custom menu icon to control drawer
        findViewById<ImageView>(R.id.imageViewMenu).setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }

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
    
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> {
                // We're already in the home screen, just close drawer
                drawerLayout.closeDrawer(GravityCompat.START)
            }
            R.id.nav_profile -> {
                // Launch profile activity
                val intent = Intent(this, ProfileActivity::class.java)
                startActivity(intent)
            }
            R.id.nav_documents -> {
                // Launch documents submission activity
                val intent = Intent(this, DocumentsSubmissionActivity::class.java)
                startActivity(intent)
            }
            R.id.nav_logout -> {
                // Log out user
                auth.signOut()
                Toast.makeText(this, "Signed out successfully", Toast.LENGTH_SHORT).show()
                
                // Go to login screen
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }
        
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }
    
    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}

// Data class for Careseeker items
data class Careseeker(
    val name: String,
    val age: Int,
    val address: String
)
