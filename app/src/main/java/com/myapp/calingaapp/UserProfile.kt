package com.myapp.calingaapp

data class UserProfile(
    val userId: String = "",
    val name: String = "",
    val age: Int = 0,
    val address: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val userType: String = "",
    val phoneNumber: String = "",
    val medicalConditions: String = "",
    val emergencyContact: String = ""
) {
    // Empty constructor required for Firestore
    constructor() : this("", "", 0, "", "", "", "", "", "", "")
    
    // Convert to Map for Firestore
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "userId" to userId,
            "name" to name,
            "age" to age,
            "address" to address,
            "email" to email,
            "photoUrl" to photoUrl,
            "userType" to userType,
            "phoneNumber" to phoneNumber,
            "medicalConditions" to medicalConditions,
            "emergencyContact" to emergencyContact
        )
    }
}
