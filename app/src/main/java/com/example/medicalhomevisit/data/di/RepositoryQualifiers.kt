package com.example.medicalhomevisit.data.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class OnlinePatientRepository

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class OfflinePatientRepository