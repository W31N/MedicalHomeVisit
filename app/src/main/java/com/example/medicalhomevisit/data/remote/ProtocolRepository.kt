package com.example.medicalhomevisit.data.remote

import com.example.medicalhomevisit.data.model.VisitProtocol
import com.example.medicalhomevisit.domain.model.ProtocolTemplate

interface ProtocolRepository {

    /**
     * Получить протокол для конкретного визита
     * @param visitId ID визита
     * @return протокол или null если не найден
     */
    suspend fun getProtocolForVisit(visitId: String): VisitProtocol?

    /**
     * Сохранить протокол (создать новый или обновить существующий)
     * @param protocol протокол для сохранения
     * @return сохраненный протокол с обновленными данными
     */
    suspend fun saveProtocol(protocol: VisitProtocol): VisitProtocol

    /**
     * Получить все доступные шаблоны протоколов
     * @return список шаблонов
     */
    suspend fun getProtocolTemplates(): List<ProtocolTemplate>

    /**
     * Получить шаблон протокола по ID
     * @param templateId ID шаблона
     * @return шаблон или null если не найден
     */
    suspend fun getProtocolTemplateById(templateId: String): ProtocolTemplate?

    /**
     * Применить шаблон к протоколу визита
     * Этот метод может быть реализован через специальный endpoint или
     * через комбинацию getProtocolTemplateById + saveProtocol
     * @param visitId ID визита
     * @param templateId ID шаблона
     * @return обновленный протокол
     */
    suspend fun applyTemplate(visitId: String, templateId: String): VisitProtocol

    /**
     * Обновить отдельное поле протокола
     * Опциональный метод для более эффективных обновлений
     * @param visitId ID визита
     * @param field название поля
     * @param value новое значение
     * @return обновленный протокол
     */
    suspend fun updateProtocolField(visitId: String, field: String, value: String): VisitProtocol

    /**
     * Обновить витальные показатели
     * Опциональный метод для более эффективных обновлений
     * @param visitId ID визита
     * @param temperature температура
     * @param systolicBP систолическое давление
     * @param diastolicBP диастолическое давление
     * @param pulse пульс
     * @return обновленный протокол
     */
    suspend fun updateVitals(
        visitId: String,
        temperature: Float? = null,
        systolicBP: Int? = null,
        diastolicBP: Int? = null,
        pulse: Int? = null
    ): VisitProtocol

    /**
     * Удалить протокол для визита
     * @param visitId ID визита
     */
    suspend fun deleteProtocol(visitId: String)

    /**
     * Синхронизация данных (для офлайн поддержки)
     * @return результат синхронизации
     */
    suspend fun syncProtocols(): Result<Unit>

    /**
     * Кэширование протоколов для офлайн работы
     * @param protocols список протоколов для кэширования
     */
    suspend fun cacheProtocols(protocols: List<VisitProtocol>)

    /**
     * Получить кэшированные протоколы
     * @return список кэшированных протоколов
     */
    suspend fun getCachedProtocols(): List<VisitProtocol>

    /**
     * Получить кэшированный протокол для визита
     * @param visitId ID визита
     * @return кэшированный протокол или null
     */
    suspend fun getCachedProtocolForVisit(visitId: String): VisitProtocol?
}