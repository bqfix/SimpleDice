package com.example.android.simpledice.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v4.view.MenuCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.example.android.simpledice.R
import com.example.android.simpledice.utils.*
import com.firebase.ui.auth.AuthUI
import com.google.android.gms.ads.AdRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.banner_ad.*
import kotlinx.android.synthetic.main.main_edit_favorite.*
import kotlinx.android.synthetic.main.results.*
import java.util.*

class MainActivity : AppCompatActivity(), FavoriteDiceRollAdapter.FavoriteDiceRollClickHandler,
    FavoriteDiceRollAdapter.DeleteDiceRollClickHandler, RollAsyncTask.RollAsyncPostExecute {

    private var mFavoriteDiceRollAdapter: FavoriteDiceRollAdapter? = null
    private var mDiceRolls: MutableList<DiceRoll>? = null

    //InputConnection for custom keyboard
    private var mCommandInputConnection: InputConnection? = null

    //Firebase variables
    private var mUserID: String? = null
    //Realtime Database
    private var mFirebaseDatabase: FirebaseDatabase? = null
    private var mBaseDatabaseReference: DatabaseReference? = null
    private var mFavoriteChildEventListener: ChildEventListener? = null
    //Auth
    private var mFirebaseAuth: FirebaseAuth? = null
    private var mAuthStateListener: FirebaseAuth.AuthStateListener? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeFirebase()

        setupAds()

        setOnClickListeners()

        setupFavoriteRecyclerView()

        setupEditTextAndKeyboard()

    }

    override fun onStart() {
        super.onStart()
        loadMostRecentDiceResults()
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
        detachDatabaseFavoritesReadListener()
    }


    /**
     * A helper method to populate the results views with data
     *
     * @param diceResults to populate the views from
     */
    private fun setDataToResultsViews(diceResults: DiceResults) {
        results_name_tv!!.text = diceResults.name
        results_total_tv!!.text = diceResults.total.toString()
        results_descrip_tv!!.text = diceResults.descrip
    }

    /**
     * A helper method to hide the system keyboard
     *
     * @param view used to get the window token (passed in from listener)
     */
    private fun hideSystemKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    /**
     * A helper method to show the custom keyboard
     */
    private fun showCustomKeyboard() {
        if (d_keyboard.visibility == View.GONE) {
            d_keyboard!!.executeEnterAnimation()
        }
    }

    /**
     * A helper method to hide the custom keyboard
     */
    private fun hideCustomKeyboard() {
        if (d_keyboard!!.visibility == View.VISIBLE) {
            d_keyboard!!.executeExitAnimation()
        }
    }

    /**
     * Override of FavoriteDiceRoll click method for RecyclerView
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
     * Helper method to setup FavoriteRecyclerView, should only be called once in onCreate
     */
    private fun setupFavoriteRecyclerView() {
        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        main_favorite_rv.layoutManager = layoutManager

        mFavoriteDiceRollAdapter = FavoriteDiceRollAdapter(this, this)
        main_favorite_rv.adapter = mFavoriteDiceRollAdapter

        main_favorite_rv.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

        if (mDiceRolls == null) {
            mDiceRolls = ArrayList()
        }
        mFavoriteDiceRollAdapter!!.setFavoriteDiceRolls(mDiceRolls)
    }

    /**
     * Helper method to set click listeners to various views, should only be called once in onCreate
     */
    private fun setOnClickListeners() {
        favorites_button.setOnClickListener {
            //Click listener to launch FavoritesActivity
            val intent = Intent(this@MainActivity, FavoriteActivity::class.java)
            startActivity(intent)
        }

        roll_button.setOnClickListener { v ->
            //Click listener to check the validity of a roll, and execute it if it is acceptable
            val formula = command_input_et!!.text.toString()
            val diceValidity = Utils.isValidDiceRoll(this@MainActivity, formula) //Get a boolean of whether the
            if (diceValidity.isValid) {
                //If formula is okay, make a new nameless DiceRoll for display in the results text
                val diceRoll = DiceRoll(formula = formula, hasOverHundredDice = diceValidity.hasOverHundredDice)

                RollAsyncTask(this@MainActivity).execute(diceRoll)

                //Hide keyboards
                hideSystemKeyboard(v)
                hideCustomKeyboard()
            } else {
                Toast.makeText(this@MainActivity, diceValidity.errorMessage, Toast.LENGTH_SHORT).show()
            }
        }

        main_help_button.setOnClickListener {
            val builder = AlertDialog.Builder(this@MainActivity)
            builder.setPositiveButton(R.string.help_dialog_positive_button) { dialog, which ->
                dialog.dismiss()
            }
                .setMessage(R.string.help_formula_advice)
                .setTitle(R.string.help_header)
            builder.show()
        }

        clear_button.setOnClickListener { command_input_et.setText("") }
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
        inflater.inflate(R.menu.main_activity_menu, menu)
        MenuCompat.setGroupDividerEnabled(menu, true)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_about_menu -> {
                val aboutIntent = Intent(this@MainActivity, AboutActivity::class.java)
                startActivity(aboutIntent)
                return true
            }
            R.id.action_history -> {
                val intent = Intent(this@MainActivity, HistoryActivity::class.java)
                startActivity(intent)
                return true
            }
            R.id.action_sign_out -> {
                val builder = AlertDialog.Builder(this@MainActivity)
                builder.setTitle(R.string.sign_out_dialog_title)
                    .setMessage(R.string.sign_out_dialog_message)
                    .setPositiveButton(R.string.sign_out_dialog_positive) { dialog, which ->
                        AuthUI.getInstance().signOut(this@MainActivity)
                    }
                    .setNegativeButton(R.string.sign_out_dialog_negative) { dialog, which -> dialog.cancel() }
                builder.show()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    /**
     * A helper method to setup the EditText and Keyboard, to be called once in onCreate
     */
    private fun setupEditTextAndKeyboard() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) { //Use custom keyboard if the Android version is over 21 (this is when showSoftInputOnFocus was implemented)
            command_input_et.showSoftInputOnFocus = false

            mCommandInputConnection = command_input_et.onCreateInputConnection(EditorInfo())
            d_keyboard.setInputConnection(mCommandInputConnection!!)

            command_input_et.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                //FocusChange listener to show custom keyboard, and minimize keyboard when clicking outside of EditText
                if (hasFocus) {
                    showCustomKeyboard()
                } else {
                    hideCustomKeyboard()
                }
            }

            command_input_et.setOnClickListener {
                //Necessary to make the custom keyboard visible if focus has not been lost, but the keyboard is minimized
                if (d_keyboard.visibility != View.VISIBLE) {
                    showCustomKeyboard()
                }
            }
        } else { //Use basic system keyboard
            command_input_et.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                //FocusChange listener to minimize keyboard when clicking outside of EditText
                if (!hasFocus) {
                    hideSystemKeyboard(v)
                }
            }
        }
    }

    override fun onBackPressed() {
        if (d_keyboard.visibility == View.VISIBLE) { //Override to hide custom keyboard if visible when back pressed
            hideCustomKeyboard()
            command_input_et.clearFocus()
            return
        }
        super.onBackPressed()
    }

    /**
     * A helper method to handle all of the initial setup for Firebase, to be called in onCreate
     */
    private fun initializeFirebase() {
        //Auth setup
        mUserID = Constants.FIREBASE_ANONYMOUS
        mFirebaseAuth = FirebaseAuth.getInstance()
        mAuthStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) { //Signed in
                onSignedInInitialize(user.uid)
            } else { //Signed out, so launch sign in activity
                onSignedOutCleanup()
                startActivityForResult(
                    AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setIsSmartLockEnabled(false)
                        .setAvailableProviders(
                            listOf(
                                AuthUI.IdpConfig.GoogleBuilder().build(),
                                AuthUI.IdpConfig.EmailBuilder().build()
                            )
                        )
                        .build(),
                    Constants.REQUEST_CODE_SIGN_IN
                )
            }
        }

        //Database setup
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

        //Blank Favorites
        mDiceRolls = ArrayList()
        mFavoriteDiceRollAdapter!!.setFavoriteDiceRolls(mDiceRolls)

        //Blank Results(by blanking SharedPreferences)
        val sharedPreferences = getSharedPreferences(getString(R.string.shared_preferences_key), Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        editor.putString(getString(R.string.dice_results_name_key), "")
        editor.putString(getString(R.string.dice_results_descrip_key), "")
        editor.putLong(getString(R.string.dice_results_total_key), 0)
        editor.putLong(getString(R.string.dice_results_date_key), 0)
        editor.apply()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Constants.REQUEST_CODE_SIGN_IN) { //Special logic for results coming from FirebaseUI sign-in
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(this, R.string.sign_in_success, Toast.LENGTH_SHORT).show()
            } else if (resultCode == Activity.RESULT_CANCELED) { //User canceled, finish(); to close app
                finish()
            }
        }
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
                    mFavoriteDiceRollAdapter!!.setFavoriteDiceRolls(mDiceRolls)
                    Utils.updateAllWidgets(this@MainActivity, mDiceRolls!!)
                }

                override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {

                }

                override fun onChildRemoved(dataSnapshot: DataSnapshot) {
                    val deletedDiceRoll = dataSnapshot.getValue(DiceRoll::class.java)
                    for (diceRoll in mDiceRolls!!) {
                        if (deletedDiceRoll!!.name == diceRoll.name && deletedDiceRoll.formula == diceRoll.formula) { //If the name and formula match, delete it
                            mDiceRolls!!.remove(diceRoll)
                            mFavoriteDiceRollAdapter!!.setFavoriteDiceRolls(mDiceRolls)
                            Utils.updateAllWidgets(this@MainActivity, mDiceRolls!!)
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
     * Override, for any given diceResults that occur from rolling a DiceRoll
     *
     * @param diceResults to use
     */
    override fun handleRollResult(diceResults: DiceResults) {
        diceResults.saveToSharedPreferences(this)
        setDataToResultsViews(diceResults)
        diceResults.saveToFirebaseHistory(mBaseDatabaseReference!!, mUserID!!)
    }

    /**
     * A helper method to setup an ad into the activity's AdView
     */
    private fun setupAds() {
        val adRequest = AdRequest.Builder().build()
        banner_ad.loadAd(adRequest)
    }
}
