package com.example.android.simpledice.utils

import android.os.Parcelable

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import kotlinx.android.parcel.Parcelize

@Parcelize
class DiceRoll(
    var name: String = "",
    var formula: String,
    var hasOverHundredDice: Boolean = false
) : Parcelable {

    /**
     * A helper method to save a DiceRoll to Firebase Realtime Database's Favorites section
     * @param databaseReference to save to
     * @param userID to save under
     */
    fun saveNewToFirebaseFavorites(databaseReference: DatabaseReference, userID: String) {
        databaseReference.child(Constants.FIREBASE_DATABASE_FAVORITES_PATH).child(userID).push().setValue(this@DiceRoll)
    }

    /**
     * A helper method to edit a DiceRoll in the Firebase Realtime Database's Favorites section
     */
    fun editSavedFirebaseFavorite(
        databaseReference: DatabaseReference,
        userID: String,
        previousName: String,
        previousFormula: String
    ) {
        val userFavorites = databaseReference.child(Constants.FIREBASE_DATABASE_FAVORITES_PATH).child(userID)
        val queryRef = userFavorites.orderByChild(Constants.FIREBASE_DATABASE_FAVORITES_FORMULA_PATH)
            .equalTo(previousFormula) //Query the database for first entry that has matching formula
        queryRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (diceRollSnapshot in dataSnapshot.children) { //Iterate through all returned values
                    val nameInDatabase =
                        diceRollSnapshot.child(Constants.FIREBASE_DATABASE_FAVORITES_NAME_PATH).value!!.toString() //Check the name of each
                    if (nameInDatabase == previousName) {//For the first result where name and formula match, edit
                        val diceRollRef = diceRollSnapshot.ref
                        diceRollRef.child(Constants.FIREBASE_DATABASE_FAVORITES_NAME_PATH).setValue(name)
                        diceRollRef.child(Constants.FIREBASE_DATABASE_FAVORITES_FORMULA_PATH).setValue(formula)
                        diceRollRef.child(Constants.FIREBASE_DATABASE_FAVORITES_OVER_HUNDRED_PATH)
                            .setValue(hasOverHundredDice)
                        break //To prevent further edits in the event of multiple matching entries
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {

            }
        })
    }

    /**
     * A helper method to delete this DiceRoll from Firebase Realtime Database's Favorites section
     */
    fun deleteDiceRoll(databaseReference: DatabaseReference, userID: String) {
        val userFavorites = databaseReference.child(Constants.FIREBASE_DATABASE_FAVORITES_PATH).child(userID)
        val queryRef = userFavorites.orderByChild(Constants.FIREBASE_DATABASE_FAVORITES_FORMULA_PATH)
            .equalTo(formula) //Query the database for first entry that has matching formula
        queryRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (diceRollSnapshot in dataSnapshot.children) { //Iterate through all returned values
                    val nameInDatabase =
                        diceRollSnapshot.child(Constants.FIREBASE_DATABASE_FAVORITES_NAME_PATH).value!!.toString() //Check the name of each
                    if (nameInDatabase == name) {//For the first result where name and formula match, edit
                        val diceRollRef = diceRollSnapshot.ref
                        diceRollRef.removeValue()
                        break //To prevent further edits in the event of multiple matching entries
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {

            }
        })
    }

}
