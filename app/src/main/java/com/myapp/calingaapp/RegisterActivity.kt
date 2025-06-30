package com.myapp.calingaapp

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
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
    private lateinit var spinnerCountryCode: Spinner
    private lateinit var checkBoxTerms: CheckBox
    
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
        spinnerCountryCode = findViewById(R.id.spinnerCountryCode)
        checkBoxTerms = findViewById(R.id.checkBoxTerms)
        val radioGroupUserType = findViewById<RadioGroup>(R.id.radioGroupUserType)
        val radioButtonCareseeker = findViewById<RadioButton>(R.id.radioButtonCareseeker)
        val buttonCreateAccount = findViewById<Button>(R.id.buttonCreateAccount)
        val textViewLogIn = findViewById<TextView>(R.id.textViewLogIn)
        
        // Setup country code spinner
        setupCountryCodeSpinner()
        
        // Set click listener for create account button
        buttonCreateAccount.setOnClickListener {
            val fullName = editTextFullName.text.toString().trim()
            val email = editTextEmail.text.toString().trim()
            val selectedCountryCode = spinnerCountryCode.selectedItem.toString()
            val phoneNumber = editTextPhone.text.toString().trim()
            val completePhone = selectedCountryCode + phoneNumber
            val password = editTextPassword.text.toString().trim()
            val confirmPassword = editTextRepeatPassword.text.toString().trim()
            val userType = if (radioButtonCareseeker.isChecked) "careseeker" else "calingapro"
            
            if (validateInputs(fullName, email, completePhone, password, confirmPassword)) {
                // Navigate to Phone Auth for OTP verification
                navigateToPhoneAuth(fullName, email, completePhone, password, userType)
            }
        }
        
        // Navigate to login screen
        textViewLogIn.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
    
    private fun setupCountryCodeSpinner() {
        val countryCodes = resources.getStringArray(R.array.country_codes)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, countryCodes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCountryCode.adapter = adapter
        // Default to +63 (Philippines)
        spinnerCountryCode.setSelection(1)
    }
    
    private fun navigateToPhoneAuth(fullName: String, email: String, phone: String, password: String, userType: String) {
        val intent = Intent(this, PhoneAuthActivity::class.java).apply {
            putExtra("fullName", fullName)
            putExtra("email", email)
            putExtra("phone", phone)
            putExtra("password", password)
            putExtra("userType", userType)
        }
        startActivity(intent)
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
        
        if (phone.length < 12) { // +63 or +1 + 10 digits minimum
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
            editTextPassword.error = "Password must be at least 6 characters"
            editTextPassword.requestFocus()
            return false
        }
        
        if (password != confirmPassword) {
            editTextRepeatPassword.error = "Passwords do not match"
            editTextRepeatPassword.requestFocus()
            return false
        }
        
        if (!checkBoxTerms.isChecked) {
            Toast.makeText(this, "Please accept the Terms and Conditions and Privacy Policy", Toast.LENGTH_SHORT).show()
            return false
        }
        
        return true
    }
}
