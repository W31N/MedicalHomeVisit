package com.example.medicalhomevisit.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.medicalhomevisit.data.local.entity.ProtocolTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProtocolTemplateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplates(templates: List<ProtocolTemplateEntity>)

    @Query("SELECT * FROM protocol_templates ORDER BY name ASC")
    fun getAllTemplates(): Flow<List<ProtocolTemplateEntity>>

    @Query("SELECT * FROM protocol_templates WHERE id = :templateId LIMIT 1")
    suspend fun getTemplateById(templateId: String): ProtocolTemplateEntity?
}