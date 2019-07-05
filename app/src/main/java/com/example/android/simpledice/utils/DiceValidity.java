package com.example.android.simpledice.utils;

public class DiceValidity {
    private boolean isValid;
    private String errorMessage;
    private boolean hasOverHundredDice;

    public DiceValidity(boolean isValid, String errorMessage, boolean hasOverHundredDice) {
        this.isValid = isValid;
        this.errorMessage = errorMessage;
        this.hasOverHundredDice = hasOverHundredDice;
    }

    public boolean isValid() {
        return isValid;
    }

    public void setValid(boolean valid) {
        isValid = valid;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public boolean hasOverHundredDice() {
        return hasOverHundredDice;
    }

    public void setHasOverHundredDice(boolean hasOverHundredDice) {
        this.hasOverHundredDice = hasOverHundredDice;
    }
}
