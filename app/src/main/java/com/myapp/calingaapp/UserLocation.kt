package com.myapp.calingaapp

data class UserLocation(
    val userId: String = "",
    val fullName: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val lastUpdated: Long = 0L,
    val isActive: Boolean = false,
    val userRole: String = "",
    val specialization: String = "", // For CalingaPros
    val careType: String = "" // For Careseekers
)
