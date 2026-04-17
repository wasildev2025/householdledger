package com.example.householdledger.di

import android.content.Context
import androidx.room.Room
import com.example.householdledger.data.local.*
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
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "household_ledger_db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideTransactionDao(database: AppDatabase): TransactionDao {
        return database.transactionDao()
    }

    @Provides
    fun provideCategoryDao(database: AppDatabase): CategoryDao {
        return database.categoryDao()
    }

    @Provides
    fun provideMemberDao(database: AppDatabase): MemberDao {
        return database.memberDao()
    }

    @Provides
    fun provideServantDao(database: AppDatabase): ServantDao {
        return database.servantDao()
    }

    @Provides
    fun provideDairyDao(database: AppDatabase): DairyDao {
        return database.dairyDao()
    }

    @Provides
    fun provideMessageDao(database: AppDatabase): MessageDao {
        return database.messageDao()
    }

    @Provides
    fun provideRecurringTemplateDao(database: AppDatabase): RecurringTemplateDao {
        return database.recurringTemplateDao()
    }

    @Provides
    fun provideOfflineQueueDao(database: AppDatabase): OfflineQueueDao {
        return database.offlineQueueDao()
    }
}
