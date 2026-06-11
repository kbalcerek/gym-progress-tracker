package com.example.gymprogresstracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BodyweightDao {
    @Query("SELECT * FROM bodyweight_entries ORDER BY date ASC")
    fun getAll(): Flow<List<BodyweightEntry>>

    @Query("SELECT * FROM bodyweight_entries ORDER BY date ASC")
    suspend fun getAllList(): List<BodyweightEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: BodyweightEntry): Long

    @Update
    suspend fun update(entry: BodyweightEntry)

    @Delete
    suspend fun delete(entry: BodyweightEntry)

    @Query("DELETE FROM bodyweight_entries")
    suspend fun deleteAll()
}
