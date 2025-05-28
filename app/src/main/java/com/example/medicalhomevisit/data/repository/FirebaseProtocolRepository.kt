package com.example.medicalhomevisit.data.repository

import android.util.Log
import com.example.medicalhomevisit.data.model.VisitProtocol
import com.example.medicalhomevisit.domain.model.ProtocolTemplate
import com.example.medicalhomevisit.domain.repository.ProtocolRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

import kotlinx.coroutines.tasks.await
import java.util.Date

class FirebaseProtocolRepository : ProtocolRepository {
    private val db = Firebase.firestore
    private val protocolsCollection = db.collection("protocols")
    private val templatesCollection = db.collection("protocolTemplates")

    override suspend fun getProtocolForVisit(visitId: String): VisitProtocol? {
        try {
            val snapshot = protocolsCollection
                .whereEqualTo("visitId", visitId)
                .get()
                .await()

            if (snapshot.documents.isEmpty()) {
                return null
            }

            val doc = snapshot.documents[0]
            val data = doc.data ?: return null

            return VisitProtocol(
                id = doc.id,
                visitId = data["visitId"] as String,
                templateId = data["templateId"] as? String,
                complaints = data["complaints"] as? String ?: "",
                anamnesis = data["anamnesis"] as? String ?: "",
                objectiveStatus = data["objectiveStatus"] as? String ?: "",
                diagnosis = data["diagnosis"] as? String ?: "",
                diagnosisCode = data["diagnosisCode"] as? String ?: "",
                recommendations = data["recommendations"] as? String ?: "",
                temperature = (data["temperature"] as? Number)?.toFloat() ?: 0f,
                systolicBP = (data["systolicBP"] as? Number)?.toInt() ?: 0,
                diastolicBP = (data["diastolicBP"] as? Number)?.toInt() ?: 0,
                pulse = (data["pulse"] as? Number)?.toInt() ?: 0,
                additionalVitals = (data["additionalVitals"] as? Map<*, *>)?.entries?.associate {
                    it.key.toString() to it.value.toString()
                } ?: emptyMap(),
                createdAt = (data["createdAt"] as? Timestamp)?.toDate() ?: Date(),
                updatedAt = (data["updatedAt"] as? Timestamp)?.toDate() ?: Date()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting protocol for visit", e)
            return null
        }
    }

    override suspend fun saveProtocol(protocol: VisitProtocol): VisitProtocol {
        try {
            val protocolData = hashMapOf(
                "visitId" to protocol.visitId,
                "templateId" to protocol.templateId,
                "complaints" to protocol.complaints,
                "anamnesis" to protocol.anamnesis,
                "objectiveStatus" to protocol.objectiveStatus,
                "diagnosis" to protocol.diagnosis,
                "diagnosisCode" to protocol.diagnosisCode,
                "recommendations" to protocol.recommendations,
                "temperature" to protocol.temperature,
                "systolicBP" to protocol.systolicBP,
                "diastolicBP" to protocol.diastolicBP,
                "pulse" to protocol.pulse,
                "additionalVitals" to protocol.additionalVitals,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            val existingProtocol = getProtocolForVisit(protocol.visitId)

            if (existingProtocol != null) {
                protocolsCollection.document(existingProtocol.id)
                    .update(protocolData as Map<String, Any>)
                    .await()

                return protocol.copy(id = existingProtocol.id)
            } else {
                protocolData["createdAt"] = FieldValue.serverTimestamp()

                val docRef = if (protocol.id.isBlank() || protocol.id == "0") {
                    protocolsCollection.add(protocolData).await()
                } else {
                    protocolsCollection.document(protocol.id).set(protocolData).await()
                    protocolsCollection.document(protocol.id)
                }

                return protocol.copy(id = docRef.id)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving protocol", e)
            throw e
        }
    }

    override suspend fun getProtocolTemplates(): List<ProtocolTemplate> {
        try {
            val snapshot = templatesCollection.get().await()

            return snapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null

                    ProtocolTemplate(
                        id = doc.id,
                        name = data["name"] as String,
                        description = data["description"] as? String ?: "",
                        complaints = data["complaints"] as? String ?: "",
                        anamnesis = data["anamnesis"] as? String ?: "",
                        objectiveStatus = data["objectiveStatus"] as? String ?: "",
                        recommendations = data["recommendations"] as? String ?: "",
                        requiredVitals = (data["requiredVitals"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        category = data["category"] as? String
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error converting template document", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting protocol templates", e)
            return emptyList()
        }
    }

    override suspend fun getProtocolTemplateById(templateId: String): ProtocolTemplate? {
        try {
            val document = templatesCollection.document(templateId).get().await()

            if (!document.exists()) {
                return null
            }

            val data = document.data ?: return null

            return ProtocolTemplate(
                id = document.id,
                name = data["name"] as String,
                description = data["description"] as? String ?: "",
                complaints = data["complaints"] as? String ?: "",
                anamnesis = data["anamnesis"] as? String ?: "",
                objectiveStatus = data["objectiveStatus"] as? String ?: "",
                recommendations = data["recommendations"] as? String ?: "",
                requiredVitals = (data["requiredVitals"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                category = data["category"] as? String
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting template by ID", e)
            return null
        }
    }

    companion object {
        private const val TAG = "FirebaseProtocolRepo"
    }
}