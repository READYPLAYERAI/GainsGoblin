package com.readyplayerai.gainsgoblin.di

import android.content.Context
import androidx.room.Room
import com.readyplayerai.gainsgoblin.data.db.GainsGoblinDatabase
import com.readyplayerai.gainsgoblin.data.db.WorkoutDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): GainsGoblinDatabase =
        Room.databaseBuilder(
            context,
            GainsGoblinDatabase::class.java,
            "gainsgoblin.db"
        ).build()

    @Provides
    @Singleton
    fun provideWorkoutDao(database: GainsGoblinDatabase): WorkoutDao = database.workoutDao()
}
