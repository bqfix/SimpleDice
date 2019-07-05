package com.example.android.simpledice.utils;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class DiceRoll implements Parcelable {

    private String mName;
    private String mFormula;
    private boolean hasOverHundredDice;

    public DiceRoll(){} //No argument constructor for deserializing from Firebase

    public DiceRoll(String formula, boolean hasOverHundredDice) {
        mName = "";
        mFormula = formula;
        this.hasOverHundredDice = hasOverHundredDice;
    }

    public DiceRoll(String name, String formula, boolean hasOverHundredDice) {
        mName = name;
        mFormula = formula;
        this.hasOverHundredDice = hasOverHundredDice;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public String getFormula() {
        return mFormula;
    }

    public void setFormula(String formula) {
        mFormula = formula;
    }

    public boolean getHasOverHundredDice() { //Must be named (get) for Firebase
        return hasOverHundredDice;
    }

    public void setHasOverHundredDice(boolean hasOverHundredDice) {
        this.hasOverHundredDice = hasOverHundredDice;
    }

    /**
     * A helper method to save a DiceRoll to Firebase Realtime Database's Favorites section
     * @param databaseReference to save to
     * @param userID to save under
     */
    public void saveNewToFirebaseFavorites(DatabaseReference databaseReference, String userID){
        databaseReference.child(Constants.FIREBASE_DATABASE_FAVORITES_PATH).child(userID).push().setValue(DiceRoll.this);
    }

    /**
     * A helper method to edit a DiceRoll in the Firebase Realtime Database's Favorites section
     */
    public void editSavedFirebaseFavorite(DatabaseReference databaseReference, String userID, final String previousName, final String previousFormula){
        DatabaseReference userFavorites = databaseReference.child(Constants.FIREBASE_DATABASE_FAVORITES_PATH).child(userID);
        Query queryRef = userFavorites.orderByChild(Constants.FIREBASE_DATABASE_FAVORITES_FORMULA_PATH).equalTo(previousFormula); //Query the database for first entry that has matching formula
        queryRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot diceRollSnapshot : dataSnapshot.getChildren()) { //Iterate through all returned values
                    String name = diceRollSnapshot.child(Constants.FIREBASE_DATABASE_FAVORITES_NAME_PATH).getValue().toString(); //Check the name of each
                    if (name.equals(previousName)) {//For the first result where name and formula match, edit
                        DatabaseReference diceRollRef = diceRollSnapshot.getRef();
                        diceRollRef.child(Constants.FIREBASE_DATABASE_FAVORITES_NAME_PATH).setValue(mName);
                        diceRollRef.child(Constants.FIREBASE_DATABASE_FAVORITES_FORMULA_PATH).setValue(mFormula);
                        diceRollRef.child(Constants.FIREBASE_DATABASE_FAVORITES_OVER_HUNDRED_PATH).setValue(hasOverHundredDice);
                        break; //To prevent further edits in the event of multiple matching entries
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    /**
     * A helper method to delete this DiceRoll from Firebase Realtime Database's Favorites section
     */
    public void deleteDiceRoll(DatabaseReference databaseReference, String userID){
        DatabaseReference userFavorites = databaseReference.child(Constants.FIREBASE_DATABASE_FAVORITES_PATH).child(userID);
        Query queryRef = userFavorites.orderByChild(Constants.FIREBASE_DATABASE_FAVORITES_FORMULA_PATH).equalTo(mFormula); //Query the database for first entry that has matching formula
        queryRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot diceRollSnapshot : dataSnapshot.getChildren()) { //Iterate through all returned values
                    String name = diceRollSnapshot.child(Constants.FIREBASE_DATABASE_FAVORITES_NAME_PATH).getValue().toString(); //Check the name of each
                    if (name.equals(mName)) {//For the first result where name and formula match, edit
                        DatabaseReference diceRollRef = diceRollSnapshot.getRef();
                        diceRollRef.removeValue();
                        break; //To prevent further edits in the event of multiple matching entries
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.mName);
        dest.writeString(this.mFormula);
        dest.writeByte(this.hasOverHundredDice ? (byte) 1 : (byte) 0);
    }

    protected DiceRoll(Parcel in) {
        this.mName = in.readString();
        this.mFormula = in.readString();
        this.hasOverHundredDice = in.readByte() != 0;
    }

    public static final Creator<DiceRoll> CREATOR = new Creator<DiceRoll>() {
        @Override
        public DiceRoll createFromParcel(Parcel source) {
            return new DiceRoll(source);
        }

        @Override
        public DiceRoll[] newArray(int size) {
            return new DiceRoll[size];
        }
    };
}
