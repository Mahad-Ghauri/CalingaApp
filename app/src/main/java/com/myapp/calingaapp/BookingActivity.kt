package com.myapp.calingaapp

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
import kotlin.math.roundToInt

class BookingActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    
    private var selectedTimeFrom: Calendar? = null
    private var selectedTimeTo: Calendar? = null
    private var selectedPaymentMethod: String = ""
    
    // Caregiver details
    private var caregiverName: String = ""
    private var caregiverTier: String = ""
    private var caregiverRate: Double = 0.0
    private var caregiverEmail: String = ""
    private var caregiverPhone: String = ""
    
    // Stripe-compatible payment methods
    private val paymentMethods = arrayOf(
        "Credit Card",
        "Debit Card", 
        "Digital Wallet (Apple Pay/Google Pay)",
        "Bank Transfer"
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking_new)
        
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
        caregiverRate = intent.getDoubleExtra("caregiver_rate", 0.0)
        caregiverEmail = intent.getStringExtra("caregiver_email") ?: ""
        caregiverPhone = intent.getStringExtra("caregiver_phone") ?: ""
        
        // Display caregiver rate
        findViewById<TextView>(R.id.tv_rate_display).text = "$${caregiverRate.roundToInt()}/hr"
        
        setupClickListeners()
    }
    
    private fun setupClickListeners() {
        // Back button
        findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            finish()
        }

        // Time from selection
        findViewById<LinearLayout>(R.id.layout_time_from).setOnClickListener {
            showTimeFromPicker()
        }

        // Time to selection
        findViewById<LinearLayout>(R.id.layout_time_to).setOnClickListener {
            showTimeToPicker()
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
    
    private fun showTimeFromPicker() {
        val calendar = Calendar.getInstance()
        // Add 15 minutes to current time for minimum start time
        calendar.add(Calendar.MINUTE, 15)
        
        val timePickerDialog = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                val selectedTime = Calendar.getInstance()
                selectedTime.set(Calendar.HOUR_OF_DAY, hourOfDay)
                selectedTime.set(Calendar.MINUTE, minute)
                selectedTime.set(Calendar.SECOND, 0)
                selectedTime.set(Calendar.MILLISECOND, 0)
                
                // Check if selected time is at least 15 minutes from now
                val now = Calendar.getInstance()
                now.add(Calendar.MINUTE, 15)
                
                if (selectedTime.before(now)) {
                    Toast.makeText(this, "Start time must be at least 15 minutes from now", Toast.LENGTH_SHORT).show()
                    return@TimePickerDialog
                }
                
                selectedTimeFrom = selectedTime
                val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                findViewById<TextView>(R.id.tv_selected_time_from).text = timeFormat.format(selectedTime.time)
                findViewById<TextView>(R.id.tv_selected_time_from).setTextColor(getColor(R.color.black))
                
                // Reset "to" time if it's before the new "from" time
                if (selectedTimeTo != null && selectedTimeTo!!.before(selectedTime)) {
                    selectedTimeTo = null
                    findViewById<TextView>(R.id.tv_selected_time_to).text = "Select End Time"
                    findViewById<TextView>(R.id.tv_selected_time_to).setTextColor(getColor(R.color.text_secondary))
                }
                
                updateTotalPrice()
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false
        )
        timePickerDialog.show()
    }

    private fun showTimeToPicker() {
        if (selectedTimeFrom == null) {
            Toast.makeText(this, "Please select start time first", Toast.LENGTH_SHORT).show()
            return
        }
        
        val calendar = Calendar.getInstance()
        calendar.time = selectedTimeFrom!!.time
        calendar.add(Calendar.HOUR, 1) // Minimum 1 hour duration
        
        val timePickerDialog = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                val selectedTime = Calendar.getInstance()
                selectedTime.set(Calendar.HOUR_OF_DAY, hourOfDay)
                selectedTime.set(Calendar.MINUTE, minute)
                selectedTime.set(Calendar.SECOND, 0)
                selectedTime.set(Calendar.MILLISECOND, 0)
                
                // Check if end time is after start time
                if (selectedTime.before(selectedTimeFrom) || selectedTime.equals(selectedTimeFrom)) {
                    Toast.makeText(this, "End time must be after start time", Toast.LENGTH_SHORT).show()
                    return@TimePickerDialog
                }
                
                selectedTimeTo = selectedTime
                val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                findViewById<TextView>(R.id.tv_selected_time_to).text = timeFormat.format(selectedTime.time)
                findViewById<TextView>(R.id.tv_selected_time_to).setTextColor(getColor(R.color.black))
                
                updateTotalPrice()
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false
        )
        timePickerDialog.show()
    }
    
    private fun updateTotalPrice() {
        if (selectedTimeFrom != null && selectedTimeTo != null) {
            val durationMillis = selectedTimeTo!!.timeInMillis - selectedTimeFrom!!.timeInMillis
            val durationHours = durationMillis / (1000 * 60 * 60.0) // Convert to hours with decimals
            val totalAmount = durationHours * caregiverRate
            
            findViewById<TextView>(R.id.tv_duration_display).text = String.format("%.1f hours", durationHours)
            findViewById<TextView>(R.id.tv_total_amount).text = String.format("$%.2f", totalAmount)
        } else {
            findViewById<TextView>(R.id.tv_duration_display).text = "0 hours"
            findViewById<TextView>(R.id.tv_total_amount).text = "$0.00"
        }
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
        if (selectedTimeFrom == null) {
            Toast.makeText(this, "Please select start time", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedTimeTo == null) {
            Toast.makeText(this, "Please select end time", Toast.LENGTH_SHORT).show()
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

        // Calculate duration and total amount
        val durationMillis = selectedTimeTo!!.timeInMillis - selectedTimeFrom!!.timeInMillis
        val totalHours = durationMillis / (1000 * 60 * 60.0)
        val totalAmount = totalHours * caregiverRate

        // Show confirmation dialog
        showBookingConfirmationDialog(address, notes, totalHours, totalAmount)
    }

    private fun showBookingConfirmationDialog(address: String, notes: String, totalHours: Double, totalAmount: Double) {
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val fromTime = timeFormat.format(selectedTimeFrom!!.time)
        val toTime = timeFormat.format(selectedTimeTo!!.time)
        
        val message = """
            Booking Summary:
            
            Caregiver: $caregiverName ($caregiverTier)
            Time: $fromTime - $toTime
            Duration: ${String.format("%.1f", totalHours)} hours
            Location: $address
            Payment: $selectedPaymentMethod
            Total: ${String.format("$%.2f", totalAmount)}
            
            Note: This is a temporary confirmation. Payment will be processed through Stripe when integrated.
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Confirm Booking")
            .setMessage(message)
            .setPositiveButton("Confirm") { _, _ ->
                processBooking(address, notes, totalHours, totalAmount)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun processBooking(address: String, notes: String, totalHours: Double, totalAmount: Double) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in to book", Toast.LENGTH_SHORT).show()
            return
        }

        // Get caregiver ID from intent
        val caregiverUid = intent.getStringExtra("caregiver_uid") ?: ""
        if (caregiverUid.isEmpty()) {
            Toast.makeText(this, "Caregiver information missing", Toast.LENGTH_SHORT).show()
            return
        }

        // Format times for storage
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val timeFromString = timeFormat.format(selectedTimeFrom!!.time)
        val timeToString = timeFormat.format(selectedTimeTo!!.time)

        // Create new booking using updated Booking data model
        val bookingId = firestore.collection("bookings").document().id
        val booking = Booking(
            bookingId = bookingId,
            careseekerId = currentUser.uid,
            calingaproId = caregiverUid,
            caregiverTier = caregiverTier,
            caregiverName = caregiverName,
            timeFrom = timeFromString,
            timeTo = timeToString,
            totalHours = totalHours,
            address = address,
            notes = notes,
            ratePerHour = caregiverRate,
            totalAmount = totalAmount,
            paymentMethod = selectedPaymentMethod,
            status = "pending",
            createdAt = com.google.firebase.Timestamp.now()
        )
        
        findViewById<TextView>(R.id.tv_confirm_booking_text).text = "Processing..."
        
        firestore.collection("bookings")
            .document(bookingId)
            .set(booking.toMap())
            .addOnSuccessListener {
                // Create notification for the caregiver
                createNotificationForCaregiver(caregiverUid, timeFromString, timeToString)
                
                // Show success dialog with redirect to My Bookings
                showBookingSuccessDialog()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Booking failed: ${e.message}", Toast.LENGTH_SHORT).show()
                findViewById<TextView>(R.id.tv_confirm_booking_text).text = "Confirm Booking"
            }
    }

    private fun showBookingSuccessDialog() {
        AlertDialog.Builder(this)
            .setTitle("Booking Confirmed!")
            .setMessage("Your booking has been confirmed successfully. You'll be contacted by $caregiverName soon.")
            .setPositiveButton("View My Bookings") { _, _ ->
                // Redirect to My Bookings page
                val intent = Intent(this, CareseekerBookingsActivity::class.java)
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Continue Browsing") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun createNotificationForCaregiver(caregiverUid: String, timeFrom: String, timeTo: String) {
        val notificationId = firestore.collection("notifications").document().id
        val displayTimeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val inputTimeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        
        try {
            val fromTime = inputTimeFormat.parse(timeFrom)
            val toTime = inputTimeFormat.parse(timeTo)
            val fromDisplay = displayTimeFormat.format(fromTime!!)
            val toDisplay = displayTimeFormat.format(toTime!!)
            
            val notification = Notification(
                notificationId = notificationId,
                userId = caregiverUid,
                title = "New Booking Request",
                message = "You have a new booking request for $fromDisplay - $toDisplay",
                seen = false,
                timestamp = com.google.firebase.Timestamp.now()
            )
            
            firestore.collection("notifications")
                .document(notificationId)
                .set(notification.toMap())
                .addOnFailureListener {
                    // Log error but don't show to user as booking was successful
                }
        } catch (e: Exception) {
            // Handle time parsing error
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
                // Navigate to CareseekerBookingsActivity for careseekers
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    firestore.collection("users").document(currentUser.uid)
                        .get()
                        .addOnSuccessListener { document ->
                            if (document != null && document.exists()) {
                                val userRole = document.getString("role")
                                if (userRole == "careseeker") {
                                    val intent = Intent(this, CareseekerBookingsActivity::class.java)
                                    startActivity(intent)
                                } else {
                                    // For caregivers, redirect to home for now
                                    val intent = Intent(this, CaregiverHomeActivity::class.java)
                                    startActivity(intent)
                                }
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
