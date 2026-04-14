package com.app.railnav.core.di

import android.app.Application
import androidx.room.Room
import com.app.railnav.core.data.local.AppDatabase
import com.app.railnav.core.data.local.dao.SearchHistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(app: Application): AppDatabase {
        return Room.databaseBuilder(
            app,
            AppDatabase::class.java,
            "railnav_db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideSearchHistoryDao(db: AppDatabase): SearchHistoryDao {
        return db.searchHistoryDao
    }
}
