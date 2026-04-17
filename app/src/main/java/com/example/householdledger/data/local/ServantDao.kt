package com.example.householdledger.data.local

import androidx.room.*
import com.example.householdledger.data.model.Servant
import kotlinx.coroutines.flow.Flow

@Dao
interface ServantDao {
    @Query("SELECT * FROM servants")
    fun getAllServants(): Flow<List<Servant>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServants(servants: List<Servant>)

    @Delete
    suspend fun deleteServant(servant: Servant)
}
