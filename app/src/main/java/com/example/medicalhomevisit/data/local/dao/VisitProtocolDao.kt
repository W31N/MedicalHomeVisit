package com.example.medicalhomevisit.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.medicalhomevisit.data.local.entity.VisitProtocolEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface VisitProtocolDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProtocol(protocol: VisitProtocolEntity)

    @Query("UPDATE visit_protocols SET isSynced = :synced, syncAction = null, lastSyncAttempt = :attemptTime WHERE id = :protocolId")
    suspend fun markAsSynced(protocolId: String, synced: Boolean = true, attemptTime: Date = Date())

    @Query("UPDATE visit_protocols SET failCount = failCount + 1, lastSyncAttempt = :attemptTime WHERE id = :protocolId")
    suspend fun incrementFailCount(protocolId: String, attemptTime: Date = Date())

    @Query("SELECT * FROM visit_protocols WHERE visitId = :visitId LIMIT 1")
    fun getProtocolByVisitId(visitId: String): Flow<VisitProtocolEntity?>

    @Query("SELECT * FROM visit_protocols WHERE visitId = :visitId LIMIT 1")
    suspend fun getProtocolByVisitIdOnce(visitId: String): VisitProtocolEntity?

    @Query("SELECT * FROM visit_protocols WHERE isSynced = 0")
    suspend fun getUnsyncedProtocols(): List<VisitProtocolEntity>

    @Query("DELETE FROM visit_protocols WHERE id = :protocolId")
    suspend fun deleteProtocolById(protocolId: String)
}