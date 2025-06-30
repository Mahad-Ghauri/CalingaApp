package com.myapp.calingaapp

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class AllBookingsActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var bookingAdapter: BookingAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyStateText: TextView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private val bookingList = ArrayList<Booking>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_bookings)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize drawer layout
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)

        // Set up toolbar navigation
        setupToolbar()

        // Initialize UI components
        recyclerView = findViewById(R.id.recyclerViewBookings)
        progressBar = findViewById(R.id.progressBar)
        emptyStateText = findViewById(R.id.emptyStateText)

        // Set up RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        bookingAdapter = BookingAdapter(bookingList, this)
        recyclerView.adapter = bookingAdapter

        // Load all bookings
        loadAllBookings()
    }

    private fun setupToolbar() {
        // Back button
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            onBackPressed()
        }

        // Menu button
        findViewById<ImageView>(R.id.imageViewMenu).setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }
    }

    private fun loadAllBookings() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        showProgress(true)

        // Query all bookings for this caregiver, ordered by creation date descending (newest first)
        db.collection("bookings")
            .whereEqualTo("calingaproId", currentUser.uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                bookingList.clear()

                for (document in documents) {
                    val booking = document.toObject(Booking::class.java)
                    if (booking != null) {
                        bookingList.add(booking)
                    }
                }

                bookingAdapter.notifyDataSetChanged()
                showProgress(false)

                // Show empty state if no bookings
                if (bookingList.isEmpty()) {
                    showEmptyState(true)
                } else {
                    showEmptyState(false)
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error loading bookings: ${exception.message}", Toast.LENGTH_SHORT).show()
                showProgress(false)
                showEmptyState(true)
            }
    }

    private fun showProgress(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        recyclerView.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun showEmptyState(show: Boolean) {
        emptyStateText.visibility = if (show) View.VISIBLE else View.GONE
        recyclerView.visibility = if (show) View.GONE else View.VISIBLE
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> {
                val intent = Intent(this, CaregiverHomeActivity::class.java)
                startActivity(intent)
            }
            R.id.nav_bookings -> {
                // We're already in the bookings screen, just close drawer
                drawerLayout.closeDrawer(GravityCompat.START)
            }
            R.id.nav_profile -> {
                val intent = Intent(this, ProfileActivity::class.java)
                startActivity(intent)
            }
            R.id.nav_documents -> {
                val intent = Intent(this, DocumentsSubmissionActivity::class.java)
                startActivity(intent)
            }
            R.id.nav_logout -> {
                auth.signOut()
                Toast.makeText(this, "Signed out successfully", Toast.LENGTH_SHORT).show()
                
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
