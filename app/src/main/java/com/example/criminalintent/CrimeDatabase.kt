package com.example.criminalintent

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Crime::class],
    version = 1,
    exportSchema = false
)
abstract class CrimeDatabase : RoomDatabase() {

    abstract fun crimeDao(): CrimeDao

    companion object {
        @Volatile
        private var INSTANCE: CrimeDatabase? = null

        fun getInstance(context: Context): CrimeDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CrimeDatabase::class.java,
                    "crime-database"
                ).build().also {
                    INSTANCE = it
                }
            }
        }
    }
}