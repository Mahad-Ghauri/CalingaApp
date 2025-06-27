package com.myapp.calingaapp

import android.content.Context
import android.location.Location
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LocationManager(private val context: Context) {
    
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    companion object {
        private const val LOCATIONS_COLLECTION = "locations"
    }
    
    /**
     * Updates the current user's location in the locations collection
     */
    fun updateUserLocation(latitude: Double, longitude: Double, callback: (Boolean, String?) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            callback(false, "User not authenticated")
            return
        }
        
        // First get the user's role from the users collection
        db.collection("users").document(currentUser.uid)
            .get()
            .addOnSuccessListener { userDoc ->
                if (userDoc.exists()) {
                    val userRole = userDoc.getString("role") ?: ""
                    
                    val locationData = LocationData(
                        uid = currentUser.uid,
                        role = userRole,
                        isActive = true,
                        lastUpdated = System.currentTimeMillis(),
                        location = LocationCoordinates(latitude, longitude)
                    )
                    
                    db.collection(LOCATIONS_COLLECTION).document(currentUser.uid)
                        .set(locationData.toMap())
                        .addOnSuccessListener {
                            callback(true, null)
                        }
                        .addOnFailureListener { exception ->
                            callback(false, exception.message)
                        }
                } else {
                    callback(false, "User profile not found")
                }
            }
            .addOnFailureListener { exception ->
                callback(false, exception.message)
            }
    }
    
    /**
     * Gets locations of all active CalingaPros within the specified radius
     */
    fun getNearbyCalingaPros(
        userLatitude: Double, 
        userLongitude: Double, 
        radiusMiles: Double,
        callback: (List<LocationData>, String?) -> Unit
    ) {
        db.collection(LOCATIONS_COLLECTION)
            .whereEqualTo("role", "calingapro")
            .whereEqualTo("isActive", true)
            .get()
            .addOnSuccessListener { documents ->
                val nearbyLocations = mutableListOf<LocationData>()
                
                for (document in documents) {
                    val locationData = document.toObject(LocationData::class.java)
                    
                    // Calculate distance
                    val distance = calculateDistanceInMiles(
                        userLatitude, userLongitude,
                        locationData.location.latitude, locationData.location.longitude
                    )
                    
                    // Include if within radius or if no location constraint
                    if (distance <= radiusMiles || 
                        (userLatitude == 0.0 || userLongitude == 0.0 || 
                         locationData.location.latitude == 0.0 || locationData.location.longitude == 0.0)) {
                        nearbyLocations.add(locationData)
                    }
                }
                
                // Sort by distance
                if (userLatitude != 0.0 && userLongitude != 0.0) {
                    nearbyLocations.sortBy { locationData ->
                        if (locationData.location.latitude != 0.0 && locationData.location.longitude != 0.0) {
                            calculateDistanceInMiles(
                                userLatitude, userLongitude,
                                locationData.location.latitude, locationData.location.longitude
                            )
                        } else {
                            Double.MAX_VALUE
                        }
                    }
                }
                
                callback(nearbyLocations, null)
            }
            .addOnFailureListener { exception ->
                callback(emptyList(), exception.message)
            }
    }
    
    /**
     * Gets the current user's location data
     */
    fun getCurrentUserLocation(callback: (LocationData?, String?) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            callback(null, "User not authenticated")
            return
        }
        
        db.collection(LOCATIONS_COLLECTION).document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val locationData = document.toObject(LocationData::class.java)
                    callback(locationData, null)
                } else {
                    callback(null, "Location data not found")
                }
            }
            .addOnFailureListener { exception ->
                callback(null, exception.message)
            }
    }
    
    /**
     * Sets the user's active status in location collection
     */
    fun setUserActiveStatus(isActive: Boolean, callback: (Boolean, String?) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            callback(false, "User not authenticated")
            return
        }
        
        db.collection(LOCATIONS_COLLECTION).document(currentUser.uid)
            .update(
                mapOf(
                    "isActive" to isActive,
                    "lastUpdated" to System.currentTimeMillis()
                )
            )
            .addOnSuccessListener {
                callback(true, null)
            }
            .addOnFailureListener { exception ->
                callback(false, exception.message)
            }
    }
    
    private fun calculateDistanceInMiles(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 3959 // Radius of the Earth in miles
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
                kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return r * c
    }
}
