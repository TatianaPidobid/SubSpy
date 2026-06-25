package com.subspy.app.data.model

data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val isPremium: Boolean = false,
    val notifyThreeDaysBefore: Boolean = true,
    val notifySevenDaysBefore: Boolean = false,
    val lastScanDate: String = ""
)
