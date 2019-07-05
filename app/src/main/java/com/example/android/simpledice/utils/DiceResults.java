package com.example.android.simpledice.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.android.simpledice.R;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Exclude;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

public class DiceResults {
    private String mName;
    private String mDescrip;
    private long mTotal;
    private long mDateCreated;

    public DiceResults(){} //No argument constructor for deserializing from Firebase

    public DiceResults(String name, String descrip, long total) {
        mName = name;
        mDescrip = descrip;
        mTotal = total;
        mDateCreated = System.currentTimeMillis();
    }

    public DiceResults(String name, String descrip, long total, long date){
        mName = name;
        mDescrip = descrip;
        mTotal = total;
        mDateCreated = date;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public String getDescrip() {
        return mDescrip;
    }

    public void setDescrip(String descrip) {
        mDescrip = descrip;
    }

    public long getTotal() {
        return mTotal;
    }

    public void setTotal(long total) {
        mTotal = total;
    }

    public long getDateCreated(){
        return mDateCreated;
    }

    public void setDateCreated(long dateCreated){mDateCreated = dateCreated;}

    @Exclude
    public String getFormattedDateCreated(){
        DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault());
        Date date = new Date(mDateCreated);
        return dateFormat.format(date);
    }

    /** A helper method used to save the Results to both SharedPreferences (as the latest roll) and History
     *
     * @param context used to save the results
     */
    public void saveToSharedPreferences(Context context){
        //Logic to add to SharedPrefs
        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.shared_preferences_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString(context.getString(R.string.dice_results_name_key), mName);
        editor.putString(context.getString(R.string.dice_results_descrip_key), mDescrip);
        editor.putLong(context.getString(R.string.dice_results_total_key), mTotal);
        editor.putLong(context.getString(R.string.dice_results_date_key), mDateCreated);
        editor.apply();
    }

    /**
     * A helper method for saving to the Firebase Realtime Database's history section
     * @param databaseReference to be saved to
     * @param userId to save under
     */
    public void saveToFirebaseHistory(DatabaseReference databaseReference, String userId){
        databaseReference.child(Constants.FIREBASE_DATABASE_HISTORY_PATH).child(userId).push().setValue(DiceResults.this);
    }
}
