package com.myapp.calingaapp

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class BookingActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    
    private var selectedDate: String = ""
    private var selectedTime: String = ""
    private var selectedPaymentMethod: String = ""
    
    // Caregiver details
    private var caregiverName: String = ""
    private var caregiverTier: String = ""
    private var caregiverRate: Int = 0
    private var caregiverEmail: String = ""
    private var caregiverPhone: String = ""
    
    // California payment methods
    private val paymentMethods = arrayOf(
        "Cash",
        "Credit Card (Visa)",
        "Credit Card (MasterCard)",
        "Credit Card (American Express)",
        "Debit Card",
        "Apple Pay",
        "Google Pay",
        "PayPal",
        "Venmo",
        "Zelle",
        "Check"
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking)
        
        // Initialize Firebase
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        
        // Initialize views
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)
        
        // Update navigation header with user info
        updateNavigationHeader()
        
        // Set up menu button click listener
        findViewById<ImageView>(R.id.imageViewMenu).setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }
        
        // Get caregiver details from intent
        caregiverName = intent.getStringExtra("caregiver_name") ?: ""
        caregiverTier = intent.getStringExtra("caregiver_tier") ?: ""
        caregiverRate = intent.getIntExtra("caregiver_rate", 0)
        caregiverEmail = intent.getStringExtra("caregiver_email") ?: ""
        caregiverPhone = intent.getStringExtra("caregiver_phone") ?: ""
        
        setupClickListeners()
    }
    
    private fun setupClickListeners() {
        // Back button
        findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            finish()
        }
        
        // Date selection
        findViewById<LinearLayout>(R.id.layout_date).setOnClickListener {
            showDatePicker()
        }
        
        // Time selection
        findViewById<LinearLayout>(R.id.layout_time).setOnClickListener {
            showTimePicker()
        }
        
        // Payment method selection
        findViewById<LinearLayout>(R.id.layout_payment).setOnClickListener {
            showPaymentMethodDialog()
        }
        
        // Confirm booking button
        findViewById<LinearLayout>(R.id.btn_confirm_booking).setOnClickListener {
            confirmBooking()
        }
    }
    
    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                selectedDate = "${dayOfMonth}/${month + 1}/${year}"
                findViewById<TextView>(R.id.tv_selected_date).text = selectedDate
                findViewById<TextView>(R.id.tv_selected_date).setTextColor(getColor(R.color.black))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        
        // Set minimum date to today
        datePickerDialog.datePicker.minDate = calendar.timeInMillis
        datePickerDialog.show()
    }
    
    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        val timePickerDialog = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                selectedTime = "${hourOfDay}:${minute.toString().padStart(2, '0')}"
                findViewById<TextView>(R.id.tv_selected_time).text = selectedTime
                findViewById<TextView>(R.id.tv_selected_time).setTextColor(getColor(R.color.black))
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false
        )
        timePickerDialog.show()
    }
    
    private fun showPaymentMethodDialog() {
        AlertDialog.Builder(this)
            .setTitle("Select Payment Method")
            .setItems(paymentMethods) { _, which ->
                selectedPaymentMethod = paymentMethods[which]
                findViewById<TextView>(R.id.tv_selected_payment).text = selectedPaymentMethod
                findViewById<TextView>(R.id.tv_selected_payment).setTextColor(getColor(R.color.black))
            }
            .show()
    }
    
    private fun confirmBooking() {
        // Validate inputs
        if (selectedDate.isEmpty()) {
            Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (selectedTime.isEmpty()) {
            Toast.makeText(this, "Please select a time", Toast.LENGTH_SHORT).show()
            return
        }
        
        val address = findViewById<TextInputEditText>(R.id.et_address).text.toString()
        if (address.isEmpty()) {
            Toast.makeText(this, "Please enter service address", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (selectedPaymentMethod.isEmpty()) {
            Toast.makeText(this, "Please select a payment method", Toast.LENGTH_SHORT).show()
            return
        }
        
        val notes = findViewById<TextInputEditText>(R.id.et_notes).text.toString()
        
        // Create booking object
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in to book", Toast.LENGTH_SHORT).show()
            return
        }
        
        val booking = hashMapOf(
            "careseekerUid" to currentUser.uid,
            "caregiverName" to caregiverName,
            "caregiverTier" to caregiverTier,
            "caregiverRate" to caregiverRate,
            "caregiverEmail" to caregiverEmail,
            "caregiverPhone" to caregiverPhone,
            "selectedDate" to selectedDate,
            "selectedTime" to selectedTime,
            "serviceAddress" to address,
            "notes" to notes,
            "paymentMethod" to selectedPaymentMethod,
            "status" to "pending",
            "createdAt" to System.currentTimeMillis()
        )
        
        findViewById<TextView>(R.id.btn_confirm_booking).text = "Booking..."
        
        firestore.collection("bookings")
            .add(booking)
            .addOnSuccessListener {
                Toast.makeText(this, "Booking confirmed! You'll be contacted by $caregiverName soon.", Toast.LENGTH_LONG).show()
                finish()
                
                // Send confirmation email or notification here if needed
                Toast.makeText(this, "Booking confirmed for $selectedDate at $selectedTime", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Booking failed: ${e.message}", Toast.LENGTH_SHORT).show()
                findViewById<TextView>(R.id.btn_confirm_booking).text = "Confirm Booking"
            }
    }
    
    private fun updateNavigationHeader() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val headerView = navigationView.getHeaderView(0)
            val navHeaderName = headerView.findViewById<TextView>(R.id.nav_header_name)
            val navHeaderEmail = headerView.findViewById<TextView>(R.id.nav_header_email)
            
            navHeaderEmail.text = currentUser.email ?: ""
            
            // Load user profile from Firestore
            firestore.collection("userProfiles").document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val displayName = document.getString("name") ?: "User"
                        navHeaderName.text = displayName
                    }
                }
        }
    }
    
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> {
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    firestore.collection("users").document(currentUser.uid)
                        .get()
                        .addOnSuccessListener { document ->
                            if (document != null && document.exists()) {
                                val userRole = document.getString("role")
                                val intent = if (userRole == "caregiver") {
                                    Intent(this, CaregiverHomeActivity::class.java)
                                } else {
                                    Intent(this, CareseekerHomeActivity::class.java)
                                }
                                startActivity(intent)
                                finish()
                            }
                        }
                }
            }
            R.id.nav_map -> {
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    firestore.collection("users").document(currentUser.uid)
                        .get()
                        .addOnSuccessListener { document ->
                            if (document != null && document.exists()) {
                                val userRole = document.getString("role") ?: "careseeker"
                                val intent = Intent(this, MapActivity::class.java)
                                intent.putExtra("USER_ROLE", userRole)
                                startActivity(intent)
                            }
                        }
                }
            }
            R.id.nav_bookings -> {
                // Navigate to home screen for now since BookingsActivity doesn't exist
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    firestore.collection("users").document(currentUser.uid)
                        .get()
                        .addOnSuccessListener { document ->
                            if (document != null && document.exists()) {
                                val userRole = document.getString("role")
                                val intent = if (userRole == "caregiver") {
                                    Intent(this, CaregiverHomeActivity::class.java)
                                } else {
                                    Intent(this, CareseekerHomeActivity::class.java)
                                }
                                startActivity(intent)
                            }
                        }
                }
            }
            R.id.nav_profile -> {
                val intent = Intent(this, ProfileActivity::class.java)
                startActivity(intent)
            }
            R.id.nav_settings -> {
                // Navigate to profile for now since SettingsActivity doesn't exist
                val intent = Intent(this, ProfileActivity::class.java)
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
