package com.example.android.simpledice.ui

import android.os.Bundle
import android.support.v4.view.MenuCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import com.example.android.simpledice.R
import com.example.android.simpledice.utils.Constants
import com.example.android.simpledice.utils.DiceResults
import com.firebase.ui.auth.AuthUI
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_history.*
import kotlinx.android.synthetic.main.banner_ad.*
import kotlinx.android.synthetic.main.results.*
import java.util.*

class HistoryActivity : AppCompatActivity(), HistoryResultsAdapter.HistoryResultsClickHandler {

    private var mHistoryAdapter: HistoryResultsAdapter? = null

    private var mDiceResults: MutableList<DiceResults>? = null

    //Firebase
    private var mUserID: String? = null
    private var mFirebaseAuth: FirebaseAuth? = null
    private var mAuthStateListener: FirebaseAuth.AuthStateListener? = null
    private var mFirebaseDatabase: FirebaseDatabase? = null
    private var mBaseDatabaseReference: DatabaseReference? = null
    private var mHistoryChildEventListener: ChildEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        setTitle(R.string.history_activity_title)

        initializeFirebase()

        setupAds()

        setupHistoryRecyclerView()

    }

    override fun onResume() {
        super.onResume()
        mFirebaseAuth!!.addAuthStateListener(mAuthStateListener!!)
    }

    override fun onPause() {
        super.onPause()
        if (mAuthStateListener != null) {
            mFirebaseAuth!!.removeAuthStateListener(mAuthStateListener!!)
        }
        detachDatabaseHistoryReadListener()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.history_activity_menu, menu)
        MenuCompat.setGroupDividerEnabled(menu, true)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_sign_out -> {
                val signoutBuilder = AlertDialog.Builder(this@HistoryActivity)
                signoutBuilder.setTitle(R.string.sign_out_dialog_title)
                    .setMessage(R.string.sign_out_dialog_message)
                    .setPositiveButton(R.string.sign_out_dialog_positive) { dialog, which ->
                        AuthUI.getInstance().signOut(this@HistoryActivity)
                    }
                    .setNegativeButton(R.string.sign_out_dialog_negative) { dialog, which -> dialog.cancel() }
                signoutBuilder.show()
                return true
            }
            R.id.action_delete_all_history -> {
                val deleteHistoryBuilder = AlertDialog.Builder(this@HistoryActivity)
                deleteHistoryBuilder.setTitle(R.string.delete_all_dialog_title)
                    .setMessage(R.string.delete_all_dialog_message)
                    .setPositiveButton(R.string.delete_all_dialog_positive) { dialog, which -> clearAllHistory() }
                    .setNegativeButton(R.string.delete_all_dialog_negative) { dialog, which -> dialog.cancel() }
                deleteHistoryBuilder.show()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onItemClick(historyResults: DiceResults) {
        setDataToResultsViews(historyResults)
    }


    /**
     * A helper method to populate the results views with data
     *
     * @param diceResults to populate the views from
     */
    private fun setDataToResultsViews(diceResults: DiceResults) {
        results_name_tv.text = diceResults.name
        results_total_tv.text = diceResults.total.toString()
        results_descrip_tv.text = diceResults.descrip
    }

    /**
     * Helper method to setup HistoryRecyclerView, should only be called once in onCreate
     */
    private fun setupHistoryRecyclerView() {
        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        history_rv.layoutManager = layoutManager

        mHistoryAdapter = HistoryResultsAdapter(this)
        history_rv.adapter = mHistoryAdapter

        history_rv.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

        if (mDiceResults == null) {
            mDiceResults = ArrayList()
        }
        mHistoryAdapter!!.setHistoryResults(mDiceResults!!)
    }

    /**
     * A helper method to handle all of the initial setup for Firebase, to be called in onCreate
     */
    private fun initializeFirebase() {
        mUserID = Constants.FIREBASE_ANONYMOUS
        //Auth setup
        mFirebaseAuth = FirebaseAuth.getInstance()

        //Database
        mFirebaseDatabase = FirebaseDatabase.getInstance()
        mBaseDatabaseReference = mFirebaseDatabase!!.reference

        mAuthStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) { //Signed in
                onSignedInInitialize(user.uid)
            } else { //Signed out, finish activity
                onSignedOutCleanup()
                finish()
            }
        }
    }

    /**
     * A helper method for when signed into FirebaseAuth
     *
     * @param userID to set the Activity's member variable
     */
    private fun onSignedInInitialize(userID: String) {
        mUserID = userID
        attachDatabaseHistoryReadListener()
    }

    /**
     * A helper method for when signed out of FirebaseAuth
     */
    private fun onSignedOutCleanup() {
        mUserID = Constants.FIREBASE_ANONYMOUS
        detachDatabaseHistoryReadListener()
    }

    /**
     * A helper method for creating the database listener that checks Firebase for DiceResults objects //TODO change to single value event listener, add onSavedInstance?
     */
    private fun attachDatabaseHistoryReadListener() {
        if (mHistoryChildEventListener == null) {
            mHistoryChildEventListener = object : ChildEventListener {
                override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
                    val diceResults = dataSnapshot.getValue(DiceResults::class.java)
                    mDiceResults!!.add(diceResults!!)
                    mHistoryAdapter!!.setHistoryResults(mDiceResults!!)
                }

                override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {

                }

                override fun onChildRemoved(dataSnapshot: DataSnapshot) {

                }

                override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {

                }

                override fun onCancelled(databaseError: DatabaseError) {

                }
            }
            mBaseDatabaseReference!!.child(Constants.FIREBASE_DATABASE_HISTORY_PATH).child(mUserID!!)
                .addChildEventListener(mHistoryChildEventListener!!)
        }
    }

    /**
     * A helper method to clear the database listener
     */
    private fun detachDatabaseHistoryReadListener() {
        if (mHistoryChildEventListener != null) {
            mBaseDatabaseReference!!.child(Constants.FIREBASE_DATABASE_HISTORY_PATH).child(mUserID!!)
                .removeEventListener(mHistoryChildEventListener!!)
            mHistoryChildEventListener = null
        }
    }

    /**
     * A helper method that deletes all the user's history from Firebase, and empties all views on HistoryActivity
     */
    private fun clearAllHistory() {
        mBaseDatabaseReference!!.child(Constants.FIREBASE_DATABASE_HISTORY_PATH).child(mUserID!!)
            .setValue(null) //Delete from database

        //Reset stored values and update RecyclerView to empty
        mDiceResults = ArrayList()
        mHistoryAdapter!!.setHistoryResults(mDiceResults!!)

        //Empty TextViews
        results_name_tv.text = ""
        results_descrip_tv.text = ""
        results_total_tv.text = ""
    }

    /**
     * A helper method to setup an ad into the activity's AdView
     */
    private fun setupAds() {
        val adRequest = AdRequest.Builder().build()
        banner_ad.loadAd(adRequest)
    }
}