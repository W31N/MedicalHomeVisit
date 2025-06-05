package com.example.medicalhomevisit.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.medicalhomevisit.data.local.converter.Converters
import com.example.medicalhomevisit.data.local.dao.PatientDao
import com.example.medicalhomevisit.data.local.dao.ProtocolTemplateDao
import com.example.medicalhomevisit.data.local.entity.VisitEntity
import com.example.medicalhomevisit.data.local.dao.VisitDao
import com.example.medicalhomevisit.data.local.dao.VisitProtocolDao
import com.example.medicalhomevisit.data.local.entity.PatientEntity
import com.example.medicalhomevisit.data.local.entity.ProtocolTemplateEntity
import com.example.medicalhomevisit.data.local.entity.VisitProtocolEntity

@Database(
    entities = [
        ProtocolTemplateEntity::class,
        VisitProtocolEntity::class,
        VisitEntity::class,
        PatientEntity::class
    ],
    version = 3,
    exportSchema = true
)

@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun visitProtocolDao(): VisitProtocolDao
    abstract fun protocolTemplateDao(): ProtocolTemplateDao
    abstract fun visitDao(): VisitDao
    abstract fun patientDao(): PatientDao
}