package com.example.android.simpledice.database

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.example.android.simpledice.utils.DiceResults

class AllDiceResultsViewModel(application: Application) : AndroidViewModel(application) {

    var diceResults : LiveData<List<DiceResults>>? = null

    init {
        val database = AppDatabase.getInstance(this.getApplication())
        diceResults = database!!.diceResultsDao().loadAllDiceResults()
    }
}