package com.myapp.calingaapp

import com.google.firebase.Timestamp

data class User(
    val uid: String = "",
    val fullName: String = "",
    val email: String = "",
    val mobileNumber: String = "",
    val role: String = "", // "careseeker" | "calingapro" | "admin"
    val createdAt: Timestamp = Timestamp.now()
) {
    // Empty constructor required for Firestore
    constructor() : this("", "", "", "", "", Timestamp.now())
    
    fun toMap(): Map<String, Any> {
        return mapOf(
            "uid" to uid,
            "fullName" to fullName,
            "email" to email,
            "mobileNumber" to mobileNumber,
            "role" to role,
            "createdAt" to createdAt
        )
    }
}

data class Booking(
    val bookingId: String = "",
    val careseekerId: String = "",
    val calingaproId: String = "",
    val caregiverTier: String = "",
    val date: String = "",
    val time: String = "",
    val address: String = "",
    val notes: String = "",
    val ratePerHour: Double = 0.0,
    val totalAmount: Double = 0.0,
    val status: String = "pending", // "pending" | "accepted" | "completed" | "cancelled"
    val completionNotes: String = "", // Notes added when service is completed
    val createdAt: Timestamp = Timestamp.now(),
    val completedAt: Timestamp? = null // Timestamp when service was completed
) {
    constructor() : this("", "", "", "", "", "", "", "", 0.0, 0.0, "pending", "", Timestamp.now(), null)
    
    fun toMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "bookingId" to bookingId,
            "careseekerId" to careseekerId,
            "calingaproId" to calingaproId,
            "caregiverTier" to caregiverTier,
            "date" to date,
            "time" to time,
            "address" to address,
            "notes" to notes,
            "ratePerHour" to ratePerHour,
            "totalAmount" to totalAmount,
            "status" to status,
            "completionNotes" to completionNotes,
            "createdAt" to createdAt
        )
        
        completedAt?.let { map["completedAt"] = it }
        
        return map
    }
}

data class Notification(
    val notificationId: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val seen: Boolean = false,
    val timestamp: Timestamp = Timestamp.now()
) {
    constructor() : this("", "", "", "", false, Timestamp.now())
    
    fun toMap(): Map<String, Any> {
        return mapOf(
            "notificationId" to notificationId,
            "userId" to userId,
            "title" to title,
            "message" to message,
            "seen" to seen,
            "timestamp" to timestamp
        )
    }
}

data class Rating(
    val ratingId: String = "",
    val careseekerId: String = "",
    val calingaproId: String = "",
    val bookingId: String = "",
    val rating: Float = 0f,
    val feedback: String = "",
    val createdAt: Timestamp = Timestamp.now()
) {
    constructor() : this("", "", "", "", 0f, "", Timestamp.now())
    
    fun toMap(): Map<String, Any> {
        return mapOf(
            "ratingId" to ratingId,
            "careseekerId" to careseekerId,
            "calingaproId" to calingaproId,
            "bookingId" to bookingId,
            "rating" to rating,
            "feedback" to feedback,
            "createdAt" to createdAt
        )
    }
}
