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
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class CaregiverHomeActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var recyclerView: RecyclerView
    private lateinit var bookingAdapter: BookingAdapter
    private val bookingList = ArrayList<Booking>()
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_caregiver_home)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        
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

        // Set up adapter for bookings
        bookingAdapter = BookingAdapter(bookingList)
        recyclerView.adapter = bookingAdapter

        // Load today's bookings
        loadTodaysBookings()

        // Set up click listeners
        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener {
            // Handle refresh click
            loadTodaysBookings()
        }
    }

    private fun loadTodaysBookings() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Get current user's profile to get the caregiver name
        db.collection("userProfiles").document(currentUser.uid)
            .get()
            .addOnSuccessListener { profileDoc ->
                if (profileDoc.exists()) {
                    val userProfile = profileDoc.toObject(UserProfile::class.java)
                    val caregiverName = userProfile?.name ?: ""
                    
                    // Get today's date in the format used when booking
                    val today = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date())
                    
                    // Query bookings for today for this caregiver
                    db.collection("bookings")
                        .whereEqualTo("calingaproId", currentUser.uid) // Updated to use calingaproId
                        .whereEqualTo("date", today) // Updated to use date field
                        .get()
                        .addOnSuccessListener { documents ->
                            bookingList.clear()
                            
                            for (document in documents) {
                                // Use the document.toObject method with the new Booking schema
                                val booking = document.toObject(Booking::class.java)
                                if (booking != null) {
                                    bookingList.add(booking)
                                }
                            }
                            
                            // Sort by time
                            bookingList.sortBy { "${it.date} ${it.time}" } // Updated to use new fields
                            bookingAdapter.notifyDataSetChanged()
                            
                            if (bookingList.isEmpty()) {
                                Toast.makeText(this, "No bookings for today", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnFailureListener { exception ->
                            Toast.makeText(this, "Error loading bookings: ${exception.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "Profile not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error loading profile: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
      override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> {
                // We're already in the home screen, just close drawer
                drawerLayout.closeDrawer(GravityCompat.START)
            }
            R.id.nav_map -> {
                // Launch map activity for caregiver
                val intent = Intent(this, MapActivity::class.java)
                intent.putExtra("USER_ROLE", "caregiver")
                startActivity(intent)
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
