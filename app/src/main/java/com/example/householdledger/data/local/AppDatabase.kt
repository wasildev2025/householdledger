package com.example.householdledger.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.householdledger.data.model.*

@Database(
    entities = [
        Transaction::class,
        Category::class,
        Member::class,
        Servant::class,
        DairyLog::class,
        Message::class,
        RecurringTemplate::class,
        OfflineQueueItem::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun memberDao(): MemberDao
    abstract fun servantDao(): ServantDao
    abstract fun dairyDao(): DairyDao
    abstract fun messageDao(): MessageDao
    abstract fun recurringTemplateDao(): RecurringTemplateDao
    abstract fun offlineQueueDao(): OfflineQueueDao
}
