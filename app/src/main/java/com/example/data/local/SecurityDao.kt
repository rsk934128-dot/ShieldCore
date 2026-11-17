package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SecurityDao {
    @Query("SELECT * FROM malware_signatures ORDER BY severity DESC, threatName ASC")
    fun observeAllSignatures(): Flow<List<MalwareSignatureEntity>>

    @Query("SELECT COUNT(*) FROM malware_signatures")
    fun observeSignatureCount(): Flow<Int>

    @Query("SELECT * FROM malware_signatures WHERE hashSha256 = :hash LIMIT 1")
    suspend fun findSignatureByHash(hash: String): MalwareSignatureEntity?

    @Query(
        "SELECT * FROM malware_signatures " +
            "WHERE LOWER(hashSha256) = LOWER(:sha256) " +
            "OR (:md5 != '' AND LOWER(hashMd5) = LOWER(:md5)) " +
            "LIMIT 1"
    )
    suspend fun findSignatureByHashes(sha256: String, md5: String): MalwareSignatureEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSignatures(signatures: List<MalwareSignatureEntity>)

    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC LIMIT 20")
    fun observeRecentScans(): Flow<List<ScanHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScanHistory(entry: ScanHistoryEntity)

    @Query("DELETE FROM scan_history")
    suspend fun clearScanHistory()
}
