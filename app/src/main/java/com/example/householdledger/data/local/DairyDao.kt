package com.example.householdledger.data.local

import androidx.room.*
import com.example.householdledger.data.model.DairyLog
import kotlinx.coroutines.flow.Flow

@Dao
interface DairyDao {
    @Query("SELECT * FROM dairy_logs ORDER BY date DESC")
    fun getAllLogs(): Flow<List<DairyLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<DairyLog>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: DairyLog)

    @Delete
    suspend fun deleteLog(log: DairyLog)

    @Query("DELETE FROM dairy_logs")
    suspend fun clearAll()
}
