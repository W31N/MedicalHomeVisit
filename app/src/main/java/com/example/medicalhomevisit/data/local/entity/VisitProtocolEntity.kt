package com.example.medicalhomevisit.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "visit_protocols",
    foreignKeys = [
        ForeignKey(
            entity = VisitEntity::class,
            parentColumns = ["id"],
            childColumns = ["visitId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ProtocolTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["visitId"], unique = true), Index(value = ["templateId"])]
)
    data class VisitProtocolEntity(
    @PrimaryKey val id: String,
    val visitId: String,
    val templateId: String?,
    @ColumnInfo(typeAffinity = ColumnInfo.TEXT) var complaints: String?,
    @ColumnInfo(typeAffinity = ColumnInfo.TEXT) var anamnesis: String?,
    @ColumnInfo(typeAffinity = ColumnInfo.TEXT) var objectiveStatus: String?,
    @ColumnInfo(typeAffinity = ColumnInfo.TEXT) var diagnosis: String?,
    var diagnosisCode: String?,
    @ColumnInfo(typeAffinity = ColumnInfo.TEXT) var recommendations: String?,
    var temperature: Float?,
    var systolicBP: Int?,
    var diastolicBP: Int?,
    var pulse: Int?,
    var additionalVitals: Map<String, String> = emptyMap(), // Потребует TypeConverter
    var createdAt: Date,
    var updatedAt: Date,

    var isSynced: Boolean = true,
    var syncAction: String? = null,
    var lastSyncAttempt: Date? = null,
    var failCount: Int = 0
)
