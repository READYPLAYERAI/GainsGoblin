package com.readyplayerai.gainsgoblin.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import com.readyplayerai.gainsgoblin.data.persistenceJson
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideJson(): Json = persistenceJson
}
