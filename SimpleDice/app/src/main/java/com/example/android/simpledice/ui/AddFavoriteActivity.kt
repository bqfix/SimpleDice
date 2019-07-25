package com.example.android.simpledice.ui

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuCompat
import com.example.android.simpledice.R
import com.example.android.simpledice.database.AppDatabase
import com.example.android.simpledice.database.AppExecutors
import com.example.android.simpledice.utils.Constants
import com.example.android.simpledice.utils.DiceRoll
import com.example.android.simpledice.utils.Utils
import com.google.android.gms.ads.AdRequest
import kotlinx.android.synthetic.main.activity_add_favorite.*
import kotlinx.android.synthetic.main.banner_ad.*
import kotlinx.android.synthetic.main.toolbar.*

class AddFavoriteActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    private var mInputConnection: InputConnection? = null

    //Included DiceRoll variables
    private var editingFavorite: Boolean = false

    private var mDatabase : AppDatabase? = null

    private var mDiceRoll : DiceRoll? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_favorite)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        setupAds()

        checkForIncludedDiceRoll()

        setListeners()

        setupFormulaEditTextAndKeyboard()

        registerOnSharedPreferenceChangeListener()

        setupDatabase()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterOnSharedPreferenceChangeListener()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = this.menuInflater
        inflater.inflate(R.menu.add_favorite_activity_menu, menu)
        MenuCompat.setGroupDividerEnabled(menu, true)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_done -> {
                saveNewFavorite()
                return true
            }
            R.id.action_settings -> {
                val settingsIntent = Intent(this@AddFavoriteActivity, SettingsActivity::class.java)
                startActivity(settingsIntent)
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    /**
     * A helper method for saving a new favorite
     */
    private fun saveNewFavorite() {
        val name = name_input_et.text.toString() //Get name and ensure it doesn't have invalid characters
        if (name.contains(Constants.NAME_FORMULA_HUNDRED_BREAK) || name.contains(Constants.DICEROLL_BREAK)) {
            Toast.makeText(this, R.string.name_contains_invalid_characters, Toast.LENGTH_SHORT).show()
            return
        }
        //Check that formula is valid
        val formula = formula_input_et.text.toString()
        val (isValid, errorMessage, hasOverHundredDice) = Utils.isValidDiceRoll(this, formula)
        if (isValid) {
            if (editingFavorite) {
                //Failsafe in case mDiceRoll is null for some reason, although it should not be
                if (mDiceRoll == null){
                    mDiceRoll = DiceRoll()
                }
                mDiceRoll!!.name == name
                mDiceRoll!!.formula = formula
                mDiceRoll!!.hasOverHundredDice == hasOverHundredDice
                AppExecutors.getInstance()!!.diskIO.execute {
                    mDatabase!!.diceRollDao().updateDiceRoll(mDiceRoll!!)
                }
            } else {
                mDiceRoll = DiceRoll(name = name, formula = formula, hasOverHundredDice = hasOverHundredDice)
                AppExecutors.getInstance()!!.diskIO.execute {
                    mDatabase!!.diceRollDao().insertDiceRoll(mDiceRoll!!)
                }
            }
            Toast.makeText(this, R.string.saved_to_firebase, Toast.LENGTH_SHORT).show()
            finish()
        } else {
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
        }

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
            d_keyboard.executeEnterAnimation()
        }
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            name_header_tv.visibility = View.GONE
            name_input_et.visibility = View.GONE
        }
    }

    /**
     * A helper method to hide the custom keyboard
     */
    private fun hideCustomKeyboard() {
        if (d_keyboard.visibility == View.VISIBLE) {
            d_keyboard.executeExitAnimation()
        }
        name_header_tv.visibility = View.VISIBLE
        name_input_et.visibility = View.VISIBLE
    }

    /**
     * Helper method to set listeners to various views, should only be called once in onCreate
     */
    private fun setListeners() {
        name_input_et.onFocusChangeListener =
            View.OnFocusChangeListener { v, hasFocus -> //FocusChange listener to minimize keyboard when clicking outside of NameEditText
                hideSystemKeyboard(v)
            }
    }

    override fun onBackPressed() {
        if (d_keyboard.visibility == View.VISIBLE) { //Override to hide custom keyboard if visible when back pressed
            hideCustomKeyboard()
            formula_input_et.clearFocus()
            return
        }
        super.onBackPressed()
    }

    /**
     * A helper method to setup the Formula EditText and Keyboard, to be called once in onCreate
     */
    private fun setupFormulaEditTextAndKeyboard() {
        //Check SharedPreferences for user's preference (assume true if not found)
        val useDKeyboard =
            PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(getString(R.string.use_d_keyboard_key), true)

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP && useDKeyboard) { //Use custom keyboard if the Android version is over 21 (this is when showSoftInputOnFocus was implemented)
            formula_input_et.showSoftInputOnFocus = false

            mInputConnection = formula_input_et.onCreateInputConnection(EditorInfo())
            d_keyboard.setInputConnection(mInputConnection!!)

            formula_input_et.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                //FocusChange listener to show custom keyboard, and minimize keyboard when clicking outside of EditText
                if (hasFocus) {
                    showCustomKeyboard()
                } else {
                    hideCustomKeyboard()
                }
            }

            formula_input_et.setOnClickListener  { //Necessary to make the custom keyboard visible if focus has not been lost, but the keyboard is minimized
                if (d_keyboard.visibility != View.VISIBLE) {
                    showCustomKeyboard()
                }
            }
        } else { //Use basic system keyboard

            //Hide Custom Keyboard if visible, and enable system keyboard (if Android version > 21)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (d_keyboard.visibility == View.VISIBLE) {
                    hideCustomKeyboard()
                }
                formula_input_et.showSoftInputOnFocus = true
            }

            //Clear previous onClickListener
            formula_input_et.setOnClickListener(null)

            formula_input_et.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                //FocusChange listener to minimize keyboard when clicking outside of EditText
                if (!hasFocus) {
                    hideSystemKeyboard(v)
                }
            }
        }
    }

    /**
     * A helper method that checks if a DiceRoll was sent in the intent, and sets up the activity
     * for editing or adding a new DiceRoll accordingly
     */
    private fun checkForIncludedDiceRoll() {
        val intent = intent
        editingFavorite = intent.hasExtra(getString(R.string.dice_roll_parcelable_key))
        if (editingFavorite) {
            setTitle(R.string.edit_favorite_activity_title)

            //Get DiceRoll and populate fields
            mDiceRoll = intent.getParcelableExtra<DiceRoll>(getString(R.string.dice_roll_parcelable_key))
            name_input_et.setText(mDiceRoll!!.name)
            formula_input_et.setText(mDiceRoll!!.formula)
        } else {
            setTitle(R.string.add_favorite_activity_title)
        }
    }

    /**
     * A helper method to setup an ad into the activity's AdView
     */
    private fun setupAds() {
        val adRequest = AdRequest.Builder().build()
        banner_ad.loadAd(adRequest)
    }

    /**
     * A helper method for preparing access to the database
     */
    private fun setupDatabase() {
        mDatabase = AppDatabase.getInstance(this)
    }

    /**
     * Override to re-do the keyboard to use when the relevant preference changes
     */
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key != null && key == getString(R.string.use_d_keyboard_key)){
            setupFormulaEditTextAndKeyboard()
        }
    }

    /**
     * Called in onCreate to register the OnSharedPreferenceChangeListener that listens for keyboard preference changes
     */
    fun registerOnSharedPreferenceChangeListener(){
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }


    /**
     * Called in onDestroy to unregister the OnSharedPreferenceChangeListener that listens for keyboard preference changes
     */
    fun unregisterOnSharedPreferenceChangeListener(){
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }
}
