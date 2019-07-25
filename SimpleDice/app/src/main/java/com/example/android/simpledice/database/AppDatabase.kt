package com.example.android.simpledice.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.android.simpledice.utils.DiceResults
import com.example.android.simpledice.utils.DiceRoll

@Database(entities = [DiceRoll::class, DiceResults::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun diceRollDao() : DiceRollDao
    abstract fun diceResultsDao() : DiceResultsDao

    companion object {
        private var sInstance : AppDatabase? = null
        private const val DATABASE_NAME = "dice_database"

        fun getInstance(context : Context) : AppDatabase? {
            if (sInstance == null) {
                synchronized(AppDatabase::class) {
                    sInstance = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, DATABASE_NAME).fallbackToDestructiveMigration().build() //TODO Handle DestuctiveMigration
                }
            }
            return sInstance
        }
    }
}