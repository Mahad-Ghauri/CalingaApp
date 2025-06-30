package com.myapp.calingaapp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
//import android.widget.Switch
import com.google.android.material.switchmaterial.SwitchMaterial
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import de.hdodenhof.circleimageview.CircleImageView
import java.util.UUID

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var locationManager: LocationManager
    
    private lateinit var profileImage: CircleImageView
    private lateinit var editName: TextInputEditText
    private lateinit var editAge: TextInputEditText
    private lateinit var editAddress: TextInputEditText
    private lateinit var editPhone: TextInputEditText
    private lateinit var editBio: TextInputEditText
    private lateinit var editEmergencyContact: TextInputEditText
    private lateinit var editMedicalConditions: TextInputEditText
    private lateinit var editRate: TextInputEditText // For CalingaPros only
    private lateinit var saveButton: Button
    private lateinit var progressBar: ProgressBar
    private var activeStatusSwitch: SwitchMaterial? = null // Optional switch for CalingaPro active status
    private var rateLayout: TextInputLayout? = null // Layout for rate field
    
    private var photoUri: Uri? = null
    private var profilePhotoUrl: String = ""
    private var userProfile = UserProfile()
    
    private val getContent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            if (data != null && data.data != null) {
                photoUri = data.data
                profileImage.setImageURI(photoUri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        
        // Initialize Firebase components
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        locationManager = LocationManager(this)
        
        // Set up toolbar
        val toolbar: Toolbar = findViewById(R.id.profile_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Edit Profile"
        
        // Initialize UI components
        profileImage = findViewById(R.id.profile_image)
        editName = findViewById(R.id.edit_name)
        editAge = findViewById(R.id.edit_age)
        editAddress = findViewById(R.id.edit_address)
        editPhone = findViewById(R.id.edit_phone)
        editBio = findViewById(R.id.edit_bio)
        editEmergencyContact = findViewById(R.id.edit_emergency_contact)
        editMedicalConditions = findViewById(R.id.edit_medical_conditions)
        editRate = findViewById(R.id.edit_rate)
        saveButton = findViewById(R.id.btn_save_profile)
        progressBar = findViewById(R.id.progress_bar)
        
        // Initialize CalingaPro-specific fields
        activeStatusSwitch = findViewById(R.id.switch_active_status)
        rateLayout = findViewById(R.id.rate_layout)
        
        // Hide CalingaPro-specific fields by default
        activeStatusSwitch?.visibility = View.GONE
        rateLayout?.visibility = View.GONE
        
        // Load user profile data
        loadUserProfile()
        
        // Set up image click listener
        profileImage.setOnClickListener {
            val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            getContent.launch(galleryIntent)
        }
        
        // Set up save button click listener
        saveButton.setOnClickListener {
            saveUserProfile()
        }
        
        // Set up active status switch listener for CalingaPros
        activeStatusSwitch?.setOnCheckedChangeListener { _, isChecked ->
            // Update location collection immediately when status changes
            locationManager.setUserActiveStatus(isChecked) { success, error ->
                if (!success && error != null) {
                    Toast.makeText(this, "Failed to update availability: $error", Toast.LENGTH_SHORT).show()
                    // Revert the switch state
                    activeStatusSwitch?.isChecked = !isChecked
                }
            }
        }
    }
    
    private fun loadUserProfile() {
        showProgress(true)
        
        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("users").document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        // Basic user data
                        val userData = document.data
                        
                        // Check if user has a profile document
                        db.collection("userProfiles").document(currentUser.uid)
                            .get()
                            .addOnSuccessListener { profileDoc ->
                                if (profileDoc != null && profileDoc.exists()) {
                                    userProfile = profileDoc.toObject(UserProfile::class.java) ?: UserProfile()
                                } else {
                                    // Create a basic profile from user data
                                    userProfile = UserProfile(
                                        userId = currentUser.uid,
                                        name = userData?.get("displayName") as? String ?: ""
                                    )
                                }
                                
                                // Fill UI with profile data
                                editName.setText(userProfile.name)
                                editAge.setText(if (userProfile.age != null && userProfile.age!! > 0) userProfile.age.toString() else "")
                                editAddress.setText(userProfile.address)
                                editBio.setText(userProfile.bio)
                                editRate.setText(if (userProfile.ratePerHour > 0) userProfile.ratePerHour.toString() else "")
                                // TODO: Phone, emergency contact, and medical conditions need to be added to new schema if needed
                                // editPhone.setText("")
                                // editEmergencyContact.setText("")
                                // editMedicalConditions.setText("")
                                
                                // Show active status toggle for CalingaPros (need to check role from users collection)
                                checkUserRoleAndShowToggle(currentUser.uid)
                                
                                // Load profile image if available
                                profilePhotoUrl = userProfile.profilePhotoUrl
                                if (profilePhotoUrl.isNotEmpty()) {
                                    // Load image using Glide or similar library
                                    // For simplicity, we're not implementing this here
                                }
                                
                                showProgress(false)
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Failed to load profile: ${e.message}", Toast.LENGTH_SHORT).show()
                                showProgress(false)
                            }
                    } else {
                        Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show()
                        showProgress(false)
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to load user data: ${e.message}", Toast.LENGTH_SHORT).show()
                    showProgress(false)
                }
        } else {
            Toast.makeText(this, "No user signed in", Toast.LENGTH_SHORT).show()
            showProgress(false)
            finish()
        }
    }
    
    private fun saveUserProfile() {
        val currentUser = auth.currentUser ?: return
        
        val name = editName.text.toString().trim()
        val ageText = editAge.text.toString().trim()
        val address = editAddress.text.toString().trim()
        val bio = editBio.text.toString().trim()
        val phone = editPhone.text.toString().trim()
        val emergencyContact = editEmergencyContact.text.toString().trim()
        val medicalConditions = editMedicalConditions.text.toString().trim()
        
        if (name.isEmpty()) {
            editName.error = "Name is required"
            return
        }
        
        val age = if (ageText.isNotEmpty()) ageText.toInt() else 0
        
        showProgress(true)
        
        // If a new photo was selected, upload it first
        if (photoUri != null) {
            val storageRef = storage.reference.child("profile_images/${currentUser.uid}/${UUID.randomUUID()}")
            storageRef.putFile(photoUri!!)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        profilePhotoUrl = uri.toString()
                        saveProfileData(currentUser.uid, name, age, address, bio, phone, emergencyContact, medicalConditions)
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to upload image: ${e.message}", Toast.LENGTH_SHORT).show()
                    showProgress(false)
                }
        } else {
            saveProfileData(currentUser.uid, name, age, address, bio, phone, emergencyContact, medicalConditions)
        }
    }
    
    private fun saveProfileData(
        userId: String, 
        name: String, 
        age: Int, 
        address: String, 
        bio: String,
        phone: String, 
        emergencyContact: String, 
        medicalConditions: String
    ) {
        // Get rate per hour if user is CalingaPro
        val rateText = editRate.text.toString().trim()
        val ratePerHour = if (rateText.isNotEmpty()) {
            try {
                rateText.toDouble()
            } catch (e: NumberFormatException) {
                0.0
            }
        } else {
            0.0
        }
        
        // Update the user profile object using the map approach since constructor parameters have changed
        val updatedProfile = mapOf(
            "userId" to userId,
            "name" to name,
            "age" to if (age > 0) age else null,
            "address" to address,
            "bio" to bio,
            "profilePhotoUrl" to profilePhotoUrl,
            "ratePerHour" to ratePerHour,
            "isActive" to (activeStatusSwitch?.isChecked ?: userProfile.isActive),
            "latitude" to userProfile.latitude,
            "longitude" to userProfile.longitude,
            "caregiverTier" to userProfile.caregiverTier,
            "specialties" to userProfile.specialties,
            "isApproved" to userProfile.isApproved,
            "documents" to userProfile.documents,
            "createdAt" to userProfile.createdAt
        )
        
                // Save to Firestore
        db.collection("userProfiles").document(userId)
            .set(updatedProfile)
            .addOnSuccessListener {
                // Also update display name in main users collection
                db.collection("users").document(userId)
                    .update(mapOf("fullName" to name))
                    .addOnSuccessListener {
                        // Update location collection with new active status for CalingaPros
                        val isActiveStatus = activeStatusSwitch?.isChecked ?: userProfile.isActive
                        locationManager.setUserActiveStatus(isActiveStatus) { success, error ->
                            if (success) {
                                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this, "Profile updated but failed to update availability status", Toast.LENGTH_SHORT).show()
                            }
                            setResult(Activity.RESULT_OK)
                            finish()
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Updated profile but failed to update display name", Toast.LENGTH_SHORT).show()
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to update profile: ${e.message}", Toast.LENGTH_SHORT).show()
                showProgress(false)
            }
    }
    
    private fun showProgress(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }
    
    private fun checkUserRoleAndShowToggle(userId: String) {
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { userDoc ->
                if (userDoc != null && userDoc.exists()) {
                    val userRole = userDoc.getString("role") ?: ""
                    
                    if (userRole == "calingapro") {
                        // Show CalingaPro specific fields
                        activeStatusSwitch?.visibility = View.VISIBLE
                        activeStatusSwitch?.isChecked = userProfile.isActive
                        activeStatusSwitch?.text = "Available for work"
                        
                        // Show rate per hour field for CalingaPros
                        rateLayout?.visibility = View.VISIBLE
                    } else {
                        // Hide CalingaPro specific fields for careseekers
                        activeStatusSwitch?.visibility = View.GONE
                        rateLayout?.visibility = View.GONE
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error checking user role: ${e.message}", Toast.LENGTH_SHORT).show()
                // Default to hiding CalingaPro specific fields
                activeStatusSwitch?.visibility = View.GONE
                rateLayout?.visibility = View.GONE
            }
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
