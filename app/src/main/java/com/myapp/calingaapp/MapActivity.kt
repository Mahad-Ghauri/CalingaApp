package com.myapp.calingaapp

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint as OsmGeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class MapActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var mapView: MapView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var myLocationOverlay: MyLocationNewOverlay
    
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    
    private var userRole: String = ""
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize osmdroid configuration
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        
        setContentView(R.layout.activity_map)
        
        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        
        // Get user role from intent
        userRole = intent.getStringExtra("USER_ROLE") ?: "careseeker"
        
        // Initialize UI components
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        mapView = findViewById(R.id.mapView)
        
        // Set up navigation based on user role
        setupNavigation()
        
        // Set up custom menu icon to control drawer
        findViewById<ImageView>(R.id.imageViewMenu).setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }
        
        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        // Setup map
        setupMap()
        
        // Update legend based on user role
        updateLegend()
        
        // Check location permissions
        checkLocationPermissions()
        
        // Set up refresh button
        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabRefreshLocation).setOnClickListener {
            getCurrentLocationAndLoadData()
        }
        
        // Set up my location button in toolbar
        findViewById<ImageView>(R.id.imageViewLocation).setOnClickListener {
            myLocationOverlay.enableFollowLocation()
            getCurrentLocationAndLoadData()
        }
    }
    
    private fun setupNavigation() {
        when (userRole) {
            "caregiver" -> {
                navigationView.inflateMenu(R.menu.caregiver_drawer_menu)
            }
            else -> {
                navigationView.inflateMenu(R.menu.drawer_menu)
            }
        }
        navigationView.setNavigationItemSelectedListener(this)
    }
    
    private fun setupMap() {
        // Configure map
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.setBuiltInZoomControls(true)
        
        // Set initial zoom and center (Philippines)
        val mapController = mapView.controller
        mapController.setZoom(12.0)
        val startPoint = OsmGeoPoint(14.5995, 120.9842) // Manila coordinates
        mapController.setCenter(startPoint)
        
        // Setup location overlay
        myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), mapView)
        myLocationOverlay.enableMyLocation()
        myLocationOverlay.enableFollowLocation()
        mapView.overlays.add(myLocationOverlay)
    }
    
    private fun checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            // Permission already granted, get current location and load map data
            getCurrentLocationAndLoadData()
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocationAndLoadData()
            } else {
                Toast.makeText(this, "Location permission is required for map functionality", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun getCurrentLocationAndLoadData() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                location?.let {
                    val currentLocation = OsmGeoPoint(it.latitude, it.longitude)
                    mapView.controller.setCenter(currentLocation)
                    
                    // Update user's location in Firestore
                    updateUserLocationInFirestore(it.latitude, it.longitude)
                    
                    // Load relevant data based on user role
                    when (userRole) {
                        "careseeker" -> loadActiveCalingaPros()
                        "caregiver" -> loadCareseekers()
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun updateUserLocationInFirestore(latitude: Double, longitude: Double) {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            val locationData = mapOf(
                "location" to GeoPoint(latitude, longitude),
                "lastUpdated" to System.currentTimeMillis(),
                "isActive" to true
            )
            
            val collection = if (userRole == "caregiver") "calingapros" else "careseekers"
            
            db.collection(collection)
                .document(user.uid)
                .update(locationData)
                .addOnFailureListener { e ->
                    // If document doesn't exist, create it
                    db.collection(collection)
                        .document(user.uid)
                        .set(locationData)
                }
        }
    }
      private fun loadActiveCalingaPros() {
        // For careseekers: show active CalingaPros on the map
        db.collection("calingapros")
            .whereEqualTo("isActive", true)
            .get()
            .addOnSuccessListener { documents ->
                // Clear existing markers except user location
                mapView.overlays.removeAll { it is Marker }
                mapView.overlays.add(myLocationOverlay)
                
                for (document in documents) {
                    val geoPoint = document.getGeoPoint("location")
                    val name = document.getString("fullName") ?: "CalingaPro"
                    val specialization = document.getString("specialization") ?: "General Care"
                    val rating = document.getDouble("rating") ?: 0.0
                    val isVerified = document.getBoolean("isVerified") ?: false
                    
                    geoPoint?.let {
                        val description = buildString {
                            append("⭐ Rating: ${String.format("%.1f", rating)}/5.0\n")
                            append("🏥 Specialization: $specialization\n")
                            if (isVerified) append("✅ Verified Professional\n")
                            append("📞 Tap to contact")
                        }
                        
                        addMarkerToMap(
                            it.latitude,
                            it.longitude,
                            name,
                            description,
                            R.drawable.ic_caregiver_marker
                        )
                    }
                }
                mapView.invalidate()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading CalingaPros: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
      private fun loadCareseekers() {
        // For caregivers: show careseekers who need care
        db.collection("careseekers")
            .whereEqualTo("isActive", true)
            .get()
            .addOnSuccessListener { documents ->
                // Clear existing markers except user location
                mapView.overlays.removeAll { it is Marker }
                mapView.overlays.add(myLocationOverlay)
                
                for (document in documents) {
                    val geoPoint = document.getGeoPoint("location")
                    val name = document.getString("fullName") ?: "Careseeker"
                    val careType = document.getString("careType") ?: "General Care"
                    val urgency = document.getString("urgency") ?: "Normal"
                    val contactNumber = document.getString("contactNumber") ?: ""
                    
                    geoPoint?.let {
                        val description = buildString {
                            append("🏠 Care Type: $careType\n")
                            append("⚡ Urgency: $urgency\n")
                            if (contactNumber.isNotEmpty()) append("📞 Contact: $contactNumber\n")
                            append("Tap for more details")
                        }
                        
                        addMarkerToMap(
                            it.latitude,
                            it.longitude,
                            name,
                            description,
                            R.drawable.ic_careseeker_marker
                        )
                    }
                }
                mapView.invalidate()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading careseekers: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun addMarkerToMap(
        latitude: Double,
        longitude: Double,
        title: String,
        description: String,
        iconRes: Int
    ) {
        val marker = Marker(mapView)
        marker.position = OsmGeoPoint(latitude, longitude)
        marker.title = title
        marker.snippet = description
        
        // Set custom icon if available
        try {
            val icon = ContextCompat.getDrawable(this, iconRes)
            icon?.let { marker.icon = it }
        } catch (e: Exception) {
            // Use default marker if custom icon not found
        }
        
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        mapView.overlays.add(marker)
    }
    
    private fun updateLegend() {
        val legendText = findViewById<TextView>(R.id.textLegendOthers)
        when (userRole) {
            "caregiver" -> {
                legendText.text = "Careseekers"
            }
            else -> {
                legendText.text = "Available CalingaPros"
            }
        }
    }
    
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> {
                finish() // Go back to home
            }
            R.id.nav_map -> {
                // We're already in the map, just close drawer
                drawerLayout.closeDrawer(GravityCompat.START)
            }
            R.id.nav_profile -> {
                // Handle profile navigation based on user role
                // Implementation depends on your existing profile activities
                drawerLayout.closeDrawer(GravityCompat.START)
            }
            R.id.nav_documents -> {
                // Handle documents navigation (for caregivers)
                drawerLayout.closeDrawer(GravityCompat.START)
            }
            R.id.nav_logout -> {
                auth.signOut()
                Toast.makeText(this, "Signed out successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
        
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }
    
    override fun onResume() {
        super.onResume()
        mapView.onResume()
        
        // Refresh location when activity resumes
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocationAndLoadData()
        }
    }
    
    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        mapView.onDetach()
    }
}
