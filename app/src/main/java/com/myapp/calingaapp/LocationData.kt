package com.myapp.calingaapp

import com.google.firebase.Timestamp

data class LocationData(
    val uid: String = "",
    val role: String = "", // "careseeker" | "calingapro"
    val isActive: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis(),
    val location: LocationCoordinates = LocationCoordinates()
) {
    // Empty constructor required for Firestore
    constructor() : this("", "", false, System.currentTimeMillis(), LocationCoordinates())
    
    fun toMap(): Map<String, Any> {
        return mapOf(
            "uid" to uid,
            "role" to role,
            "isActive" to isActive,
            "lastUpdated" to lastUpdated,
            "location" to location.toMap()
        )
    }
}

data class LocationCoordinates(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
) {
    constructor() : this(0.0, 0.0)
    
    fun toMap(): Map<String, Any> {
        return mapOf(
            "latitude" to latitude,
            "longitude" to longitude
        )
    }
}
