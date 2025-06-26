package com.myapp.calingaapp

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class BookingActivity : AppCompatActivity() {
    
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
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
        "Bank Transfer",
        "Check"
    )
      override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking)
          // Initialize Firebase
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        
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
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        
        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val calendar = Calendar.getInstance()
                calendar.set(selectedYear, selectedMonth, selectedDay)
                
                val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
                selectedDate = dateFormat.format(calendar.time)
                  findViewById<TextView>(R.id.tv_selected_date).text = selectedDate
                findViewById<TextView>(R.id.tv_selected_date).setTextColor(getColor(R.color.black))
            },
            year, month, day
        )
        
        // Set minimum date to today
        datePickerDialog.datePicker.minDate = System.currentTimeMillis()
        datePickerDialog.show()
    }
    
    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        
        val timePickerDialog = TimePickerDialog(
            this,
            { _, selectedHour, selectedMinute ->
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.HOUR_OF_DAY, selectedHour)
                calendar.set(Calendar.MINUTE, selectedMinute)
                
                val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                selectedTime = timeFormat.format(calendar.time)
                  findViewById<TextView>(R.id.tv_selected_time).text = selectedTime
                findViewById<TextView>(R.id.tv_selected_time).setTextColor(getColor(R.color.black))
            },
            hour, minute, false
        )
        
        timePickerDialog.show()
    }
    
    private fun showPaymentMethodDialog() {        AlertDialog.Builder(this)
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
            Toast.makeText(this, "Please select a preferred date", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (selectedTime.isEmpty()) {
            Toast.makeText(this, "Please select a preferred time", Toast.LENGTH_SHORT).show()
            return
        }
        
        val address = findViewById<TextInputEditText>(R.id.et_address).text.toString().trim()
        if (address.isEmpty()) {
            Toast.makeText(this, "Please enter the service address", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (selectedPaymentMethod.isEmpty()) {
            Toast.makeText(this, "Please select a payment method", Toast.LENGTH_SHORT).show()
            return
        }
        
        val notes = findViewById<TextInputEditText>(R.id.et_notes).text.toString().trim()
        
        // Create booking data
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }
          val bookingData = hashMapOf(
            "userId" to currentUser.uid,
            "userEmail" to currentUser.email,
            "caregiverName" to caregiverName,
            "caregiverTier" to caregiverTier,
            "caregiverRate" to caregiverRate,
            "caregiverEmail" to caregiverEmail,
            "caregiverPhone" to caregiverPhone,
            "preferredDate" to selectedDate,
            "preferredTime" to selectedTime,
            "serviceAddress" to address,
            "notes" to notes,
            "paymentMethod" to selectedPaymentMethod,
            "bookingStatus" to "pending",
            "createdAt" to System.currentTimeMillis(),
            "timestamp" to com.google.firebase.Timestamp.now()
        )
          // Show loading state
        findViewById<LinearLayout>(R.id.btn_confirm_booking).isEnabled = false
        
        // Save to Firebase
        firestore.collection("bookings")
            .add(bookingData)
            .addOnSuccessListener { documentReference ->
                Toast.makeText(this, "Booking confirmed successfully!", Toast.LENGTH_LONG).show()
                  // Show success dialog
                val caregiverInfo = if (caregiverName.isNotEmpty()) " with $caregiverName" else ""
                AlertDialog.Builder(this)
                    .setTitle("Booking Confirmed")
                    .setMessage("Your appointment has been scheduled$caregiverInfo for $selectedDate at $selectedTime. You will receive a confirmation shortly.")
                    .setPositiveButton("OK") { _, _ ->
                        finish()
                    }
                    .setCancelable(false)
                    .show()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to confirm booking: ${exception.message}", Toast.LENGTH_LONG).show()
                findViewById<LinearLayout>(R.id.btn_confirm_booking).isEnabled = true
            }
    }
}
