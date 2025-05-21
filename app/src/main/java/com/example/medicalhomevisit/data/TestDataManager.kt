package com.example.medicalhomevisit.data

import com.example.medicalhomevisit.data.model.Gender
import com.example.medicalhomevisit.data.model.Patient
import com.example.medicalhomevisit.data.model.Visit
import com.example.medicalhomevisit.data.model.VisitProtocol
import com.example.medicalhomevisit.data.model.VisitStatus
import com.example.medicalhomevisit.domain.model.ProtocolTemplate
import java.util.Date
import java.util.UUID

object TestDataManager {
    // Кэш для хранения данных
    private val visits = mutableMapOf<String, Visit>()
    private val patients = mutableMapOf<String, Patient>()
    private val protocols = mutableMapOf<String, VisitProtocol>()
    private val protocolTemplates = mutableListOf<ProtocolTemplate>()

    // Флаг для отслеживания инициализации
    private var initialized = false

    // Инициализация тестовых данных
    private fun initializeIfNeeded() {
        if (initialized) return

        val now = Date()

        // Создание тестовых визитов
        val visit1 = Visit(
            id = "1",
            patientId = "p1",
            scheduledTime = Date(now.time + 3600000), // через час
            status = VisitStatus.PLANNED,
            address = "ул. Ленина, д. 15, кв. 7",
            reasonForVisit = "ОРВИ, повторный осмотр",
            notes = "Пациент пожилой, необходимо уделить особое внимание давлению",
            createdAt = now,
            updatedAt = now
        )

        val visit2 = Visit(
            id = "2",
            patientId = "p2",
            scheduledTime = Date(now.time + 7200000), // через 2 часа
            status = VisitStatus.PLANNED,
            address = "ул. Гагарина, д. 23, кв. 12",
            reasonForVisit = "Гипертония, контроль давления",
            createdAt = now,
            updatedAt = now
        )

        val visit3 = Visit(
            id = "3",
            patientId = "p3",
            scheduledTime = Date(now.time + 10800000), // через 3 часа
            status = VisitStatus.PLANNED,
            address = "ул. Пушкина, д. 8, кв. 44",
            reasonForVisit = "Патронаж новорожденного",
            createdAt = now,
            updatedAt = now
        )

        // Сохранение визитов в кэш
        visits[visit1.id] = visit1
        visits[visit2.id] = visit2
        visits[visit3.id] = visit3

        // Создание тестовых пациентов
        val patient1 = Patient(
            id = "p1",
            fullName = "Иванов Иван Иванович",
            dateOfBirth = Date(70, 0, 1), // 1 января 1970
            age = 55,
            gender = Gender.MALE,
            address = "ул. Ленина, д. 15, кв. 7",
            phoneNumber = "+7 (999) 123-45-67",
            policyNumber = "1234567890123456",
            allergies = listOf("Пенициллин", "Пыль"),
            chronicConditions = listOf("Гипертония")
        )

        val patient2 = Patient(
            id = "p2",
            fullName = "Петрова Анна Сергеевна",
            dateOfBirth = Date(85, 3, 15), // 15 апреля 1985
            age = 40,
            gender = Gender.FEMALE,
            address = "ул. Гагарина, д. 23, кв. 12",
            phoneNumber = "+7 (999) 234-56-78",
            policyNumber = "2345678901234567",
            allergies = null,
            chronicConditions = listOf("Гипертония", "Сахарный диабет 2 типа")
        )

        val patient3 = Patient(
            id = "p3",
            fullName = "Сидорова Мария Петровна",
            dateOfBirth = Date(90, 6, 10), // 10 июля 1990
            age = 35,
            gender = Gender.FEMALE,
            address = "ул. Пушкина, д. 8, кв. 44",
            phoneNumber = "+7 (999) 345-67-89",
            policyNumber = "3456789012345678",
            allergies = null,
            chronicConditions = null
        )

        // Сохранение пациентов в кэш
        patients[patient1.id] = patient1
        patients[patient2.id] = patient2
        patients[patient3.id] = patient3

        // Создание тестового протокола для визита 2
        val protocol2 = VisitProtocol(
            id = "proto2",
            visitId = "2",
            templateId = null,
            complaints = "Головная боль, повышенное давление",
            anamnesis = "Гипертоническая болезнь 2 ст., регулярно принимает эналаприл",
            objectiveStatus = "Кожные покровы обычной окраски. Дыхание везикулярное. ЧДД 18 в минуту. Тоны сердца ясные, ритмичные.",
            diagnosis = "Гипертоническая болезнь 2 стадии, 2 степени, риск 3",
            diagnosisCode = "I11.9",
            recommendations = "Продолжить прием эналаприла 10мг утром. Контроль АД 2 раза в день.",
            temperature = 36.7f,
            systolicBP = 160,
            diastolicBP = 95,
            pulse = 78,
            additionalVitals = mapOf("Гликемия" to "5.8 ммоль/л"),
            createdAt = Date(now.time - 86400000), // вчера
            updatedAt = Date(now.time - 86400000)
        )

        // Сохранение протокола в кэш
        protocols["proto2"] = protocol2

        // Создание тестовых шаблонов протоколов
        val template1 = ProtocolTemplate(
            id = "template1",
            name = "ОРВИ",
            description = "Шаблон для острых респираторных вирусных инфекций",
            complaints = "Повышение температуры тела, головная боль, боль в горле, насморк, общая слабость",
            anamnesis = "Заболел(а) остро ... дней назад, когда появились вышеуказанные жалобы",
            objectiveStatus = "Состояние удовлетворительное. Кожные покровы обычной окраски. Зев гиперемирован. Миндалины не увеличены. Дыхание везикулярное, хрипов нет. ЧДД ... в минуту. Тоны сердца ясные, ритмичные. ЧСС ... уд/мин. АД ... мм.рт.ст. Живот мягкий, безболезненный.",
            recommendations = "Обильное теплое питье. Постельный режим. Парацетамол 500 мг при температуре выше 38.5°C. Промывание носа физ. раствором. Контроль температуры. При ухудшении - вызвать врача.",
            requiredVitals = listOf("temperature", "pulse", "blood_pressure")
        )

        val template2 = ProtocolTemplate(
            id = "template2",
            name = "Гипертония",
            description = "Шаблон для контроля гипертонической болезни",
            complaints = "Головная боль, головокружение, повышенное артериальное давление",
            anamnesis = "Страдает гипертонической болезнью в течение ... лет. Регулярно принимает гипотензивные препараты.",
            objectiveStatus = "Состояние удовлетворительное. Кожные покровы обычной окраски. Дыхание везикулярное, хрипов нет. ЧДД ... в минуту. Тоны сердца ритмичные, акцент II тона на аорте. ЧСС ... уд/мин. АД ... мм.рт.ст. Живот мягкий, безболезненный.",
            recommendations = "Продолжить прием гипотензивной терапии. Контроль АД утром и вечером. Ограничение потребления соли. Дозированные физические нагрузки.",
            requiredVitals = listOf("blood_pressure", "pulse")
        )

        val template3 = ProtocolTemplate(
            id = "template3",
            name = "Патронаж новорожденного",
            description = "Шаблон для патронажа новорожденного",
            complaints = "Жалоб нет",
            anamnesis = "Ребенок от ... беременности, ... родов. Беременность протекала ... Роды на ... неделе. Масса при рождении ... г, рост ... см.",
            objectiveStatus = "Состояние удовлетворительное. Кожные покровы чистые, физиологической окраски. Пуповинный остаток/ранка ... Большой родничок ... см, не напряжен. Дыхание пуэрильное, хрипов нет. ЧДД ... в мин. Тоны сердца ясные, ритмичные. ЧСС ... уд/мин. Живот мягкий, безболезненный. Стул ... раз в сутки, характер ...",
            recommendations = "Грудное вскармливание по требованию. Обработка пуповинного остатка/ранки. Профилактика опрелостей. Прогулки на свежем воздухе. Посещение поликлиники через ... дней.",
            requiredVitals = listOf("temperature", "weight", "height")
        )

        // Сохранение шаблонов в кэш
        protocolTemplates.add(template1)
        protocolTemplates.add(template2)
        protocolTemplates.add(template3)

        initialized = true
    }

    // Методы для работы с визитами

    fun getVisit(id: String): Visit {
        initializeIfNeeded()
        return visits[id] ?: throw IllegalArgumentException("Визит не найден")
    }

    fun getAllVisits(): List<Visit> {
        initializeIfNeeded()
        return visits.values.toList()
    }

    fun updateVisitStatus(visitId: String, newStatus: VisitStatus) {
        initializeIfNeeded()
        val visit = visits[visitId] ?: throw IllegalArgumentException("Визит не найден")
        val updatedVisit = visit.copy(
            status = newStatus,
            updatedAt = Date()
        )
        visits[visitId] = updatedVisit
    }

    // Методы для работы с пациентами

    fun getPatient(id: String): Patient {
        initializeIfNeeded()
        return patients[id] ?: throw IllegalArgumentException("Пациент не найден")
    }

    // Методы для работы с протоколами

    fun getProtocolForVisit(visitId: String): VisitProtocol? {
        initializeIfNeeded()
        return protocols.values.find { it.visitId == visitId }
    }

    fun saveProtocol(protocol: VisitProtocol): VisitProtocol {
        initializeIfNeeded()
        protocols[protocol.id] = protocol
        return protocol
    }

    // Методы для работы с шаблонами протоколов

    fun getProtocolTemplates(): List<ProtocolTemplate> {
        initializeIfNeeded()
        return protocolTemplates
    }

    fun getProtocolTemplateById(templateId: String): ProtocolTemplate? {
        initializeIfNeeded()
        return protocolTemplates.find { it.id == templateId }
    }
}