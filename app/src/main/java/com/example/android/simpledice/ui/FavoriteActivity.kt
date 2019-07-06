package com.example.android.simpledice.ui

import android.content.Intent
import android.os.Bundle
import android.support.v4.view.MenuCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.view.Menu
import android.view.MenuItem
import com.example.android.simpledice.R
import com.example.android.simpledice.utils.*
import com.firebase.ui.auth.AuthUI
import com.google.android.gms.ads.AdRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.banner_ad.*
import kotlinx.android.synthetic.main.favorite_recycler_view.*
import kotlinx.android.synthetic.main.results.*
import java.util.*

class FavoriteActivity : AppCompatActivity(), FavoriteDiceRollAdapter.FavoriteDiceRollClickHandler,
    FavoriteDiceRollAdapter.DeleteDiceRollClickHandler, RollAsyncTask.RollAsyncPostExecute {

    private var mFavoriteDiceRollAdapter: FavoriteDiceRollAdapter? = null

    private var widgetIntentHandled = false

    private var mDiceRolls: MutableList<DiceRoll>? = null

    //Firebase
    private var mUserID: String? = null
    //Auth
    private var mFirebaseAuth: FirebaseAuth? = null
    private var mAuthStateListener: FirebaseAuth.AuthStateListener? = null
    //Database
    private var mFirebaseDatabase: FirebaseDatabase? = null
    private var mBaseDatabaseReference: DatabaseReference? = null
    private var mFavoriteChildEventListener: ChildEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorite)
        setTitle(R.string.favorites_title)

        setupAds()

        restoreSavedInstanceState(savedInstanceState)

        initializeFirebase()

        setupRecyclerView()

        add_favorite_fab.setOnClickListener {
            val intent = Intent(this@FavoriteActivity, AddFavoriteActivity::class.java)
            startActivity(intent)
        }

    }

    override fun onStart() {
        super.onStart()
        loadMostRecentDiceResults()
    }

    override fun onResume() {
        super.onResume()
        mFirebaseAuth!!.addAuthStateListener(mAuthStateListener!!)
        handleWidgetIntent()
    }

    override fun onPause() {
        super.onPause()
        if (mAuthStateListener != null) {
            mFirebaseAuth!!.removeAuthStateListener(mAuthStateListener!!)
        }
        detachDatabaseFavoritesReadListener()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(getString(R.string.widget_intent_handled_boolean_key), widgetIntentHandled)
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
     * Override of FavoriteDiceRoll click method
     *
     * @param favoriteDiceRoll the clicked DiceRoll to be used
     */
    override fun onItemClick(favoriteDiceRoll: DiceRoll) {
        RollAsyncTask(this).execute(favoriteDiceRoll)
    }

    override fun onDeleteClick(favoriteDiceRoll: DiceRoll) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.delete_dialog_title)
            .setMessage(R.string.delete_dialog_message)
            .setPositiveButton(R.string.delete_dialog_positive) { dialog, which ->
                favoriteDiceRoll.deleteDiceRoll(
                    mBaseDatabaseReference!!,
                    mUserID!!
                )
            }
            .setNegativeButton(R.string.delete_dialog_negative) { dialog, which -> dialog.cancel() }
            .show()
    }

    /**
     * Helper method to setup RecyclerView, should only be called once in onCreate
     */
    private fun setupRecyclerView() {
        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        favorite_rv.layoutManager = layoutManager

        mFavoriteDiceRollAdapter = FavoriteDiceRollAdapter(this, this)
        favorite_rv.adapter = mFavoriteDiceRollAdapter

        favorite_rv.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

        if (mDiceRolls == null) {
            mDiceRolls = ArrayList()
        }
        mFavoriteDiceRollAdapter!!.setFavoriteDiceRolls(mDiceRolls!!)
    }

    /**
     * A helper method to load the most recent dice results into the results views.
     * Called in onStart, as it is cheaper than using a listener, since any new rolls occuring while this is the foreground activity will be updated directly.
     */
    private fun loadMostRecentDiceResults() {
        val diceResults = Utils.retrieveLatestDiceResults(this)

        setDataToResultsViews(diceResults)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = this.menuInflater
        inflater.inflate(R.menu.favorite_activity_menu, menu)
        MenuCompat.setGroupDividerEnabled(menu, true)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_history -> {
                val intent = Intent(this@FavoriteActivity, HistoryActivity::class.java)
                startActivity(intent)
                return true
            }
            R.id.action_sign_out -> {
                val builder = AlertDialog.Builder(this@FavoriteActivity)
                builder.setTitle(R.string.sign_out_dialog_title)
                    .setMessage(R.string.sign_out_dialog_message)
                    .setPositiveButton(R.string.sign_out_dialog_positive) { dialog, which ->
                        AuthUI.getInstance().signOut(this@FavoriteActivity)
                    }
                    .setNegativeButton(R.string.sign_out_dialog_negative) { dialog, which -> dialog.cancel() }
                builder.show()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    /**
     * A helper method to handle all of the initial setup for Firebase, to be called in onCreate
     */
    private fun initializeFirebase() {
        mUserID = Constants.FIREBASE_ANONYMOUS
        //Auth setup
        mFirebaseAuth = FirebaseAuth.getInstance()

        mAuthStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) { //Signed in
                onSignedInInitialize(user.uid)
            } else { //Signed out, finish activity
                onSignedOutCleanup()
                finish()
            }
        }

        //Database Setup
        mFirebaseDatabase = FirebaseDatabase.getInstance()
        mBaseDatabaseReference = mFirebaseDatabase!!.reference
    }

    /**
     * A helper method for when signed into FirebaseAuth
     *
     * @param userID to set the Activity's member variable
     */
    private fun onSignedInInitialize(userID: String) {
        mUserID = userID
        attachDatabaseFavoritesReadListener()
    }

    /**
     * A helper method for when signed out of FirebaseAuth
     */
    private fun onSignedOutCleanup() {
        mUserID = Constants.FIREBASE_ANONYMOUS
        detachDatabaseFavoritesReadListener()
    }

    /**
     * A helper method for creating the database listener that checks Firebase for DiceRoll objects
     */
    private fun attachDatabaseFavoritesReadListener() {
        mDiceRolls = ArrayList() //Reset mDiceRolls, or edits to the Database cause repeat data
        if (mFavoriteChildEventListener == null) {
            mFavoriteChildEventListener = object : ChildEventListener {
                override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
                    val diceRoll = dataSnapshot.getValue(DiceRoll::class.java)
                    mDiceRolls!!.add(diceRoll!!)
                    mFavoriteDiceRollAdapter!!.setFavoriteDiceRolls(mDiceRolls!!)
                    Utils.updateAllWidgets(this@FavoriteActivity, mDiceRolls!!)
                }

                override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {

                }

                override fun onChildRemoved(dataSnapshot: DataSnapshot) {
                    val deletedDiceRoll = dataSnapshot.getValue(DiceRoll::class.java)
                    for (diceRoll in mDiceRolls!!) {
                        if (deletedDiceRoll!!.name == diceRoll.name && deletedDiceRoll.formula == diceRoll.formula) { //If the name and formula match, delete it
                            mDiceRolls!!.remove(diceRoll)
                            mFavoriteDiceRollAdapter!!.setFavoriteDiceRolls(mDiceRolls!!)
                            Utils.updateAllWidgets(this@FavoriteActivity, mDiceRolls!!)
                            break //Prevent removing more than one diceRoll
                        }
                    }
                }

                override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {

                }

                override fun onCancelled(databaseError: DatabaseError) {

                }
            }
            mBaseDatabaseReference!!.child(Constants.FIREBASE_DATABASE_FAVORITES_PATH).child(mUserID!!)
                .addChildEventListener(mFavoriteChildEventListener!!)
        }
    }

    /**
     * A helper method to clear the database listener
     */
    private fun detachDatabaseFavoritesReadListener() {
        if (mFavoriteChildEventListener != null) {
            mBaseDatabaseReference!!.child(Constants.FIREBASE_DATABASE_FAVORITES_PATH).child(mUserID!!)
                .removeEventListener(mFavoriteChildEventListener!!)
            mFavoriteChildEventListener = null
        }
    }

    /**
     * A helper method to handle Intents coming from the widget that contain a DiceRoll to be rolled
     * Called in onCreate.
     */
    private fun handleWidgetIntent() {
        if (!widgetIntentHandled) {
            val intent = intent
            val parcelableKey = getString(R.string.widget_favorites_intent_parcelable_key)
            if (intent.hasExtra(parcelableKey)) { //If available, retrieve DiceRoll
                val diceRoll = intent.getParcelableExtra<DiceRoll>(parcelableKey)
                RollAsyncTask(this).execute(diceRoll)
            }
            widgetIntentHandled = true
        }
    }

    /** Override, for any given diceResults that occur from rolling a DiceRoll, up
     *
     * @param diceResults to use
     */
    override fun handleRollResult(diceResults: DiceResults) {
        diceResults.saveToSharedPreferences(this)
        setDataToResultsViews(diceResults)
        diceResults.saveToFirebaseHistory(mBaseDatabaseReference!!, mUserID!!)
    }

    /** A helper method for handling saved instance states in onCreate
     *
     * @param savedInstanceState to handle
     */
    private fun restoreSavedInstanceState(savedInstanceState: Bundle?) {
        if (savedInstanceState != null && !savedInstanceState.isEmpty) {
            widgetIntentHandled =
                savedInstanceState.getBoolean(getString(R.string.widget_intent_handled_boolean_key), false)
        }
    }

    /**
     * A helper method to setup an ad into the activity's AdView
     */
    private fun setupAds() {
        val adRequest = AdRequest.Builder().build()
        banner_ad!!.loadAd(adRequest)
    }
}
