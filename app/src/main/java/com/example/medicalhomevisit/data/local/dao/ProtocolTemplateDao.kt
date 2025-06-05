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
    suspend fun insertTemplate(template: ProtocolTemplateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplates(templates: List<ProtocolTemplateEntity>)

    @Update
    suspend fun updateTemplate(template: ProtocolTemplateEntity) // Если они редактируемые

    @Query("SELECT * FROM protocol_templates ORDER BY name ASC")
    fun getAllTemplates(): Flow<List<ProtocolTemplateEntity>>

    @Query("SELECT * FROM protocol_templates WHERE id = :templateId LIMIT 1")
    suspend fun getTemplateById(templateId: String): ProtocolTemplateEntity?

    @Query("SELECT * FROM protocol_templates WHERE name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchTemplates(query: String): Flow<List<ProtocolTemplateEntity>>

    // Методы для синхронизации, если шаблоны могут меняться на клиенте и синхронизироваться "вверх"
    // @Query("SELECT * FROM protocol_templates WHERE isSynced = 0")
    // suspend fun getUnsyncedTemplates(): List<ProtocolTemplateEntity>
    // @Query("UPDATE protocol_templates SET isSynced = 1, syncAction = null WHERE id = :templateId")
    // suspend fun markTemplateAsSynced(templateId: String)
}