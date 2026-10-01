package com.aura.dashboard.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.aura.dashboard.core.network.AndroidConnectivityObserver
import com.aura.dashboard.core.network.ConnectivityObserver
import com.aura.dashboard.core.time.AppClock
import com.aura.dashboard.core.time.SystemAppClock
import com.aura.dashboard.data.local.AuraDatabase
import com.aura.dashboard.data.preferences.DataStoreUserPreferencesRepository
import com.aura.dashboard.data.preferences.UserPreferencesRepository
import com.aura.dashboard.data.repository.CachedNewsRepository
import com.aura.dashboard.data.repository.DeviceQuickAppRepository
import com.aura.dashboard.data.repository.NewsRepository
import com.aura.dashboard.data.repository.OfflineFirstReminderRepository
import com.aura.dashboard.data.repository.OfflineFirstTaskRepository
import com.aura.dashboard.data.repository.QuickAppRepository
import com.aura.dashboard.data.repository.ReminderRepository
import com.aura.dashboard.data.repository.TaskRepository
import com.aura.dashboard.notifications.AlarmScheduler
import com.aura.dashboard.notifications.AndroidAlarmScheduler
import com.aura.dashboard.notifications.NotificationDismisser
import com.aura.dashboard.notifications.ReminderNotifier
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AuraDatabase =
        Room.databaseBuilder(context, AuraDatabase::class.java, AuraDatabase.NAME)
            // Schema changes must ship real migrations; only downgrades (dev builds) may reset data.
            .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
            .build()

    @Provides
    @Singleton
    fun provideDataStore(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        scope = CoroutineScope(SupervisorJob() + ioDispatcher),
        produceFile = { context.preferencesDataStoreFile("user_preferences") },
    )
}

@Module
@InstallIn(SingletonComponent::class)
abstract class BindingsModule {
    @Binds abstract fun bindClock(impl: SystemAppClock): AppClock
    @Binds abstract fun bindConnectivity(impl: AndroidConnectivityObserver): ConnectivityObserver
    @Binds abstract fun bindPreferences(impl: DataStoreUserPreferencesRepository): UserPreferencesRepository
    @Binds abstract fun bindAlarmScheduler(impl: AndroidAlarmScheduler): AlarmScheduler
    @Binds abstract fun bindNotificationDismisser(impl: ReminderNotifier): NotificationDismisser
    @Binds abstract fun bindTaskRepository(impl: OfflineFirstTaskRepository): TaskRepository
    @Binds abstract fun bindReminderRepository(impl: OfflineFirstReminderRepository): ReminderRepository
    @Binds abstract fun bindQuickAppRepository(impl: DeviceQuickAppRepository): QuickAppRepository
    @Binds abstract fun bindNewsRepository(impl: CachedNewsRepository): NewsRepository
}
