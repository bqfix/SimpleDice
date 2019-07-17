package com.example.android.simpledice.ui

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Pair
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuCompat
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.android.simpledice.R
import com.example.android.simpledice.database.AllDiceRollsViewModel
import com.example.android.simpledice.database.AppDatabase
import com.example.android.simpledice.database.AppExecutors
import com.example.android.simpledice.utils.DiceResults
import com.example.android.simpledice.utils.DiceRoll
import com.example.android.simpledice.utils.RollAsyncTask
import com.example.android.simpledice.utils.Utils
import com.google.android.gms.ads.AdRequest
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.banner_ad.*
import kotlinx.android.synthetic.main.d_keyboard.*
import kotlinx.android.synthetic.main.main_edit_favorite.*
import kotlinx.android.synthetic.main.results.*
import java.util.*

class MainActivity : AppCompatActivity(), FavoriteDiceRollAdapter.FavoriteDiceRollClickHandler,
    FavoriteDiceRollAdapter.DeleteDiceRollClickHandler, RollAsyncTask.RollAsyncPostExecute,
    SharedPreferences.OnSharedPreferenceChangeListener {

    private var mFavoriteDiceRollAdapter: FavoriteDiceRollAdapter? = null
    private var mDiceRolls: MutableList<DiceRoll>? = null

    //InputConnection for custom keyboard
    private var mCommandInputConnection: InputConnection? = null

    private var mDatabase: AppDatabase? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupAds()

        setOnClickListeners()

        setupFavoriteRecyclerView()

        setupEditTextAndKeyboard()

        registerOnSharedPreferenceChangeListener()

        setupDatabaseAndViewModel()

    }

    override fun onStart() {
        super.onStart()
        loadMostRecentDiceResults()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterOnSharedPreferenceChangeListener()
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
                AppExecutors.getInstance()!!.diskIO.execute {
                    mDatabase!!.diceRollDao().deleteDiceRoll(favoriteDiceRoll)
                }
            }
            .setNegativeButton(R.string.delete_dialog_negative) { dialog, which -> dialog.cancel() }
            .show()
    }

    /**
     * Helper method to setup FavoriteRecyclerView, should only be called once in onCreate
     */
    private fun setupFavoriteRecyclerView() {
        val layoutManager = LinearLayoutManager(
            this,
            RecyclerView.VERTICAL,
            false
        )
        main_favorite_rv.layoutManager = layoutManager

        mFavoriteDiceRollAdapter = FavoriteDiceRollAdapter(this, this)
        main_favorite_rv.adapter = mFavoriteDiceRollAdapter

        main_favorite_rv.addItemDecoration(
            androidx.recyclerview.widget.DividerItemDecoration(
                this,
                androidx.recyclerview.widget.DividerItemDecoration.VERTICAL
            )
        )

        if (mDiceRolls == null) {
            mDiceRolls = ArrayList()
        }
        mFavoriteDiceRollAdapter!!.setFavoriteDiceRolls(mDiceRolls!!)
    }

    /**
     * Helper method to set click listeners to various views, should only be called once in onCreate
     */
    private fun setOnClickListeners() {
        favorites_button.setOnClickListener {
            //Click listener to launch FavoritesActivity
            val intent = Intent(this@MainActivity, FavoriteActivity::class.java)

            //Shared elements to animate (if SDK > 21), otherwise simply start activity
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                //Scroll results to start to make transition smoother
                descrip_scrollview.smoothScrollTo(0,0)

                val optionsBundle = ActivityOptions.makeSceneTransitionAnimation(
                    this@MainActivity,
                    Pair.create<View,String>(main_favorite_rv, main_favorite_rv.transitionName),
                    Pair.create<View,String>(results_constraint_layout, results_constraint_layout.transitionName),
                    Pair.create<View,String>(results_divider, results_divider.transitionName),
                    Pair.create<View,String>(ad_divider, ad_divider.transitionName)
                ).toBundle()
                startActivity(intent, optionsBundle)
            } else {
                startActivity(intent)
            }
        }

        roll_button.setOnClickListener { v ->
            executeDiceRoll(v)
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
            R.id.action_settings -> {
                val settingsIntent = Intent(this@MainActivity, SettingsActivity::class.java)
                startActivity(settingsIntent)
                return true
            }
            R.id.action_history -> {
                val intent = Intent(this@MainActivity, HistoryActivity::class.java)
                startActivity(intent)
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    /**
     * A helper method to setup the EditText and Keyboard, to be called once in onCreate
     */
    private fun setupEditTextAndKeyboard() {
        //Check SharedPreferences for user's preference (assume true if not found)
        val useDKeyboard =
            PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(getString(R.string.use_d_keyboard_key), true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && useDKeyboard) { //Use custom keyboard if the Android version is over 21 (this is when showSoftInputOnFocus was implemented)
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

            //Custom enter button logic for MainActivity
            enter_button.setOnClickListener { v ->
                executeDiceRoll(v)
            }
        } else { //Use basic system keyboard

            //Hide Custom Keyboard if visible, and enable system keyboard (if Android version > 21)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (d_keyboard.visibility == View.VISIBLE) {
                    hideCustomKeyboard()
                }
                command_input_et.showSoftInputOnFocus = true
            }

            //Clear the old onClickListener if it exists
            command_input_et.setOnClickListener(null)

            //Add listener to perform logic when enter key is clicked
            command_input_et.setOnKeyListener { v, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_ENTER) {
                    executeDiceRoll(v)
                    return@setOnKeyListener true
                }
                return@setOnKeyListener false
            }

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
     * Override, for any given diceResults that occur from rolling a DiceRoll
     *
     * @param diceResults to use
     */
    override fun handleRollResult(diceResults: DiceResults) {
        diceResults.saveToSharedPreferences(this)
        setDataToResultsViews(diceResults)
        AppExecutors.getInstance()!!.diskIO.execute {
            mDatabase!!.diceResultsDao().insertDiceResults(diceResults)
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
     * A helper method for preparing access to the database, and populating views relevant to it
     */
    private fun setupDatabaseAndViewModel() {
        mDatabase = AppDatabase.getInstance(this)

        val viewModel = ViewModelProviders.of(this).get(AllDiceRollsViewModel::class.java)
        viewModel.diceRolls!!.observe(this, androidx.lifecycle.Observer { diceRolls ->

            //Hide Recycler and error, show ProgressBar
            main_favorite_rv.visibility = View.GONE
            main_no_favorites_tv.visibility = View.GONE
            main_progress_bar.visibility = View.VISIBLE


            mDiceRolls = arrayListOf() //Clear and repopulate mDiceRolls
            for (diceRoll in diceRolls) {
                mDiceRolls!!.add(diceRoll)
            }
            mFavoriteDiceRollAdapter!!.setFavoriteDiceRolls(mDiceRolls!!)
            Utils.updateAllWidgets(this@MainActivity, mDiceRolls!!)

            //Hide progress, check if there are any favorites
            main_progress_bar.visibility = View.GONE
            if (mDiceRolls!!.isEmpty()) {
                //Show error
                main_no_favorites_tv.visibility = View.VISIBLE
            } else {
                //Show Recycler
                main_favorite_rv.visibility = View.VISIBLE

            }
        })
    }

    /**
     * Override to re-do the keyboard to use when the relevant preference changes
     */
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key != null && key == getString(R.string.use_d_keyboard_key)) {
            setupEditTextAndKeyboard()
        }
    }

    /**
     * Called in onCreate to register the OnSharedPreferenceChangeListener that listens for keyboard preference changes
     */
    fun registerOnSharedPreferenceChangeListener() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }


    /**
     * Called in onDestroy to unregister the OnSharedPreferenceChangeListener that listens for keyboard preference changes
     */
    fun unregisterOnSharedPreferenceChangeListener() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    /**
     * A function to perform the steps necessary to extract a formula and roll the dice, called in clickListeners
     */
    fun executeDiceRoll(view: View) {
        //Check the validity of a roll, and execute it if it is acceptable
        val formula = command_input_et!!.text.toString()
        val diceValidity = Utils.isValidDiceRoll(this@MainActivity, formula) //Get a boolean of whether the
        if (diceValidity.isValid) {
            //If formula is okay, make a new nameless DiceRoll for display in the results text
            val diceRoll = DiceRoll(formula = formula, hasOverHundredDice = diceValidity.hasOverHundredDice)

            RollAsyncTask(this@MainActivity).execute(diceRoll)

            //Hide keyboards
            hideSystemKeyboard(view)
            hideCustomKeyboard()
        } else {
            Toast.makeText(this@MainActivity, diceValidity.errorMessage, Toast.LENGTH_SHORT).show()
        }
    }
}
