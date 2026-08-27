package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_progress")
data class UserProgress(
    @PrimaryKey val id: Int = 1,
    val username: String = "حريف كراش",
    val currentRiddleLevel: Int = 1,
    val currentWordLevel: Int = 1,
    val gems: Int = 925,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val lastDailyClaim: Long = 0L,
    val streak: Int = 0,
    val unlockedBackgrounds: String = "DEFAULT",
    val selectedBackground: String = "DEFAULT"
)
