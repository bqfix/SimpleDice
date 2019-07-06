package com.example.android.simpledice.utils

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.support.v4.util.Pair
import com.example.android.simpledice.R
import com.example.android.simpledice.widget.FavoritesWidget
import java.util.*

object Utils {

    /** Temporary helper method to provide a list of fake data to test RecyclerViews, etc.
     *
     * @return a List of DiceRolls
     */
    val diceRollFakeData: ArrayList<DiceRoll>
        get() {
            val diceRolls = ArrayList<DiceRoll>()
            for (i in 0..49) {
                diceRolls.add(DiceRoll("Standard Die Plus One", "1d6 + 1", false))
            }
            return diceRolls
        }

    /** Temporary helper method to provide a list of fake DiceResults data to test RecyclerViews, etc.
     *
     * @return a List of DiceResults
     */
    val diceResultsFakeData: ArrayList<DiceResults>
        get() {
            val diceResults = ArrayList<DiceResults>()
            for (i in 0..49) {
                diceResults.add(DiceResults("Standard Roll", "1d6 + 1 = +(6) + (1)", 7))
            }
            return diceResults
        }

    /** A helper method to return randomized rolls given a number of dice and their value
     *
     * @param numberOfDice
     * @param dieSize
     * @return a Pair including a String that details each roll, and an Integer that holds the total
     */
    fun calculateDice(numberOfDice: Int, dieSize: Int): Pair<String, Long> {
        val randomizer = Random()
        var compiledRolls = ""
        var total: Long = 0

        if (numberOfDice == 0 || dieSize == 0)
            return Pair(compiledRolls, total) //Return early if answer will be 0

        val firstRoll = randomizer.nextInt(dieSize) + 1  //Add and append the first value, and add to total
        compiledRolls = compiledRolls.plus("($firstRoll")
        total += firstRoll.toLong()

        for (currentDieNumber in 2..numberOfDice) { //Iterate through a number of times equal to the remaining dice, repeating the append and adding to total
            val roll = randomizer.nextInt(dieSize) + 1
            compiledRolls = compiledRolls.plus(" + $roll")
            total += roll.toLong()
        }

        compiledRolls = compiledRolls.plus(")") //Append closing bracket to compiledRolls and return
        return Pair(compiledRolls, total)
    }

    /** A helper method that checks if a given formula will be valid, should be called before creating or editing DiceRolls' formulas
     *
     * @param context used to access string resources
     * @param formula to be checked
     * @return a Pair containing a Boolean of whether the formula is valid or not, and an error message to be used if it is not.
     */
    fun isValidDiceRoll(context: Context, formula: String): DiceValidity {
        if (!formula.matches("[0123456789dD +-]+".toRegex())) { //Check that the formula contains exclusively numbers, d, D, or +/-
            return DiceValidity(false, context.getString(R.string.invalid_characters), false)
        }

        var totalDice : Long = 0
        val splitFormulaByPlusMinus = formula.trim { it <= ' ' }.split("[+-]".toRegex())
            .toTypedArray() //Split the formula by + and -, -1 limit forces inclusion of empty sections from excessive + or -
        for (splitSection in splitFormulaByPlusMinus) {
            val splitFormulaByD = splitSection.trim { it <= ' ' }.split("[dD]".toRegex())
                .toTypedArray() //Further split each section by whether there is a d or D, -1 limit provided to force inclusion of empty strings for subsequent length parsing (in the event of multiple ds)
            when (splitFormulaByD.size) {
                //Each section should only be 2 numbers long (meaning the numbers can be multiplied) or 1 number long
                2 -> {
                    for (potentialNumber in splitFormulaByD) { //Confirm that each number is valid
                        try {
                            val number = potentialNumber.trim().toLong()
                            if (number > Constants.MAX_DIE_SIZE) {
                                return DiceValidity(false, context.getString(R.string.incorrectly_formatted_section), false)
                            }
                        } catch (e: NumberFormatException) {
                            return DiceValidity(false, context.getString(R.string.incorrectly_formatted_section), false)
                        }

                    }
                    totalDice += splitFormulaByD[0].trim().toLong()  //If both numbers were valid, add the first number (which will be the number of dice) to totalDice
                }
                1 -> {
                    try { //Confirm that the number is valid
                        val number = splitFormulaByD[0].trim().toLong()
                        if (number > Constants.MAX_DIE_SIZE) {
                            return DiceValidity(false, context.getString(R.string.incorrectly_formatted_section), false)
                        }
                    } catch (e: NumberFormatException) {
                        return DiceValidity(false, context.getString(R.string.incorrectly_formatted_section), false)
                    }

                }
                else -> return DiceValidity(false, context.getString(R.string.incorrectly_formatted_section), false)
            }
        }
        if (totalDice > Constants.MAX_DICE_PER_ROLL) { //This is to prevent exceptionally large rolls that may lock down the app
            return DiceValidity(false, context.getString(R.string.too_many_dice), false)
        }
        val overHundred = totalDice > 100
        return DiceValidity(true, context.getString(R.string.no_error), overHundred)
    }

    /** A helper method to read SharedPrefs and return the latest DiceResults from it
     *
     * @param context used to access SharedPrefs
     * @return the most recent DiceResults
     */
    fun retrieveLatestDiceResults(context: Context): DiceResults {
        val sharedPreferences =
            context.getSharedPreferences(context.getString(R.string.shared_preferences_key), Context.MODE_PRIVATE)

        val name = sharedPreferences.getString(context.getString(R.string.dice_results_name_key), "")
        val descrip = sharedPreferences.getString(context.getString(R.string.dice_results_descrip_key), "")
        val total = sharedPreferences.getLong(context.getString(R.string.dice_results_total_key), 0)
        val date = sharedPreferences.getLong(context.getString(R.string.dice_results_date_key), 0)

        return DiceResults(name!!, descrip!!, total, date)
    }

    /** A helper method to update all widgets when favorited DiceRoll data changes.  Generally called when updates to Favorites-based recycler views are called.
     *
     * @param context used for accessing the resources needed to call the update widgets method
     * @param diceRolls to update the widgets with
     */
    fun updateAllWidgets(context: Context, diceRolls: List<DiceRoll>) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, FavoritesWidget::class.java))
        FavoritesWidget.updateAppWidget(context, appWidgetManager, appWidgetIds, diceRolls)
    }

    /**
     *  A method to convert a List of DiceRolls into a string for passing to RemoteViewFactory
     */
    fun diceRollsToString(diceRolls: List<DiceRoll>): String {
        val builder = StringBuilder()
        for (position in diceRolls.indices) {
            val diceRoll = diceRolls[position]
            builder.append(diceRoll.name)
                .append(Constants.NAME_FORMULA_HUNDRED_BREAK)
                .append(diceRoll.formula)
                .append(Constants.NAME_FORMULA_HUNDRED_BREAK)
                .append(diceRoll.hasOverHundredDice) //Append name, break, and formula for each DiceRoll
            if (position != diceRolls.size - 1) { //If not last in list, additionally append diceRoll break
                builder.append(Constants.DICEROLL_BREAK)
            }
        }
        return builder.toString()
    }

    /**
     * A method to fetch a List of DiceRolls from a string in RemoteViewFactory
     */
    fun stringToDiceRolls(string: String): List<DiceRoll> {
        val diceRolls = ArrayList<DiceRoll>()
        val splitByDiceRolls = string.split(Constants.DICEROLL_BREAK.toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray() //Split into dicerolls
        for (splitString in splitByDiceRolls) {
            val splitByNameAndFormula =
                splitString.split(Constants.NAME_FORMULA_HUNDRED_BREAK.toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray() //Split into usable data
            if (splitByNameAndFormula.size == 3) {
                diceRolls.add(
                    DiceRoll(
                        splitByNameAndFormula[0],
                        splitByNameAndFormula[1],
                        java.lang.Boolean.valueOf(splitByNameAndFormula[2])
                    )
                )
            }
        }
        return diceRolls
    }
}
