package com.example.android.simpledice.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.android.simpledice.utils.Constants
import com.example.android.simpledice.utils.DiceResults

@Dao
interface DiceResultsDao {

    @Query("SELECT * FROM ${Constants.DICE_RESULTS_TABLE_NAME} ORDER BY dateCreated DESC")
    fun loadAllDiceResults() : LiveData<List<DiceResults>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertDiceResults(diceResults: DiceResults)

    @Query("DELETE FROM ${Constants.DICE_RESULTS_TABLE_NAME}")
    fun deleteAllDiceResults()
}