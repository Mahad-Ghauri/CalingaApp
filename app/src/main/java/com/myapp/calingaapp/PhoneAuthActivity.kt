package com.myapp.calingaapp

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.myapp.calingaapp.services.TwilioService

class PhoneAuthActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var twilioService: TwilioService
    
    private lateinit var otpEditText: EditText
    private lateinit var tilOtp: TextInputLayout
    private lateinit var sendOtpButton: Button
    private lateinit var verifyOtpButton: Button
    private lateinit var resendOtpButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var timerTextView: TextView
    private lateinit var statusTextView: TextView
    private lateinit var phoneDisplayTextView: TextView
    
    private var countDownTimer: CountDownTimer? = null
    private var isOtpSent: Boolean = false
    
    // Data passed from RegisterActivity
    private var fullName: String = ""
    private var email: String = ""
    private var phone: String = ""
    private var password: String = ""
    private var role: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_phone_auth)
        
        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        
        // Initialize Twilio service
        twilioService = TwilioService(this)
        
        // Get data from intent
        fullName = intent.getStringExtra("fullName") ?: ""
        email = intent.getStringExtra("email") ?: ""
        phone = intent.getStringExtra("phone") ?: ""
        password = intent.getStringExtra("password") ?: ""
        role = intent.getStringExtra("userType") ?: ""
        
        initializeViews()
        setupListeners()
        
        // Display phone number
        phoneDisplayTextView.text = "We'll send an OTP to $phone"
    }
    
    private fun initializeViews() {
        otpEditText = findViewById(R.id.et_otp)
        tilOtp = findViewById(R.id.til_otp)
        sendOtpButton = findViewById(R.id.btn_send_otp)
        verifyOtpButton = findViewById(R.id.btn_verify_otp)
        resendOtpButton = findViewById(R.id.btn_resend_otp)
        progressBar = findViewById(R.id.progress_bar)
        timerTextView = findViewById(R.id.tv_timer)
        statusTextView = findViewById(R.id.tv_status)
        phoneDisplayTextView = findViewById(R.id.tv_phone_display)
    }
    
    private fun setupListeners() {
        sendOtpButton.setOnClickListener {
            sendOtp()
        }
        
        verifyOtpButton.setOnClickListener {
            verifyOtp()
        }
        
        resendOtpButton.setOnClickListener {
            sendOtp()
        }
    }
    
    private fun sendOtp() {
        showProgress(true)
        updateStatus("Sending OTP...")
        
        twilioService.sendOtp(phone) { success, message ->
            runOnUiThread {
                showProgress(false)
                if (success) {
                    isOtpSent = true
                    updateStatus("OTP sent to $phone")
                    showOtpSection()
                    startCountdown()
                } else {
                    updateStatus("Failed to send OTP: $message")
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun verifyOtp() {
        val enteredOtp = otpEditText.text.toString().trim()
        
        if (enteredOtp.length != 6) {
            otpEditText.error = "Please enter a valid 6-digit OTP"
            return
        }
        
        showProgress(true)
        updateStatus("Verifying OTP...")
        
        twilioService.verifyOtp(phone, enteredOtp) { success, message ->
            runOnUiThread {
                showProgress(false)
                if (success) {
                    updateStatus("Phone verified! Creating account...")
                    createUserAccount()
                } else {
                    updateStatus("Verification failed: $message")
                    otpEditText.error = message
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun createUserAccount() {
        showProgress(true)
        updateStatus("Creating your account...")
        
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        saveUserDataToFirestore(user.uid)
                    }
                } else {
                    showProgress(false)
                    updateStatus("Failed to create account")
                    Toast.makeText(baseContext, "Authentication failed: ${task.exception?.message}",
                        Toast.LENGTH_SHORT).show()
                }
            }
    }
    
    private fun saveUserDataToFirestore(userId: String) {
        val userData = hashMapOf(
            "fullName" to fullName,
            "email" to email,
            "phone" to phone,
            "role" to role,
            "isPhoneVerified" to true,
            "createdAt" to System.currentTimeMillis(),
            "isActive" to true
        )
        
        db.collection("users").document(userId)
            .set(userData)
            .addOnSuccessListener {
                showProgress(false)
                updateStatus("Account created successfully!")
                
                // Navigate to appropriate home screen
                val intent = when (role.lowercase()) {
                    "calingapro" -> Intent(this, CaregiverHomeActivity::class.java)
                    else -> Intent(this, CareseekerHomeActivity::class.java)
                }
                
                Toast.makeText(this, "Welcome to Calinga!", Toast.LENGTH_SHORT).show()
                startActivity(intent)
                finishAffinity() // Clear the entire task stack
            }
            .addOnFailureListener { e ->
                showProgress(false)
                updateStatus("Failed to save user data")
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun showOtpSection() {
        tilOtp.visibility = View.VISIBLE
        verifyOtpButton.visibility = View.VISIBLE
        sendOtpButton.visibility = View.GONE
    }
    
    private fun showProgress(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        sendOtpButton.isEnabled = !show
        verifyOtpButton.isEnabled = !show
        resendOtpButton.isEnabled = !show
    }
    
    private fun updateStatus(message: String) {
        statusTextView.text = message
    }
    
    private fun startCountdown() {
        countDownTimer?.cancel()
        timerTextView.visibility = View.VISIBLE
        resendOtpButton.visibility = View.GONE
        
        countDownTimer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                timerTextView.text = "Resend OTP in ${seconds}s"
            }
            
            override fun onFinish() {
                timerTextView.visibility = View.GONE
                resendOtpButton.visibility = View.VISIBLE
            }
        }.start()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
    
    override fun onBackPressed() {
        super.onBackPressed()
        // Go back to register activity
        finish()
    }
}
