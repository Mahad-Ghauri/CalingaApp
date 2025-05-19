package com.myapp.calingaapp

import android.content.Intent
import android.os.Bundle
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
    private lateinit var editTextUsername: EditText
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
        editTextUsername = findViewById(R.id.editTextUsername)
        editTextPassword = findViewById(R.id.editTextPassword)
        editTextRepeatPassword = findViewById(R.id.editTextRepeatPassword)
        val radioGroupUserType = findViewById<RadioGroup>(R.id.radioGroupUserType)
        val radioButtonCareseeker = findViewById<RadioButton>(R.id.radioButtonCareseeker)
        val buttonCreateAccount = findViewById<Button>(R.id.buttonCreateAccount)
        val textViewLogIn = findViewById<TextView>(R.id.textViewLogIn)
        
        // Set click listener for create account button
        buttonCreateAccount.setOnClickListener {
            val email = editTextUsername.text.toString().trim()
            val password = editTextPassword.text.toString().trim()
            val confirmPassword = editTextRepeatPassword.text.toString().trim()
            val userType = if (radioButtonCareseeker.isChecked) "careseeker" else "calingapro"
            
            if (validateInputs(email, password, confirmPassword)) {
                registerUser(email, password, userType)
            }
        }
        
        // Navigate to login screen
        textViewLogIn.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
    
    private fun validateInputs(email: String, password: String, confirmPassword: String): Boolean {
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
    
    private fun registerUser(email: String, password: String, userType: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // User created successfully, now store additional info in Firestore
                    val user = auth.currentUser
                    if (user != null) {
                        // Create user document in Firestore
                        val userMap = hashMapOf(
                            "email" to email,
                            "userType" to userType,
                            "createdAt" to System.currentTimeMillis()
                        )
                        
                        db.collection("users").document(user.uid)
                            .set(userMap)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()
                                
                                // Navigate based on user type
                                if (userType == "careseeker") {
                                    startActivity(Intent(this, CareseekerHomeActivity::class.java))
                                } else {
                                    startActivity(Intent(this, CaregiverHomeActivity::class.java))
                                }
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Failed to save user data: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    // If registration fails, display error message
                    Toast.makeText(baseContext, "Registration failed: ${task.exception?.message}",
                        Toast.LENGTH_SHORT).show()
                }
            }
    }
}
