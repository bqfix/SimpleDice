package com.example.android.simpledice.database

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.example.android.simpledice.utils.DiceRoll

class AllDiceRollsViewModel(application: Application) : AndroidViewModel(application) {

    var diceRolls : LiveData<List<DiceRoll>>? = null

    init {
        val database = AppDatabase.getInstance(this.getApplication())
        diceRolls = database!!.diceRollDao().loadAllDiceRolls()
    }
}