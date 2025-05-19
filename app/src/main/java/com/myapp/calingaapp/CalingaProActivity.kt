package com.myapp.calingaapp

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class CalingaProActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calinga_pro)
        
        // Extract caregiver details from intent
        val name = intent.getStringExtra(CalingaProAdapter.EXTRA_CAREGIVER_NAME) ?: "Unknown Caregiver"
        val tier = intent.getStringExtra(CalingaProAdapter.EXTRA_CAREGIVER_TIER) ?: ""
        val address = intent.getStringExtra(CalingaProAdapter.EXTRA_CAREGIVER_ADDRESS) ?: ""
        val rate = intent.getIntExtra(CalingaProAdapter.EXTRA_CAREGIVER_RATE, 0)
        val photoResId = intent.getIntExtra(CalingaProAdapter.EXTRA_CAREGIVER_PHOTO_RES_ID, R.drawable.ic_person_placeholder)
        val experience = intent.getStringExtra(CalingaProAdapter.EXTRA_CAREGIVER_EXPERIENCE) ?: "N/A"
        val patients = intent.getIntExtra(CalingaProAdapter.EXTRA_CAREGIVER_PATIENTS, 0)
        val bloodType = intent.getStringExtra(CalingaProAdapter.EXTRA_CAREGIVER_BLOOD_TYPE) ?: "N/A"
        val height = intent.getStringExtra(CalingaProAdapter.EXTRA_CAREGIVER_HEIGHT) ?: "N/A"
        val about = intent.getStringExtra(CalingaProAdapter.EXTRA_CAREGIVER_ABOUT) ?: "No information available."
        val email = intent.getStringExtra(CalingaProAdapter.EXTRA_CAREGIVER_EMAIL) ?: ""
        val phone = intent.getStringExtra(CalingaProAdapter.EXTRA_CAREGIVER_PHONE) ?: ""
        
        // Set the views with the caregiver data
        findViewById<ImageView>(R.id.imageViewPhoto).setImageResource(photoResId)
        findViewById<TextView>(R.id.textViewName).text = name
        findViewById<TextView>(R.id.textViewTitle).text = tier
        findViewById<TextView>(R.id.textViewEmail).text = email
        findViewById<TextView>(R.id.textViewPhone).text = phone
        findViewById<TextView>(R.id.textViewExperience).text = experience
        findViewById<TextView>(R.id.textViewPatients).text = patients.toString()
        findViewById<TextView>(R.id.textViewBloodType).text = bloodType
        findViewById<TextView>(R.id.textViewHeight).text = height
        findViewById<TextView>(R.id.textViewAbout).text = about
        
        // Set up the accept patient button
        findViewById<Button>(R.id.buttonAcceptCare).setOnClickListener {
            Toast.makeText(this, "Request sent to $name", Toast.LENGTH_SHORT).show()
        }
    }
}
