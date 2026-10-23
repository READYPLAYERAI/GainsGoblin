package com.readyplayerai.gainsgoblin.di

import android.content.Context
import com.readyplayerai.gainsgoblin.data.backup.BackupFileStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BackupModule {
    @Provides
    @Singleton
    fun provideBackupFileStore(@ApplicationContext context: Context): BackupFileStore =
        BackupFileStore(File(context.filesDir, "backups"))
}
