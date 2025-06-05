package com.example.medicalhomevisit.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "protocol_templates", indices = [Index(value = ["name"], unique = true)])
data class ProtocolTemplateEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(typeAffinity = ColumnInfo.TEXT) val description: String?,
    @ColumnInfo(typeAffinity = ColumnInfo.TEXT) val complaintsTemplate: String?,
    @ColumnInfo(typeAffinity = ColumnInfo.TEXT) val anamnesisTemplate: String?,
    @ColumnInfo(typeAffinity = ColumnInfo.TEXT) val objectiveStatusTemplate: String?,
    @ColumnInfo(typeAffinity = ColumnInfo.TEXT) val recommendationsTemplate: String?,
    val requiredVitals: List<String> = emptyList(),
    var createdAt: Date,
    var updatedAt: Date
)