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
            onDelete = ForeignKey.CASCADE // или SET_NULL, в зависимости от логики
        ),
        ForeignKey(
            entity = ProtocolTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.SET_NULL // Шаблон может быть удален, протокол останется
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

    // Поля для синхронизации
    var isSynced: Boolean = true,
    var syncAction: String? = null, // "CREATE", "UPDATE", "DELETE"
    var lastSyncAttempt: Date? = null,
    var failCount: Int = 0
)
