package com.example.android.simpledice.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.android.simpledice.utils.Constants
import com.example.android.simpledice.utils.DiceRoll

@Dao
interface DiceRollDao {

    @Query("SELECT * FROM ${Constants.DICE_ROLL_TABLE_NAME} ORDER BY databaseId")
    fun loadAllDiceRolls() : LiveData<List<DiceRoll>>

    @Query("SELECT * FROM ${Constants.DICE_ROLL_TABLE_NAME} ORDER BY databaseId")
    fun loadAllDiceRollsForWidget() : List<DiceRoll>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertDiceRoll(diceRoll: DiceRoll)

    @Delete
    fun deleteDiceRoll(diceRoll: DiceRoll)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateDiceRoll(diceRoll: DiceRoll)

}