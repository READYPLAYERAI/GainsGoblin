package com.readyplayerai.gainsgoblin.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides the process-wide DataStore instance.
 *
 * Constructed directly on `filesDir/datastore/gainsgoblin_data.preferences_pb`, the
 * same path the legacy `preferencesDataStore(name = "gainsgoblin_data")` delegate
 * used, so existing persisted data is read unchanged.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("gainsgoblin_data") }
}
