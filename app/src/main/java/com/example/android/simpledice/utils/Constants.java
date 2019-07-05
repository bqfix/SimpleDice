package com.example.android.simpledice.utils;

public final class Constants {
    public static final String FIREBASE_DATABASE_HISTORY_PATH = "history";
    public static final String FIREBASE_DATABASE_FAVORITES_PATH = "favorites";
    public static final String FIREBASE_DATABASE_FAVORITES_NAME_PATH = "name";
    public static final String FIREBASE_DATABASE_FAVORITES_FORMULA_PATH = "formula";
    public static final String FIREBASE_DATABASE_FAVORITES_OVER_HUNDRED_PATH = "hasOverHundredDice";
    public static final int REQUEST_CODE_SIGN_IN = 1;
    public final static int MAX_DICE_PER_ROLL = 100000;
    public final static int MAX_DIE_SIZE = 1000000;
    public static final String FIREBASE_ANONYMOUS = "anonymous";
    public static final String NAME_FORMULA_HUNDRED_BREAK = "::";
    public static final String DICEROLL_BREAK = "%%";
    public static final String OVER_HUNDRED_DICE_DESCRIP = "No details available for rolls with more than 100 dice.";
}
