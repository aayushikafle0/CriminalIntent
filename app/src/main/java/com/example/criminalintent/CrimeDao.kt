package com.example.criminalintent

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CrimeDao {

    @Query("SELECT * FROM crimes ORDER BY date DESC")
    fun getCrimes(): Flow<List<Crime>>

    @Query("SELECT * FROM crimes WHERE id = :id")
    fun getCrime(id: String): Flow<Crime?>

    @Insert
    suspend fun addCrime(crime: Crime)

    @Update
    suspend fun updateCrime(crime: Crime)

    @Delete
    suspend fun deleteCrime(crime: Crime)
}
