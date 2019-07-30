package com.gmail.maxfixsoftware.simpledice.database

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.gmail.maxfixsoftware.simpledice.utils.DiceRoll

class AllDiceRollsViewModel(application: Application) : AndroidViewModel(application) {

    var diceRolls : LiveData<List<DiceRoll>>? = null

    init {
        val database = AppDatabase.getInstance(this.getApplication())
        diceRolls = database!!.diceRollDao().loadAllDiceRolls()
    }
}