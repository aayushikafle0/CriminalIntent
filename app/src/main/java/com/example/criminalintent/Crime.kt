package com.example.criminalintent

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "crimes")
data class Crime(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val date: Long = System.currentTimeMillis(),
    val isSolved: Boolean = false,
    val photoFileName: String? = null
)
