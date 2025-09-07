package com.a1.sitesync.data.models

data class User(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val phoneNumber: String? = null,
    val profilePictureUrl: String? = null,
    val role: String = "surveyor" // e.g., 'surveyor', 'admin'
)