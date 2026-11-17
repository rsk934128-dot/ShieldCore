package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_history")
data class ScanHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val scanType: String,
    val filesScanned: Int,
    val appsAudited: Int,
    val threatsFound: Int,
    val junkBytesFound: Long,
    val securityScore: Int,
    val summary: String
)
