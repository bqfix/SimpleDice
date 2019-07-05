package com.example.android.simpledice.utils;

import android.os.AsyncTask;
import android.support.v4.util.Pair;

import java.util.ArrayList;

public class RollAsyncTask extends AsyncTask<DiceRoll, Void, DiceResults> {
    private RollAsyncPostExecute mRollAsyncPostExecute;

    public RollAsyncTask(RollAsyncPostExecute rollAsyncPostExecute) {
        mRollAsyncPostExecute = rollAsyncPostExecute;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected DiceResults doInBackground(DiceRoll... params) {
        DiceRoll diceRoll = params[0];
        String formula = diceRoll.getFormula();
        StringBuilder compiledRolls = new StringBuilder("");
        long total = 0;
        ArrayList<String[]> splitFormulaByD = new ArrayList<>();

        //Create an ArrayList containing all the plusses and minuses in the formula, for later cross-referencing
        ArrayList<String> plussesAndMinuses = new ArrayList<>();
        plussesAndMinuses.add("+"); //First value in formula must always be positive
        for (int index = 0; index < formula.length(); index++) {
            String currentCharacter = String.valueOf(formula.charAt(index));
            if (currentCharacter.equals("+") || currentCharacter.equals("-")) { //If a character in the formula is + or -, append it to the list
                plussesAndMinuses.add(currentCharacter);
            }
        }


        String[] splitFormulaByPlusMinus = formula.trim().split("[+-]"); //Split formula based on + and -
        for (String section : splitFormulaByPlusMinus) {
            String trimmedSection = section.trim();
            splitFormulaByD.add(trimmedSection.split("[dD]")); //Add each trimmed section to the ArrayList, splitting it by d or D if applicable
        }

        for (int index = 0; index < splitFormulaByD.size(); index++) {
            String[] splitRoll = splitFormulaByD.get(index);
            boolean positive = plussesAndMinuses.get(index).equals("+"); //Cross reference with plussesAndMinuses to determine if positive or negative

            if (splitRoll.length == 2) { //If the size is 2, it was delineated by d or D, and thus we know that the first value is the numberOfDice, and the second value is the dieSize
                int numberOfDice = Integer.parseInt(splitRoll[0].trim());
                int dieSize = Integer.parseInt(splitRoll[1].trim());
                Pair<String, Long> rolledValues = Utils.calculateDice(numberOfDice, dieSize); //Use the utils method to calculate a Pair with the individual values, and the total, and append/total these

                if (positive) { //Add or subtract accordingly
                    compiledRolls.append(" +").append(rolledValues.first);
                    total += rolledValues.second;
                } else { //Negative
                    compiledRolls.append(" -").append(rolledValues.first);
                    total -= rolledValues.second;
                }
            }

            if (splitRoll.length == 1) { //If the length is one, simply append the number and add or subtract accordingly
                String number = splitRoll[0].trim();
                if (positive) {
                    compiledRolls.append("+(").append(number).append(")");
                    total += Integer.parseInt(number);
                } else { //Negative
                    compiledRolls.append("-(").append(number).append(")");
                    total -= Integer.parseInt(number);
                }
            }
        }

        //Create a description of the formula, along with the compiled rolls
        String descrip;
        if (diceRoll.getHasOverHundredDice()){
            descrip = Constants.OVER_HUNDRED_DICE_DESCRIP;
        } else {
            descrip = formula + "=\n\n" + compiledRolls.toString();
        }
        String name = diceRoll.getName();

        //Create a new DiceResults object and return
        return new DiceResults(name, descrip, total);
    }


    @Override
    protected void onPostExecute(DiceResults diceResults) {
        mRollAsyncPostExecute.handleRollResult(diceResults);
    }

    public interface RollAsyncPostExecute {
        void handleRollResult(DiceResults diceResults);
    }
}
