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
        
        // Auto-format phone number with +63 prefix
        editTextPhone.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val text = s.toString()
                if (!text.startsWith("+63") && text.isNotEmpty()) {
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
                navigateToPhoneAuth(fullName, email, phone, password, userType)
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
        
        if (!phone.startsWith("+63")) {
            editTextPhone.error = "Please use Philippine format (+63)"
            editTextPhone.requestFocus()
            return false
        }
        
        if (phone.length < 13) {
            editTextPhone.error = "Please enter a valid Philippine phone number"
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
}
