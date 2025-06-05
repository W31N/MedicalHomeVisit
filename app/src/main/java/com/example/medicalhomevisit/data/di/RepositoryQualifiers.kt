package com.example.medicalhomevisit.data.di

import javax.inject.Qualifier

/**
 * Квалификатор для онлайн-версии PatientRepository
 * Используется для прямого взаимодействия с API
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class OnlinePatientRepository

/**
 * Квалификатор для офлайн-версии PatientRepository
 * Используется для работы с кэшированными данными
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class OfflinePatientRepository

// В будущем можно добавить квалификаторы для других репозиториев:
// @OnlineVisitRepository, @OfflineVisitRepository и т.д.