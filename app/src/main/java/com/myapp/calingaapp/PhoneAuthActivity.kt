package com.myapp.calingaapp

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit

class PhoneAuthActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    
    private lateinit var phoneNumberEditText: EditText
    private lateinit var otpEditText: EditText
    private lateinit var sendOtpButton: Button
    private lateinit var verifyOtpButton: Button
    private lateinit var resendOtpButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var timerTextView: TextView
    private lateinit var statusTextView: TextView
    
    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var countDownTimer: CountDownTimer? = null
    
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
        
        // Get data from RegisterActivity
        fullName = intent.getStringExtra("fullName") ?: ""
        email = intent.getStringExtra("email") ?: ""
        phone = intent.getStringExtra("phone") ?: ""
        password = intent.getStringExtra("password") ?: ""
        role = intent.getStringExtra("role") ?: ""
        
        initViews()
        setupListeners()
        
        // Pre-fill phone number and automatically send OTP
        if (phone.isNotEmpty()) {
            phoneNumberEditText.setText(phone)
            phoneNumberEditText.isEnabled = false // Disable editing since it's already validated
            sendOtp(phone)
        }
    }
    
    private fun initViews() {
        phoneNumberEditText = findViewById(R.id.et_phone_number)
        otpEditText = findViewById(R.id.et_otp)
        sendOtpButton = findViewById(R.id.btn_send_otp)
        verifyOtpButton = findViewById(R.id.btn_verify_otp)
        resendOtpButton = findViewById(R.id.btn_resend_otp)
        progressBar = findViewById(R.id.progress_bar)
        timerTextView = findViewById(R.id.tv_timer)
        statusTextView = findViewById(R.id.tv_status)
        
        // Initially hide OTP section
        otpEditText.visibility = View.GONE
        verifyOtpButton.visibility = View.GONE
        resendOtpButton.visibility = View.GONE
        timerTextView.visibility = View.GONE
    }
    
    private fun setupListeners() {
        sendOtpButton.setOnClickListener {
            val phoneNumber = phoneNumberEditText.text.toString().trim()
            if (validatePhoneNumber(phoneNumber)) {
                sendOtp(phoneNumber)
            }
        }
        
        verifyOtpButton.setOnClickListener {
            val otp = otpEditText.text.toString().trim()
            if (validateOtp(otp)) {
                verifyOtp(otp)
            }
        }
        
        resendOtpButton.setOnClickListener {
            val phoneNumber = phoneNumberEditText.text.toString().trim()
            resendOtp(phoneNumber)
        }
        
        // Auto-format phone number
        phoneNumberEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val text = s.toString()
                if (!text.startsWith("+63") && text.isNotEmpty()) {
                    phoneNumberEditText.setText("+63$text")
                    phoneNumberEditText.setSelection(phoneNumberEditText.text.length)
                }
            }
        })
    }
    
    private fun validatePhoneNumber(phoneNumber: String): Boolean {
        return when {
            phoneNumber.isEmpty() -> {
                phoneNumberEditText.error = "Phone number is required"
                false
            }
            !phoneNumber.startsWith("+63") -> {
                phoneNumberEditText.error = "Please use Philippine format (+63)"
                false
            }
            phoneNumber.length < 13 -> {
                phoneNumberEditText.error = "Please enter a valid Philippine phone number"
                false
            }
            else -> true
        }
    }
    
    private fun validateOtp(otp: String): Boolean {
        return when {
            otp.isEmpty() -> {
                otpEditText.error = "OTP is required"
                false
            }
            otp.length != 6 -> {
                otpEditText.error = "OTP must be 6 digits"
                false
            }
            else -> true
        }
    }
    
    private fun sendOtp(phoneNumber: String) {
        showProgress(true)
        statusTextView.text = "Sending OTP..."
        
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    Log.d("PhoneAuth", "onVerificationCompleted: $credential")
                    signInWithPhoneAuthCredential(credential)
                }
                
                override fun onVerificationFailed(e: FirebaseException) {
                    Log.w("PhoneAuth", "onVerificationFailed", e)
                    showProgress(false)
                    statusTextView.text = "Verification failed: ${e.message}"
                    Toast.makeText(this@PhoneAuthActivity, "Verification failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
                
                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    Log.d("PhoneAuth", "onCodeSent: $verificationId")
                    this@PhoneAuthActivity.verificationId = verificationId
                    this@PhoneAuthActivity.resendToken = token
                    
                    showProgress(false)
                    showOtpSection()
                    startTimer()
                    statusTextView.text = "OTP sent to $phoneNumber"
                    Toast.makeText(this@PhoneAuthActivity, "OTP sent!", Toast.LENGTH_SHORT).show()
                }
            })
            .build()
        
        PhoneAuthProvider.verifyPhoneNumber(options)
    }
    
    private fun resendOtp(phoneNumber: String) {
        if (resendToken == null) {
            Toast.makeText(this, "Cannot resend OTP at this time", Toast.LENGTH_SHORT).show()
            return
        }
        
        showProgress(true)
        statusTextView.text = "Resending OTP..."
        
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setForceResendingToken(resendToken!!)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    signInWithPhoneAuthCredential(credential)
                }
                
                override fun onVerificationFailed(e: FirebaseException) {
                    showProgress(false)
                    statusTextView.text = "Resend failed: ${e.message}"
                    Toast.makeText(this@PhoneAuthActivity, "Resend failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
                
                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    this@PhoneAuthActivity.verificationId = verificationId
                    this@PhoneAuthActivity.resendToken = token
                    
                    showProgress(false)
                    startTimer()
                    statusTextView.text = "OTP resent to $phoneNumber"
                    Toast.makeText(this@PhoneAuthActivity, "OTP resent!", Toast.LENGTH_SHORT).show()
                }
            })
            .build()
        
        PhoneAuthProvider.verifyPhoneNumber(options)
    }
    
    private fun verifyOtp(otp: String) {
        if (verificationId == null) {
            Toast.makeText(this, "Please request OTP first", Toast.LENGTH_SHORT).show()
            return
        }
        
        showProgress(true)
        statusTextView.text = "Verifying OTP..."
        
        val credential = PhoneAuthProvider.getCredential(verificationId!!, otp)
        signInWithPhoneAuthCredential(credential)
    }
    
    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        // First create user with email and password
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Link phone credential to email account
                    val user = auth.currentUser
                    user?.linkWithCredential(credential)
                        ?.addOnCompleteListener { linkTask ->
                            if (linkTask.isSuccessful) {
                                createUserDocuments()
                            } else {
                                Log.w("PhoneAuth", "linkWithCredential:failure", linkTask.exception)
                                // If linking fails, still create documents (email auth was successful)
                                createUserDocuments()
                            }
                        }
                } else {
                    Log.w("PhoneAuth", "createUserWithEmailAndPassword:failure", task.exception)
                    showProgress(false)
                    statusTextView.text = "Registration failed: ${task.exception?.message}"
                    Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }
    
    private fun createUserDocuments() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            showProgress(false)
            Toast.makeText(this, "User authentication failed", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Use the phone number from the intent (already validated)
        val phoneNumber = phone
        
        // Create user document
        val user = User(
            uid = currentUser.uid,
            fullName = fullName,
            email = email,
            mobileNumber = phoneNumber,
            role = role
        )
        
        // Create userProfile document
        val userProfile = UserProfile(
            userId = currentUser.uid,
            name = fullName,
            isApproved = if (role == "calingapro") false else true, // CalingaPros need approval
            isActive = false
        )
        
        // Save to Firestore
        db.collection("users").document(currentUser.uid)
            .set(user.toMap())
            .addOnSuccessListener {
                db.collection("userProfiles").document(currentUser.uid)
                    .set(userProfile.toMap())
                    .addOnSuccessListener {
                        showProgress(false)
                        statusTextView.text = "Registration successful!"
                        
                        if (role == "calingapro") {
                            Toast.makeText(this, "Registration successful! Your account will be reviewed and approved.", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()
                        }
                        
                        // Navigate to appropriate home screen
                        navigateToHome()
                    }
                    .addOnFailureListener { e ->
                        showProgress(false)
                        Toast.makeText(this, "Failed to create profile: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                showProgress(false)
                Toast.makeText(this, "Failed to create user: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
    
    private fun navigateToHome() {
        val intent = when (role) {
            "careseeker" -> Intent(this, CareseekerHomeActivity::class.java)
            "calingapro" -> Intent(this, CaregiverHomeActivity::class.java)
            else -> Intent(this, LoginActivity::class.java)
        }
        
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    private fun showOtpSection() {
        phoneNumberEditText.isEnabled = false
        sendOtpButton.visibility = View.GONE
        otpEditText.visibility = View.VISIBLE
        verifyOtpButton.visibility = View.VISIBLE
        resendOtpButton.visibility = View.VISIBLE
        timerTextView.visibility = View.VISIBLE
    }
    
    private fun startTimer() {
        resendOtpButton.isEnabled = false
        countDownTimer?.cancel()
        
        countDownTimer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timerTextView.text = "Resend OTP in ${millisUntilFinished / 1000}s"
            }
            
            override fun onFinish() {
                timerTextView.text = "You can resend OTP now"
                resendOtpButton.isEnabled = true
            }
        }.start()
    }
    
    private fun showProgress(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        sendOtpButton.isEnabled = !show
        verifyOtpButton.isEnabled = !show
    }
    
    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}
