package com.readyplayerai.gainsgoblin.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    // Same Json config as the legacy storage layer: explicitNulls=false so nulls
    // are omitted, ignoreUnknownKeys for forward-compat with future fields,
    // encodeDefaults so all persisted fields round-trip.
    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        explicitNulls = false
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
}
