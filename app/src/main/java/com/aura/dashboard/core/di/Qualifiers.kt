package com.aura.dashboard.core.di

import javax.inject.Qualifier

/** A process-wide scope for work that must outlive a screen (e.g. alarm handling). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher
