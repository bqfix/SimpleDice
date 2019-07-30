package com.gmail.maxfixsoftware.simpledice.utils

import android.content.Context
import android.os.AsyncTask
import android.preference.PreferenceManager
import com.gmail.maxfixsoftware.simpledice.R

class RollAsyncTask(private val mRollAsyncPreExecute: RollAsyncPreExecute, private val mRollAsyncPostExecute: RollAsyncPostExecute, context : Context) :
    AsyncTask<DiceRoll, Void, DiceResults>() {

    private val mContext : Context

    init{
        mContext = context.applicationContext //Initialize context as application context, to prevent leaks in the event that the activity outlives the asynctask (rare)
    }

    override fun doInBackground(vararg params: DiceRoll): DiceResults {
        val diceRoll = params[0]
        val formula = diceRoll.formula
        var compiledRolls = ""
        var total: Long = 0
        val splitFormulaByD = arrayListOf<Array<String>>()

        //Create an ArrayList containing all the plusses and minuses in the formula, for later cross-referencing
        val plussesAndMinuses = arrayListOf<String>()
        plussesAndMinuses.add("+") //First value in formula must always be positive
        for (index in 0 until formula.length) {
            val currentCharacter = formula[index].toString()
            if (currentCharacter == "+" || currentCharacter == "-") { //If a character in the formula is + or -, append it to the list
                plussesAndMinuses.add(currentCharacter)
            }
        }


        val splitFormulaByPlusMinus = formula.trim().split("[+-]".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray() //Split formula based on + and -
        for (section in splitFormulaByPlusMinus) {
            val trimmedSection = section.trim()
            splitFormulaByD.add(trimmedSection.split("[dD]".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) //Add each trimmed section to the ArrayList, splitting it by d or D if applicable
        }

        for (index in splitFormulaByD.indices) {
            val splitRoll = splitFormulaByD[index]
            val positive = plussesAndMinuses[index] == "+" //Cross reference with plussesAndMinuses to determine if positive or negative

            if (splitRoll.size == 2) { //If the size is 2, it was delineated by d or D, and thus we know that the first value is the numberOfDice, and the second value is the dieSize
                val numberOfDice = splitRoll[0].trim().toInt()
                val dieSize = splitRoll[1].trim().toInt()
                val rolledValues = Utils.calculateDice(numberOfDice, dieSize) //Use the utils method to calculate a Pair with the individual values, and the total, and append/total these

                if (positive) { //Add or subtract accordingly
                    compiledRolls = compiledRolls.plus(" +${rolledValues.first}")
                    total += rolledValues.second!!
                } else { //Negative
                    compiledRolls = compiledRolls.plus(" -${rolledValues.first}")
                    total -= rolledValues.second!!
                }
            }

            if (splitRoll.size == 1) { //If the length is one, simply append the number and add or subtract accordingly
                val number = splitRoll[0].trim().toLong()
                if (positive) {
                    compiledRolls = compiledRolls.plus("+($number)")
                    total += number
                } else { //Negative
                    compiledRolls = compiledRolls.plus("-($number)")
                    total -= number
                }
            }
        }

        //Check if the roll is excessively large, or if the setting for showing details is off (assume it's on if not accessible)
        val showDetails = PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(mContext.getString(R.string.show_diceroll_details_key), true)
        val descrip = if (diceRoll.hasOverHundredDice || !showDetails) { //Simply append the formula and nothing else
            formula
        } else { //Create a description of the formula, along with the compiled rolls
            "$formula=\n\n$compiledRolls"
        }
        val name = diceRoll.name

        //Create a new DiceResults object and return
        return DiceResults(name = name, descrip = descrip, total = total)
    }


    override fun onPreExecute() {
        super.onPreExecute()
        mRollAsyncPreExecute.handleRollPreExecute()
    }

    override fun onPostExecute(diceResults: DiceResults) {
        mRollAsyncPostExecute.handleRollResult(diceResults)
    }

    interface RollAsyncPreExecute {
        fun handleRollPreExecute()
    }

    interface RollAsyncPostExecute {
        fun handleRollResult(diceResults: DiceResults)
    }
}
