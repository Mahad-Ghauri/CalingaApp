package com.myapp.calingaapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login_container)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        // Get references to UI components
        val radioGroupUserType = findViewById<RadioGroup>(R.id.radioGroupUserType)
        val radioButtonCareseeker = findViewById<RadioButton>(R.id.radioButtonCareseeker)
        val buttonCreateAccount = findViewById<Button>(R.id.buttonCreateAccount)
        
        // Set click listener for create account button
        buttonCreateAccount.setOnClickListener {
            // Check which radio button is selected
            if (radioButtonCareseeker.isChecked) {
                // Careseeker is selected, navigate to CaresekerActivity
                val intent = Intent(this, CareseekerHomeActivity::class.java)
                startActivity(intent)
            } else {
                // CALINGApro is selected, navigate to CalingaProActivity
                val intent = Intent(this, CaregiverHomeActivity::class.java)
                startActivity(intent)
            }
        }
    }
}
