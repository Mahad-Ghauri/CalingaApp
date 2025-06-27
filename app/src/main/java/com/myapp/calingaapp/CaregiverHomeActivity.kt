package com.myapp.calingaapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.MenuItem
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.cardview.widget.CardView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
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
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var miniMapCard: CardView
    private val bookingList = ArrayList<Booking>()
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var locationManager: LocationManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_caregiver_home)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        locationManager = LocationManager(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        // Initialize UI components
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        miniMapCard = findViewById(R.id.miniMapCard)
        
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

        // Set up swipe refresh
        swipeRefreshLayout.setOnRefreshListener {
            loadTodaysBookings()
        }
        
        // Set up swipe refresh colors
        swipeRefreshLayout.setColorSchemeResources(
            R.color.primary_color,
            R.color.secondary_color,
            R.color.accent_color
        )

        // Mini map click listener
        miniMapCard.setOnClickListener {
            val intent = Intent(this, MapActivity::class.java)
            intent.putExtra("USER_ROLE", "calingapro")
            startActivity(intent)
        }

        // Load today's bookings
        loadTodaysBookings()

        // Update CalingaPro location and set active status
        updateLocation()

        // Set up click listeners
        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener {
            // Handle refresh click
            loadTodaysBookings()
        }
    }

    private fun updateLocation() {
        // Check location permissions
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request location permissions
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                1
            )
            return
        }

        // Get current location
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                // Update location in Firestore
                locationManager.updateUserLocation(
                    location.latitude,
                    location.longitude
                ) { success, error ->
                    if (success) {
                        // Location updated successfully
                        // Set user as active
                        locationManager.setUserActiveStatus(true) { activeSuccess, activeError ->
                            if (!activeSuccess && activeError != null) {
                                Toast.makeText(this, "Failed to set active status: $activeError", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else if (error != null) {
                        Toast.makeText(this, "Failed to update location: $error", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                updateLocation()
            } else {
                Toast.makeText(this, "Location permission is required for CalingaPro services", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadTodaysBookings() {
        swipeRefreshLayout.isRefreshing = true
        val currentUser = auth.currentUser
        if (currentUser == null) {
            swipeRefreshLayout.isRefreshing = false
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
                            swipeRefreshLayout.isRefreshing = false
                            
                            if (bookingList.isEmpty()) {
                                Toast.makeText(this, "No bookings for today", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnFailureListener { exception ->
                            swipeRefreshLayout.isRefreshing = false
                            Toast.makeText(this, "Error loading bookings: ${exception.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    swipeRefreshLayout.isRefreshing = false
                    Toast.makeText(this, "Profile not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                swipeRefreshLayout.isRefreshing = false
                Toast.makeText(this, "Error loading profile: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
      override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> {
                // We're already in the home screen, just close drawer
                drawerLayout.closeDrawer(GravityCompat.START)
            }
            R.id.nav_bookings -> {
                // Launch all bookings activity
                val intent = Intent(this, AllBookingsActivity::class.java)
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
                // Set user as inactive before logging out
                locationManager.setUserActiveStatus(false) { success, error ->
                    // Log out user regardless of location update success
                    auth.signOut()
                    Toast.makeText(this, "Signed out successfully", Toast.LENGTH_SHORT).show()
                    
                    // Go to login screen
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
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
