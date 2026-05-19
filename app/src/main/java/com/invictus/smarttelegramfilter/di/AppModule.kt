package com.invictus.smarttelegramfilter.di

import android.content.Context
import androidx.room.Room
import com.invictus.smarttelegramfilter.data.db.AppDatabase
import com.invictus.smarttelegramfilter.data.db.dao.ChannelFilterDao
import com.invictus.smarttelegramfilter.data.db.dao.MatchedMessageDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "smart_filter.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideChannelFilterDao(db: AppDatabase): ChannelFilterDao = db.channelFilterDao()

    @Provides
    fun provideMatchedMessageDao(db: AppDatabase): MatchedMessageDao = db.matchedMessageDao()
}
