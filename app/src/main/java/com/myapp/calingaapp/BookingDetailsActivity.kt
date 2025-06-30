package com.myapp.calingaapp

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class BookingDetailsActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var firestore: FirebaseFirestore
    
    // UI Elements
    private lateinit var textViewStatus: TextView
    private lateinit var textViewCustomerName: TextView
    private lateinit var textViewAddress: TextView
    private lateinit var textViewTimeSlot: TextView
    private lateinit var textViewDuration: TextView
    private lateinit var textViewRate: TextView
    private lateinit var textViewTotalAmount: TextView
    private lateinit var textViewPaymentMethod: TextView
    private lateinit var textViewNotes: TextView
    private lateinit var textViewCreatedAt: TextView
    private lateinit var textViewCompletedAt: TextView
    private lateinit var layoutCompletedAt: LinearLayout
    private lateinit var layoutActionButtons: LinearLayout
    private lateinit var buttonAccept: Button
    private lateinit var buttonComplete: Button
    
    private var currentBooking: Booking? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking_details)

        // Initialize Firebase
        firestore = FirebaseFirestore.getInstance()

        // Initialize drawer layout
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)

        // Initialize UI elements
        initializeViews()

        // Set up toolbar
        setupToolbar()

        // Get booking data from intent
        getBookingFromIntent()

        // Set up action buttons
        setupActionButtons()
    }

    private fun initializeViews() {
        textViewStatus = findViewById(R.id.textViewStatus)
        textViewCustomerName = findViewById(R.id.textViewCustomerName)
        textViewAddress = findViewById(R.id.textViewAddress)
        textViewTimeSlot = findViewById(R.id.textViewTimeSlot)
        textViewDuration = findViewById(R.id.textViewDuration)
        textViewRate = findViewById(R.id.textViewRate)
        textViewTotalAmount = findViewById(R.id.textViewTotalAmount)
        textViewPaymentMethod = findViewById(R.id.textViewPaymentMethod)
        textViewNotes = findViewById(R.id.textViewNotes)
        textViewCreatedAt = findViewById(R.id.textViewCreatedAt)
        textViewCompletedAt = findViewById(R.id.textViewCompletedAt)
        layoutCompletedAt = findViewById(R.id.layoutCompletedAt)
        layoutActionButtons = findViewById(R.id.layoutActionButtons)
        buttonAccept = findViewById(R.id.buttonAccept)
        buttonComplete = findViewById(R.id.buttonComplete)
    }

    private fun setupToolbar() {
        // Back button
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            onBackPressed()
        }

        // Menu button (optional - for future use)
        findViewById<ImageView>(R.id.imageViewMenu).setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }
    }

    private fun getBookingFromIntent() {
        val bookingId = intent.getStringExtra("BOOKING_ID")
        val careseekerId = intent.getStringExtra("CARESEEKER_ID") ?: ""
        val caregiverName = intent.getStringExtra("CAREGIVER_NAME") ?: ""
        val timeFrom = intent.getStringExtra("TIME_FROM") ?: ""
        val timeTo = intent.getStringExtra("TIME_TO") ?: ""
        val address = intent.getStringExtra("ADDRESS") ?: ""
        val notes = intent.getStringExtra("NOTES") ?: ""
        val status = intent.getStringExtra("STATUS") ?: ""
        val ratePerHour = intent.getDoubleExtra("RATE_PER_HOUR", 0.0)
        val totalHours = intent.getDoubleExtra("TOTAL_HOURS", 0.0)
        val totalAmount = intent.getDoubleExtra("TOTAL_AMOUNT", 0.0)
        val paymentMethod = intent.getStringExtra("PAYMENT_METHOD") ?: ""
        val createdAtMillis = intent.getLongExtra("CREATED_AT", 0L)
        val completedAtMillis = intent.getLongExtra("COMPLETED_AT", 0L)

        // Create booking object
        currentBooking = Booking(
            bookingId = bookingId ?: "",
            careseekerId = careseekerId,
            caregiverName = caregiverName,
            timeFrom = timeFrom,
            timeTo = timeTo,
            address = address,
            notes = notes,
            status = status,
            ratePerHour = ratePerHour,
            totalHours = totalHours,
            totalAmount = totalAmount,
            paymentMethod = paymentMethod,
            createdAt = if (createdAtMillis > 0) Timestamp(Date(createdAtMillis)) else Timestamp.now(),
            completedAt = if (completedAtMillis > 0) Timestamp(Date(completedAtMillis)) else null
        )

        // Populate UI with booking data
        populateBookingDetails()
    }

    private fun populateBookingDetails() {
        currentBooking?.let { booking ->
            // Status
            textViewStatus.text = booking.status.uppercase()
            updateStatusAppearance(booking.status)

            // Customer info - fetch actual careseeker name
            loadCareseekerName(booking.careseekerId)
            textViewAddress.text = booking.address

            // Service details
            textViewTimeSlot.text = "${booking.timeFrom} - ${booking.timeTo}"
            textViewDuration.text = "${booking.totalHours} hours"
            textViewRate.text = "$${booking.ratePerHour.toInt()}/hour"
            textViewTotalAmount.text = "Total: $${booking.totalAmount.toInt()}"
            textViewPaymentMethod.text = "Payment: ${booking.paymentMethod}"

            // Notes
            textViewNotes.text = if (booking.notes.isNotEmpty()) {
                booking.notes
            } else {
                "No special notes provided."
            }

            // Timeline
            val dateFormat = SimpleDateFormat("MMMM dd, yyyy 'at' h:mm a", Locale.getDefault())
            textViewCreatedAt.text = "Created: ${dateFormat.format(booking.createdAt.toDate())}"

            if (booking.completedAt != null) {
                layoutCompletedAt.visibility = View.VISIBLE
                textViewCompletedAt.text = "Completed: ${dateFormat.format(booking.completedAt.toDate())}"
            } else {
                layoutCompletedAt.visibility = View.GONE
            }

            // Action buttons visibility
            updateActionButtons(booking.status)
        }
    }

    private fun updateStatusAppearance(status: String) {
        when (status.lowercase()) {
            "pending" -> {
                textViewStatus.setTextColor(Color.parseColor("#FF9800"))
                textViewStatus.setBackgroundResource(R.drawable.status_pending_background)
            }
            "accepted" -> {
                textViewStatus.setTextColor(Color.parseColor("#4CAF50"))
                textViewStatus.setBackgroundResource(R.drawable.status_accepted_background)
            }
            "completed" -> {
                textViewStatus.setTextColor(Color.parseColor("#2196F3"))
                textViewStatus.setBackgroundResource(R.drawable.status_completed_background)
            }
            "cancelled" -> {
                textViewStatus.setTextColor(Color.parseColor("#F44336"))
                textViewStatus.setBackgroundResource(R.drawable.status_cancelled_background)
            }
        }
    }

    private fun updateActionButtons(status: String) {
        when (status.lowercase()) {
            "pending" -> {
                layoutActionButtons.visibility = View.VISIBLE
                buttonAccept.visibility = View.VISIBLE
                buttonComplete.visibility = View.GONE
            }
            "accepted" -> {
                layoutActionButtons.visibility = View.VISIBLE
                buttonAccept.visibility = View.GONE
                buttonComplete.visibility = View.VISIBLE
            }
            else -> {
                layoutActionButtons.visibility = View.GONE
            }
        }
    }

    private fun setupActionButtons() {
        buttonAccept.setOnClickListener {
            currentBooking?.let { booking ->
                updateBookingStatus(booking, "accepted")
            }
        }

        buttonComplete.setOnClickListener {
            currentBooking?.let { booking ->
                showCompleteServiceDialog(booking)
            }
        }
    }

    private fun updateBookingStatus(booking: Booking, newStatus: String) {
        if (booking.bookingId.isEmpty()) {
            Toast.makeText(this, "Invalid booking ID", Toast.LENGTH_SHORT).show()
            return
        }

        firestore.collection("bookings")
            .document(booking.bookingId)
            .update("status", newStatus)
            .addOnSuccessListener {
                Toast.makeText(this, "Booking ${newStatus.lowercase()}", Toast.LENGTH_SHORT).show()
                
                // Update local booking object and UI
                currentBooking = currentBooking?.copy(status = newStatus)
                populateBookingDetails()
                
                // Set result to refresh previous activity
                setResult(RESULT_OK)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error updating booking: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showCompleteServiceDialog(booking: Booking) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_complete_service, null)
        val notesEditText = dialogView.findViewById<TextInputEditText>(R.id.et_completion_notes)

        AlertDialog.Builder(this)
            .setTitle("Complete Service")
            .setMessage("Add any completion notes for this service:")
            .setView(dialogView)
            .setPositiveButton("Complete") { _, _ ->
                val completionNotes = notesEditText.text.toString().trim()
                completeService(booking, completionNotes)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun completeService(booking: Booking, completionNotes: String) {
        if (booking.bookingId.isEmpty()) {
            Toast.makeText(this, "Invalid booking ID", Toast.LENGTH_SHORT).show()
            return
        }

        val updates = hashMapOf<String, Any>(
            "status" to "completed",
            "completionNotes" to completionNotes,
            "completedAt" to Timestamp.now()
        )

        firestore.collection("bookings")
            .document(booking.bookingId)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Service completed successfully!", Toast.LENGTH_SHORT).show()
                
                // Update local booking object and UI
                currentBooking = currentBooking?.copy(
                    status = "completed",
                    completionNotes = completionNotes,
                    completedAt = Timestamp.now()
                )
                populateBookingDetails()
                
                // Set result to refresh previous activity
                setResult(RESULT_OK)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error completing service: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun loadCareseekerName(careseekerId: String) {
        if (careseekerId.isNotEmpty()) {
            firestore.collection("users").document(careseekerId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val firstName = document.getString("firstName") ?: ""
                        val lastName = document.getString("lastName") ?: ""
                        val fullName = "$firstName $lastName".trim()
                        textViewCustomerName.text = if (fullName.isNotEmpty()) {
                            fullName
                        } else {
                            "Customer"
                        }
                    } else {
                        textViewCustomerName.text = "Customer"
                    }
                }
                .addOnFailureListener {
                    textViewCustomerName.text = "Customer"
                }
        } else {
            textViewCustomerName.text = "Customer"
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> {
                val intent = Intent(this, CaregiverHomeActivity::class.java)
                startActivity(intent)
            }
            R.id.nav_bookings -> {
                val intent = Intent(this, AllBookingsActivity::class.java)
                startActivity(intent)
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
                // Handle logout
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
