package com.example.android.simpledice.utils

object Constants {
    val FIREBASE_DATABASE_HISTORY_PATH = "history"
    val FIREBASE_DATABASE_FAVORITES_PATH = "favorites"
    val FIREBASE_DATABASE_FAVORITES_NAME_PATH = "name"
    val FIREBASE_DATABASE_FAVORITES_FORMULA_PATH = "formula"
    val FIREBASE_DATABASE_FAVORITES_OVER_HUNDRED_PATH = "hasOverHundredDice"
    val REQUEST_CODE_SIGN_IN = 1
    val MAX_DICE_PER_ROLL = 100000
    val MAX_DIE_SIZE = 1000000
    val FIREBASE_ANONYMOUS = "anonymous"
    val NAME_FORMULA_HUNDRED_BREAK = "::"
    val DICEROLL_BREAK = "%%"
    val OVER_HUNDRED_DICE_DESCRIP = "No details available for rolls with more than 100 dice."
}
