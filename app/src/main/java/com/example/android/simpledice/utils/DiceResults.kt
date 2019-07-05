package com.example.android.simpledice.utils

import android.content.Context
import android.content.SharedPreferences
import android.os.Parcelable

import com.example.android.simpledice.R
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.Exclude
import kotlinx.android.parcel.Parcelize

import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Parcelize
class DiceResults(
    var name: String = "",
    var descrip: String = "",
    var total: Long = 0,
    var dateCreated: Long = System.currentTimeMillis()
) : Parcelable {
    val formattedDateCreated: String
        @Exclude
        get() {
            val dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault())
            val date = Date(dateCreated)
            return dateFormat.format(date)
        }


    /** A helper method used to save the Results to both SharedPreferences (as the latest roll) and History
     *
     * @param context used to save the results
     */
    fun saveToSharedPreferences(context: Context) {
        //Logic to add to SharedPrefs
        val sharedPreferences =
            context.getSharedPreferences(context.getString(R.string.shared_preferences_key), Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        editor.putString(context.getString(R.string.dice_results_name_key), name)
        editor.putString(context.getString(R.string.dice_results_descrip_key), descrip)
        editor.putLong(context.getString(R.string.dice_results_total_key), total)
        editor.putLong(context.getString(R.string.dice_results_date_key), dateCreated)
        editor.apply()
    }

    /**
     * A helper method for saving to the Firebase Realtime Database's history section
     * @param databaseReference to be saved to
     * @param userId to save under
     */
    fun saveToFirebaseHistory(databaseReference: DatabaseReference, userId: String) {
        databaseReference.child(Constants.FIREBASE_DATABASE_HISTORY_PATH).child(userId).push()
            .setValue(this@DiceResults)
    }
}
