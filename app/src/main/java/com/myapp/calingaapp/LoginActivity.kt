package com.myapp.calingaapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {
    
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var editTextUsername: EditText
    private lateinit var editTextPassword: EditText
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        
        // Initialize Firebase components
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login_container)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        // Get references to UI components
        editTextUsername = findViewById(R.id.editTextUsername)
        editTextPassword = findViewById(R.id.editTextPassword)
        val buttonLogin = findViewById<Button>(R.id.buttonLogin)
        val textViewRegister = findViewById<TextView>(R.id.textViewRegister)
        
        // Set click listener for login button
        buttonLogin.setOnClickListener {
            val email = editTextUsername.text.toString().trim()
            val password = editTextPassword.text.toString().trim()
            
            if (validateInputs(email, password)) {
                loginUser(email, password)
            }
        }
        
        // Navigate to registration screen
        textViewRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }
    
    private fun validateInputs(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            editTextUsername.error = "Email is required"
            editTextUsername.requestFocus()
            return false
        }
        
        if (password.isEmpty()) {
            editTextPassword.error = "Password is required"
            editTextPassword.requestFocus()
            return false
        }
        
        return true
    }
    
    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Get current user
                    val user = auth.currentUser
                    
                    if (user != null) {
                        // Check if email is verified (optional)
                        if (user.isEmailVerified) {
                            // Determine user type from Firestore and navigate accordingly
                            checkUserTypeAndNavigate(user.uid)
                        } else {
                            // Even if email is not verified, we'll proceed
                            // In a production app, you might want to force verification
                            checkUserTypeAndNavigate(user.uid)
                        }
                    }
                } else {
                    // If sign in fails, display a message to the user
                    Toast.makeText(baseContext, "Authentication failed: ${task.exception?.message}",
                        Toast.LENGTH_SHORT).show()
                }
            }
    }
    
    private fun checkUserTypeAndNavigate(userId: String) {
        // Show a loading indicator if needed
        // loadingIndicator.visibility = View.VISIBLE
        
        // Query Firestore to determine user type
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                // Hide loading indicator
                // loadingIndicator.visibility = View.GONE
                
                if (document != null && document.exists()) {
                    val userType = document.getString("userType")?.lowercase() ?: ""
                    
                    when (userType) {
                        "careseeker" -> {
                            // Navigate to Careseeker Home
                            startActivity(Intent(this, CareseekerHomeActivity::class.java))
                        }
                        "calingapro" -> {
                            // Navigate to Caregiver Home
                            startActivity(Intent(this, CaregiverHomeActivity::class.java))
                        }
                        else -> {
                            // Unknown user type, default to Careseeker
                            Toast.makeText(this, "Unknown user type. Defaulting to Careseeker view.", 
                                Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, CareseekerHomeActivity::class.java))
                        }
                    }
                    finish()
                } else {
                    // No user document exists, default to Careseeker
                    Toast.makeText(this, "User profile not found. Please contact support.", 
                        Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, CareseekerHomeActivity::class.java))
                    finish()
                }
            }
            .addOnFailureListener { e ->
                // Hide loading indicator
                // loadingIndicator.visibility = View.GONE
                
                // Error occurred, log it and default to Careseeker
                Toast.makeText(this, "Error retrieving user type: ${e.message}", 
                    Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, CareseekerHomeActivity::class.java))
                finish()
            }
    }
    
    // Check if user is already signed in when activity starts
    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // User is signed in, navigate accordingly
            checkUserTypeAndNavigate(currentUser.uid)
        }
    }
}
