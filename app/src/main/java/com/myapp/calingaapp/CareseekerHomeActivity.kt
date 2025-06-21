package com.myapp.calingaapp

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import de.hdodenhof.circleimageview.CircleImageView

class CareseekerHomeActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var calingaProAdapter: CalingaProAdapter
    private lateinit var editTextSearch: EditText
    private val calingaProList = ArrayList<CalingaPro>()
    private val filteredList = ArrayList<CalingaPro>()
    
    private lateinit var navHeaderName: TextView
    private lateinit var navHeaderEmail: TextView
    private lateinit var navHeaderImage: CircleImageView
    
    private var userProfile: UserProfile? = null
    
    private val profileActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // Refresh user profile data
            loadUserProfile()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_careseeker_home)
        
        // Initialize Firebase components
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        
        // Initialize UI components
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        
        // Set up toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = ""
        // Hide default home button that ActionBarDrawerToggle would use
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        supportActionBar?.setHomeButtonEnabled(false)
        
        // Set up navigation view
        navigationView.setNavigationItemSelectedListener(this)
        
        // Set up navigation header views
        val headerView = navigationView.getHeaderView(0)
        navHeaderName = headerView.findViewById(R.id.nav_header_name)
        navHeaderEmail = headerView.findViewById(R.id.nav_header_email)
        navHeaderImage = headerView.findViewById(R.id.nav_header_image)
        
        // Use the custom menu icon to control the drawer
        findViewById<ImageView>(R.id.imageViewMenu).setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }
        
        // Initialize search bar
        editTextSearch = findViewById(R.id.editTextSearch)
        editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterProfessionals(s.toString())
            }
        })

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.recyclerViewCaregivers)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Add sample data
        populateCalingaProList()
        filteredList.addAll(calingaProList)

        // Set up adapter
        calingaProAdapter = CalingaProAdapter(filteredList)
        recyclerView.adapter = calingaProAdapter

        // Set up click listeners
        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener {
            // Handle filter click
        }
        
        // Load user profile data
        loadUserProfile()
    }
    
    private fun loadUserProfile() {
        val currentUser = auth.currentUser
        
        if (currentUser != null) {
            // First, update the navigation header with basic user info
            navHeaderEmail.text = currentUser.email ?: ""
            
            // Now load the full profile data
            db.collection("userProfiles").document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        userProfile = document.toObject(UserProfile::class.java)
                        updateUIWithProfile()
                    } else {
                        // If no detailed profile exists, try to get basic info from users collection
                        db.collection("users").document(currentUser.uid)
                            .get()
                            .addOnSuccessListener { userDoc ->
                                if (userDoc != null && userDoc.exists()) {
                                    val displayName = userDoc.getString("displayName") ?: "User"
                                    navHeaderName.text = displayName
                                    
                                    // Initialize empty profile
                                    userProfile = UserProfile(
                                        userId = currentUser.uid,
                                        name = displayName,
                                        email = currentUser.email ?: "",
                                        userType = userDoc.getString("userType") ?: ""
                                    )
                                    
                                    // Show placeholder data in patient card
                                    findViewById<TextView>(R.id.textViewPatientName).text = displayName
                                    findViewById<TextView>(R.id.textViewPatientAge).text = "Age not set"
                                    findViewById<TextView>(R.id.textViewPatientAddress).text = "Address not set"
                                }
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error loading profile: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
    
    private fun updateUIWithProfile() {
        userProfile?.let { profile ->
            // Update navigation drawer header
            navHeaderName.text = profile.name
            navHeaderEmail.text = profile.email
            
            // Update patient info card
            findViewById<TextView>(R.id.textViewPatientName).text = profile.name
            findViewById<TextView>(R.id.textViewPatientAge).text = "${profile.age} years old"
            findViewById<TextView>(R.id.textViewPatientAddress).text = profile.address
            
            // Load profile image if available (you could use Glide or Picasso here)
            if (profile.photoUrl.isNotEmpty()) {
                // This would require a library like Glide or Picasso
                // Glide.with(this).load(profile.photoUrl).into(navHeaderImage)
                // Glide.with(this).load(profile.photoUrl).into(findViewById(R.id.imageViewPatient))
            }
        }
    }
    
    private fun filterProfessionals(query: String) {
        filteredList.clear()
        
        if (query.isEmpty()) {
            filteredList.addAll(calingaProList)
        } else {
            val lowerCaseQuery = query.lowercase()
            for (pro in calingaProList) {
                if (pro.name.lowercase().contains(lowerCaseQuery) || 
                    pro.tier.lowercase().contains(lowerCaseQuery) || 
                    pro.address.lowercase().contains(lowerCaseQuery)) {
                    filteredList.add(pro)
                }
            }
        }
        
        calingaProAdapter.notifyDataSetChanged()
    }

    private fun populateCalingaProList() {
        // Add the sample data with photos and additional information
        calingaProList.add(
            CalingaPro(
                name = "Emma Brown",
                tier = "Basic",
                address = "100 Market Street, San Francisco, CA 94103",
                rate = 20,
                photoResId = R.drawable.emma_brown,  // Use your actual drawable resource
                experience = "3yrs",
                patients = 125,
                bloodType = "A+",
                height = "165cm",
                about = "Emma Brown is a dedicated caregiver with 3 years of experience caring for elderly patients. She specializes in companionship, medication management, and light housekeeping.",
                email = "emma.brown@example.com",
                phone = "+1 123-456-7890"
            )
        )
        
        calingaProList.add(
            CalingaPro(
                name = "John Reyes",
                tier = "CNA",
                address = "456 Palm Street, San Diego, CA 92101",
                rate = 22,
                photoResId = R.drawable.doctor,  // Use your actual drawable resource
                experience = "5yrs",
                patients = 210,
                bloodType = "B+",
                height = "178cm",
                about = "John Reyes is a Certified Nursing Assistant with extensive experience in post-operative care, mobility assistance, and vital sign monitoring.",
                email = "john.reyes@example.com",
                phone = "+1 234-567-8901"
            )
        )
        
        // Add remaining caregivers with similar detailed information
        calingaProList.add(
            CalingaPro(
                name = "Angela Cruz",
                tier = "LVN",
                address = "789 Ocean Avenue, Long Beach, CA 90802",
                rate = 28,
                photoResId = R.drawable.angela_cruz,
                experience = "7yrs",
                patients = 350,
                bloodType = "O-",
                height = "162cm",
                about = "Angela Cruz is a Licensed Vocational Nurse specializing in diabetes management, wound care, and patient education.",
                email = "angela.cruz@example.com",
                phone = "+1 345-678-9012"
            )
        )
        
        // Continue with remaining caregivers...
        calingaProList.add(
            CalingaPro(
                name = "David Molina",
                tier = "RN",
                address = "321 Sunset Drive, Sacramento, CA 95814",
                rate = 35,
                photoResId = R.drawable.david_molina,
                experience = "10yrs",
                patients = 450,
                bloodType = "AB+",
                height = "180cm",
                about = "David Molina is a Registered Nurse with a background in critical care and home health nursing. He excels in complex medical care management.",
                email = "david.molina@example.com",
                phone = "+1 456-789-0123"
            )
        )
        
        calingaProList.add(
            CalingaPro(
                name = "Kristine Gomez",
                tier = "NP",
                address = "654 Mission Street, San Francisco, CA 94105",
                rate = 45,
                photoResId = R.drawable.kistine_gomez,
                experience = "12yrs",
                patients = 520,
                bloodType = "A-",
                height = "168cm",
                about = "Kristine Gomez is a Nurse Practitioner who provides comprehensive healthcare services including health assessments, medication management, and treatment plans.",
                email = "kristine.gomez@example.com",
                phone = "+1 567-890-1234"
            )
        )
    }
      override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> {
                // We're already in the home screen, just close drawer
                drawerLayout.closeDrawer(GravityCompat.START)
            }
            R.id.nav_map -> {
                // Launch map activity for careseeker
                val intent = Intent(this, MapActivity::class.java)
                intent.putExtra("USER_ROLE", "careseeker")
                startActivity(intent)
            }
            R.id.nav_profile -> {
                // Launch profile activity
                val intent = Intent(this, ProfileActivity::class.java)
                profileActivityLauncher.launch(intent)
            }
            R.id.nav_logout -> {
                // Log out user
                auth.signOut()
                Toast.makeText(this, "Signed out successfully", Toast.LENGTH_SHORT).show()
                
                // Go to login screen
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }
        
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }
    
    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}

// Data class for CALINGApro items
data class CalingaPro(
    val name: String,
    val tier: String,
    val address: String,
    val rate: Int,
    val photoResId: Int = R.drawable.ic_person_placeholder, // Default placeholder
    val experience: String = "1yr",
    val patients: Int = 0,
    val bloodType: String = "O+",
    val height: String = "170cm",
    val about: String = "No information available.",
    val email: String = "",
    val phone: String = ""
)
