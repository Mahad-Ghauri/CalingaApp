package com.myapp.calingaapp

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {
    
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var editTextFullName: EditText
    private lateinit var editTextEmail: EditText
    private lateinit var editTextPhone: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var editTextRepeatPassword: EditText
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        
        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login_container)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        // Get references to UI components
        editTextFullName = findViewById(R.id.editTextFullName)
        editTextEmail = findViewById(R.id.editTextEmail)
        editTextPhone = findViewById(R.id.editTextPhone)
        editTextPassword = findViewById(R.id.editTextPassword)
        editTextRepeatPassword = findViewById(R.id.editTextRepeatPassword)
        val radioGroupUserType = findViewById<RadioGroup>(R.id.radioGroupUserType)
        val radioButtonCareseeker = findViewById<RadioButton>(R.id.radioButtonCareseeker)
        val buttonCreateAccount = findViewById<Button>(R.id.buttonCreateAccount)
        val textViewLogIn = findViewById<TextView>(R.id.textViewLogIn)
        
        // Auto-format phone number with +63 prefix (optional until OTP is implemented)
        editTextPhone.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val text = s.toString()
                // Optional: Auto-add +63 prefix for convenience
                if (!text.startsWith("+63") && text.isNotEmpty() && text.startsWith("9")) {
                    editTextPhone.setText("+63$text")
                    editTextPhone.setSelection(editTextPhone.text.length)
                }
            }
        })
        
        // Set click listener for create account button
        buttonCreateAccount.setOnClickListener {
            val fullName = editTextFullName.text.toString().trim()
            val email = editTextEmail.text.toString().trim()
            val phone = editTextPhone.text.toString().trim()
            val password = editTextPassword.text.toString().trim()
            val confirmPassword = editTextRepeatPassword.text.toString().trim()
            val userType = if (radioButtonCareseeker.isChecked) "careseeker" else "calingapro"
            
            if (validateInputs(fullName, email, phone, password, confirmPassword)) {
                // Skip OTP for now - directly create account
                registerUserDirectly(fullName, email, phone, password, userType)
                
                // TODO: When Firebase Phone Auth is approved, use this instead:
                // navigateToPhoneAuth(fullName, email, phone, password, userType)
            }
        }
        
        // Navigate to login screen
        textViewLogIn.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
    
    private fun validateInputs(fullName: String, email: String, phone: String, password: String, confirmPassword: String): Boolean {
        if (fullName.isEmpty()) {
            editTextFullName.error = "Full name is required"
            editTextFullName.requestFocus()
            return false
        }
        
        if (email.isEmpty()) {
            editTextEmail.error = "Email is required"
            editTextEmail.requestFocus()
            return false
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmail.error = "Please enter a valid email address"
            editTextEmail.requestFocus()
            return false
        }
        
        if (phone.isEmpty()) {
            editTextPhone.error = "Phone number is required"
            editTextPhone.requestFocus()
            return false
        }
        
        // Relax phone validation since we're not doing OTP verification yet
        if (phone.length < 10) {
            editTextPhone.error = "Please enter a valid phone number"
            editTextPhone.requestFocus()
            return false
        }
        
        if (password.isEmpty()) {
            editTextPassword.error = "Password is required"
            editTextPassword.requestFocus()
            return false
        }
        
        if (password.length < 6) {
            editTextPassword.error = "Password should be at least 6 characters"
            editTextPassword.requestFocus()
            return false
        }
        
        if (confirmPassword.isEmpty()) {
            editTextRepeatPassword.error = "Please confirm your password"
            editTextRepeatPassword.requestFocus()
            return false
        }
        
        if (password != confirmPassword) {
            editTextRepeatPassword.error = "Passwords do not match"
            editTextRepeatPassword.requestFocus()
            return false
        }
        
        return true
    }
    
    private fun navigateToPhoneAuth(fullName: String, email: String, phone: String, password: String, userType: String) {
        val intent = Intent(this, PhoneAuthActivity::class.java).apply {
            putExtra("fullName", fullName)
            putExtra("email", email)
            putExtra("phone", phone)
            putExtra("password", password)
            putExtra("role", userType)
        }
        startActivity(intent)
    }
    
    private fun registerUserDirectly(fullName: String, email: String, phone: String, password: String, userType: String) {
        // Show progress
        // You can add a progress bar here if needed
        
        // Create user with email and password
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val currentUser = auth.currentUser
                    if (currentUser != null) {
                        createUserDocumentsDirectly(currentUser.uid, fullName, email, phone, userType)
                    } else {
                        Toast.makeText(this, "User creation failed", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }
    
    private fun createUserDocumentsDirectly(userId: String, fullName: String, email: String, phone: String, userType: String) {
        // Create user document
        val user = User(
            uid = userId,
            fullName = fullName,
            email = email,
            mobileNumber = phone,
            role = userType
        )
        
        // Create userProfile document
        val userProfile = UserProfile(
            userId = userId,
            name = fullName,
            isApproved = if (userType == "calingapro") false else true, // CalingaPros need approval
            isActive = false
        )
        
        // Save to Firestore
        db.collection("users").document(userId)
            .set(user.toMap())
            .addOnSuccessListener {
                db.collection("userProfiles").document(userId)
                    .set(userProfile.toMap())
                    .addOnSuccessListener {
                        if (userType == "calingapro") {
                            Toast.makeText(this, "Registration successful! Your account will be reviewed and approved.", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()
                        }
                        
                        // Navigate to appropriate home screen
                        navigateToHome(userType)
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to create profile: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to create user: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
    
    private fun navigateToHome(userType: String) {
        val intent = when (userType) {
            "careseeker" -> Intent(this, CareseekerHomeActivity::class.java)
            "calingapro" -> Intent(this, CaregiverHomeActivity::class.java)
            else -> Intent(this, LoginActivity::class.java)
        }
        
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    // TODO: Re-enable this when Firebase Phone Auth is approved
    /*
    private fun navigateToPhoneAuth(fullName: String, email: String, phone: String, password: String, userType: String) {
        val intent = Intent(this, PhoneAuthActivity::class.java).apply {
            putExtra("fullName", fullName)
            putExtra("email", email)
            putExtra("phone", phone)
            putExtra("password", password)
            putExtra("role", userType)
        }
        startActivity(intent)
    }
    */
}
