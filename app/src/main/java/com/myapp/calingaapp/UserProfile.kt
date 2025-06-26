package com.myapp.calingaapp

import com.google.firebase.Timestamp

data class UserProfile(
    val userId: String = "",
    val name: String = "",
    val age: Int? = null,
    val address: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val bio: String = "",
    val profilePhotoUrl: String = "",
    val specialties: List<String> = emptyList(),
    val caregiverTier: String = "",
    val isApproved: Boolean = false,
    val isActive: Boolean = false,
    val documents: List<String> = emptyList(),
    val createdAt: Timestamp = Timestamp.now()
) {
    // Empty constructor required for Firestore
    constructor() : this("", "", null, "", 0.0, 0.0, "", "", emptyList(), "", false, false, emptyList(), Timestamp.now())
    
    // Convert to Map for Firestore
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "userId" to userId,
            "name" to name,
            "age" to age,
            "address" to address,
            "latitude" to latitude,
            "longitude" to longitude,
            "bio" to bio,
            "profilePhotoUrl" to profilePhotoUrl,
            "specialties" to specialties,
            "caregiverTier" to caregiverTier,
            "isApproved" to isApproved,
            "isActive" to isActive,
            "documents" to documents,
            "createdAt" to createdAt
        )
    }
}
