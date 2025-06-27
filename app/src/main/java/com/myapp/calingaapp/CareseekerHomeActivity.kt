package com.myapp.calingaapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
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
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import de.hdodenhof.circleimageview.CircleImageView
import kotlin.math.*

class CareseekerHomeActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationManager: LocationManager
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var calingaProAdapter: CalingaProAdapter
    private lateinit var editTextSearch: EditText
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private val calingaProList = ArrayList<CalingaPro>()
    private val filteredList = ArrayList<CalingaPro>()
    
    private lateinit var navHeaderName: TextView
    private lateinit var navHeaderEmail: TextView
    private lateinit var navHeaderImage: CircleImageView
    
    private var userProfile: UserProfile? = null
    private var currentLatitude: Double = 0.0
    private var currentLongitude: Double = 0.0
    
    private val profileActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // Refresh user profile data
            loadUserProfile()
        }
    }

    companion object {
        private const val GEOFENCE_RADIUS_MILES = 10.0
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_careseeker_home)
        
        // Initialize Firebase components
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationManager = LocationManager(this)
        
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
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)

        // Set up adapter
        calingaProAdapter = CalingaProAdapter(filteredList)
        recyclerView.adapter = calingaProAdapter

        // Set up swipe refresh
        swipeRefreshLayout.setOnRefreshListener {
            getCurrentLocationAndLoadCalingaPros()
        }
        
        // Set up swipe refresh colors
        swipeRefreshLayout.setColorSchemeResources(
            R.color.primary_color,
            R.color.secondary_color,
            R.color.accent_color
        )

        // Set up click listeners
        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener {
            // Handle filter click
        }
        
        // Load user profile data and get location
        loadUserProfile()
        getCurrentLocationAndLoadCalingaPros()
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
                                        name = displayName
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
            // Email will need to be fetched from users collection since it's not in userProfiles
            
            // Update patient info card
            findViewById<TextView>(R.id.textViewPatientName).text = profile.name
            findViewById<TextView>(R.id.textViewPatientAge).text = if (profile.age != null) "${profile.age} years old" else "Age not set"
            findViewById<TextView>(R.id.textViewPatientAddress).text = profile.address
            
            // Load profile image if available (you could use Glide or Picasso here)
            if (profile.profilePhotoUrl.isNotEmpty()) {
                // This would require a library like Glide or Picasso
                // Glide.with(this).load(profile.profilePhotoUrl).into(navHeaderImage)
                // Glide.with(this).load(profile.profilePhotoUrl).into(findViewById(R.id.imageViewPatient))
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

    private fun getCurrentLocationAndLoadCalingaPros() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request location permissions
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
            // Load CalingaPros without location filtering
            loadActiveCalingaPros()
            return
        }
        
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    currentLatitude = location.latitude
                    currentLongitude = location.longitude
                    
                    // Update user's location in the locations collection
                    locationManager.updateUserLocation(currentLatitude, currentLongitude) { success, error ->
                        if (!success && error != null) {
                            // Log error but don't stop the flow
                            android.util.Log.e("LocationUpdate", "Failed to update location: $error")
                        }
                    }
                }
                loadActiveCalingaPros()
            }
            .addOnFailureListener {
                // If location fails, still load CalingaPros without filtering
                Toast.makeText(this, "Unable to get location. Showing all available CalingaPros.", Toast.LENGTH_LONG).show()
                loadActiveCalingaPros()
            }
    }
    
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, try to get location again
                    getCurrentLocationAndLoadCalingaPros()
                } else {
                    // Permission denied, show message and load all CalingaPros
                    Toast.makeText(this, "Location permission denied. Showing all available CalingaPros.", Toast.LENGTH_LONG).show()
                    loadActiveCalingaPros()
                }
            }
        }
    }
    
    private fun loadActiveCalingaPros() {
        swipeRefreshLayout.isRefreshing = true
        // Use LocationManager to get nearby CalingaPros
        locationManager.getNearbyCalingaPros(
            currentLatitude, 
            currentLongitude, 
            GEOFENCE_RADIUS_MILES
        ) { nearbyLocations, error ->
            
            swipeRefreshLayout.isRefreshing = false
            
            if (error != null) {
                Toast.makeText(this, "Error loading nearby CalingaPros: $error", Toast.LENGTH_SHORT).show()
                return@getNearbyCalingaPros
            }
            
            if (nearbyLocations.isEmpty()) {
                calingaProList.clear()
                filteredList.clear()
                calingaProAdapter.notifyDataSetChanged()
                
                val message = if (currentLatitude != 0.0 && currentLongitude != 0.0) {
                    "No CalingaPros found within 10 miles of your location"
                } else {
                    "No CalingaPros available. Please enable location services to find nearby professionals."
                }
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                return@getNearbyCalingaPros
            }
            
            // Get the user IDs from the location data
            val calingaproUserIds = nearbyLocations.map { it.uid }
            
            // Now get userProfiles for these users that are approved and active
            db.collection("userProfiles")
                .whereIn("userId", calingaproUserIds)
                .whereEqualTo("isApproved", true)
                .whereEqualTo("isActive", true)
                .get()
                .addOnSuccessListener { documents ->
                    calingaProList.clear()
                    val tempList = ArrayList<CalingaPro>()
                    
                    for (document in documents) {
                        val userProfile = document.toObject(UserProfile::class.java)
                        
                        // Find the corresponding location data
                        val locationData = nearbyLocations.find { it.uid == userProfile.userId }
                        
                        // Calculate distance
                        var distanceInMiles = 0.0
                        if (currentLatitude != 0.0 && currentLongitude != 0.0 && locationData != null &&
                            locationData.location.latitude != 0.0 && locationData.location.longitude != 0.0) {
                            distanceInMiles = calculateDistanceInMiles(
                                currentLatitude, currentLongitude,
                                locationData.location.latitude, locationData.location.longitude
                            )
                        }
                        
                        val calingaPro = CalingaPro(
                            name = userProfile.name,
                            tier = userProfile.caregiverTier,
                            address = userProfile.address,
                            rate = 25, // Default rate, you can add this field to UserProfile later
                            photoResId = R.drawable.ic_person_placeholder,
                            experience = "1yr", // You can add this field to UserProfile later
                            patients = 0, // You can add this field to UserProfile later
                            bloodType = "O+", // You can add this field to UserProfile later
                            height = "170cm", // You can add this field to UserProfile later
                            about = userProfile.bio,
                            email = "", // Will be fetched from users collection if needed
                            phone = "", // Will be fetched from users collection if needed
                            latitude = locationData?.location?.latitude ?: 0.0,
                            longitude = locationData?.location?.longitude ?: 0.0,
                            distanceInMiles = distanceInMiles
                        )
                        tempList.add(calingaPro)
                    }
                    
                    // Sort by distance if location is available
                    if (currentLatitude != 0.0 && currentLongitude != 0.0) {
                        tempList.sortBy { calingaPro ->
                            if (calingaPro.latitude != 0.0 && calingaPro.longitude != 0.0) {
                                calingaPro.distanceInMiles
                            } else {
                                Double.MAX_VALUE // Put CalingaPros without location at the end
                            }
                        }
                    }
                    
                    calingaProList.addAll(tempList)
                    filteredList.clear()
                    filteredList.addAll(calingaProList)
                    calingaProAdapter.notifyDataSetChanged()
                    
                    // Show message about number of CalingaPros found
                    val message = if (tempList.isEmpty()) {
                        if (currentLatitude != 0.0 && currentLongitude != 0.0) {
                            "No approved CalingaPros found within 10 miles"
                        } else {
                            "No approved CalingaPros available"
                        }
                    } else {
                        if (currentLatitude != 0.0 && currentLongitude != 0.0) {
                            "Found ${tempList.size} CalingaPro(s) within 10 miles"
                        } else {
                            "Found ${tempList.size} CalingaPro(s)"
                        }
                    }
                    Toast.makeText(this@CareseekerHomeActivity, message, Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "Error loading CalingaPro profiles: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
    
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371 // Radius of the Earth in kilometers
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
    
    private fun calculateDistanceInMiles(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 3959 // Radius of the Earth in miles
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
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
    val phone: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val distanceInMiles: Double = 0.0 // Distance from current user in miles
)
