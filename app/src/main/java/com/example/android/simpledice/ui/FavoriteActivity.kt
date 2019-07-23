package com.example.android.simpledice.ui

import android.app.ActivityOptions
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Pair
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuCompat
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
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
import kotlinx.android.synthetic.main.activity_favorite.*
import kotlinx.android.synthetic.main.banner_ad.*
import kotlinx.android.synthetic.main.favorite_recycler_view.*
import kotlinx.android.synthetic.main.results.*
import kotlinx.android.synthetic.main.toolbar.*
import java.util.*

class FavoriteActivity : AppCompatActivity(), FavoriteDiceRollAdapter.FavoriteDiceRollClickHandler,
    FavoriteDiceRollAdapter.DeleteDiceRollClickHandler, RollAsyncTask.RollAsyncPreExecute, RollAsyncTask.RollAsyncPostExecute {

    private var mFavoriteDiceRollAdapter: FavoriteDiceRollAdapter? = null

    private var widgetIntentHandled = false

    private var mDiceRolls: MutableList<DiceRoll>? = null

    private var mDatabase: AppDatabase? = null

    private var mIsStillRolling : Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorite)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        setTitle(R.string.favorites_title)

        setupAds()

        restoreSavedInstanceState(savedInstanceState)

        setupRecyclerView()

        add_favorite_fab.setOnClickListener {
            val intent = Intent(this@FavoriteActivity, AddFavoriteActivity::class.java)
            startActivity(intent)
        }

        setupDatabaseAndViewModel()

        handleWidgetIntent()

    }

    override fun onStart() {
        super.onStart()
        loadMostRecentDiceResults()
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
    override fun onItemClick(favoriteDiceRoll: DiceRoll) { //If a previous roll is still going, show an error, else create a new roll
        if (mIsStillRolling) {
            Toast.makeText(this, R.string.async_task_still_executing, Toast.LENGTH_SHORT).show()
        } else {
            RollAsyncTask(this, this, this).execute(favoriteDiceRoll)
        }
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
     * Helper method to setup RecyclerView, should only be called once in onCreate
     */
    private fun setupRecyclerView() {
        val layoutManager = LinearLayoutManager(
            this,
            RecyclerView.VERTICAL,
            false
        )
        favorite_rv.layoutManager = layoutManager

        mFavoriteDiceRollAdapter = FavoriteDiceRollAdapter(this, this)
        favorite_rv.adapter = mFavoriteDiceRollAdapter

        favorite_rv.addItemDecoration(
            DividerItemDecoration(
                this,
                DividerItemDecoration.VERTICAL
            )
        )

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
                val historyIntent = Intent(this@FavoriteActivity, HistoryActivity::class.java)

                //Shared elements to animate (if SDK > 21), otherwise simply start activity
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    //Scroll results to start to make transition smoother
                    descrip_scrollview.smoothScrollTo(0, 0)

                    val optionsBundle = ActivityOptions.makeSceneTransitionAnimation(
                        this@FavoriteActivity,
                        Pair.create<View, String>(results_header, results_header.transitionName)
                    ).toBundle()

                    startActivity(historyIntent, optionsBundle)
                } else {
                    startActivity(historyIntent)
                }
                return true
            }
            R.id.action_settings -> {
                val settingsIntent = Intent(this@FavoriteActivity, SettingsActivity::class.java)
                startActivity(settingsIntent)
                return true
            }
            android.R.id.home -> {
                onBackPressed()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }


    /**
     * A helper method to handle Intents coming from the widget that contain a DiceRoll to be rolled
     * Called in onCreate, must be called after database is setup, and after savedInstanceState is restored.
     */
    private fun handleWidgetIntent() {
        if (!widgetIntentHandled) {
            val intent = intent
            val parcelableKey = getString(R.string.widget_favorites_intent_parcelable_key)
            if (intent.hasExtra(parcelableKey)) { //If available, retrieve DiceRoll
                val diceRoll = intent.getParcelableExtra<DiceRoll>(parcelableKey)
                RollAsyncTask(this,this, this).execute(diceRoll)
            }
            widgetIntentHandled = true
        }
    }

    /**
     * Override to handle UI changes, etc when a roll begins executing
     */
    override fun handleRollPreExecute() {
        mIsStillRolling = true

        /* Check again after 200ms if IsStillRolling, and show the progress bar if so.
        This is to prevent blinking ProgressBars on rolls that happen almost instantly (i.e. most rolls)
        */
        val handler = Handler()
        handler.postDelayed({ if (mIsStillRolling){ results_progress_bar.visibility = View.VISIBLE }}, 200)
    }

    /** Handling for any given diceResults that occur from rolling a DiceRoll
     *
     * @param diceResults to use
     */
    override fun handleRollResult(diceResults: DiceResults) {
        mIsStillRolling = false
        results_progress_bar.visibility = View.GONE

        diceResults.saveToSharedPreferences(this)
        setDataToResultsViews(diceResults)
        AppExecutors.getInstance()!!.diskIO.execute {
            mDatabase!!.diceResultsDao().insertDiceResults(diceResults)
        }
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

    /**
     * A helper method for preparing access to the database, and populating views relevant to it
     */
    private fun setupDatabaseAndViewModel() {
        mDatabase = AppDatabase.getInstance(this)

        val viewModel = ViewModelProviders.of(this).get(AllDiceRollsViewModel::class.java)
        viewModel.diceRolls!!.observe(this, androidx.lifecycle.Observer { diceRolls ->

            //Hide Recycler and error, show ProgressBar
            favorite_activity_no_favorites_tv.visibility = View.GONE
            favorite_rv.visibility = View.GONE
            favorite_progress_bar.visibility = View.VISIBLE

            mDiceRolls = arrayListOf() //Clear and repopulate mDiceRolls
            for (diceRoll in diceRolls) {
                mDiceRolls!!.add(diceRoll)
            }
            mFavoriteDiceRollAdapter!!.setFavoriteDiceRolls(mDiceRolls!!)
            Utils.updateAllWidgets(this@FavoriteActivity, mDiceRolls!!)

            //Hide ProgressBar, check for favorites
            favorite_progress_bar.visibility = View.GONE
            if (mDiceRolls!!.isEmpty()) {
                //Show error
                favorite_activity_no_favorites_tv.visibility = View.VISIBLE
            } else {
                //Show Recycler
                favorite_rv.visibility = View.VISIBLE
            }
        })
    }

    override fun onBackPressed() {
        //If over version 21, scroll the results to start to make activity transitions appear smoother
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            descrip_scrollview.smoothScrollTo(0, 0)
            val layoutManager = favorite_rv.layoutManager as LinearLayoutManager
            layoutManager.scrollToPositionWithOffset(0, 0)

            //Set the CardView's isTransitionGroup to true, to prevent blinking on return
            favorite_rv_container.isTransitionGroup = true
            result_container.isTransitionGroup = true
        }
        super.onBackPressed()
    }
}
