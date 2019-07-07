package com.example.android.simpledice.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.android.simpledice.R
import com.example.android.simpledice.utils.DiceResults
import com.example.android.simpledice.utils.DiceRoll
import com.example.android.simpledice.utils.RollAsyncTask
import com.example.android.simpledice.utils.Utils
import com.google.android.gms.ads.AdRequest
import kotlinx.android.synthetic.main.banner_ad.*
import kotlinx.android.synthetic.main.favorite_recycler_view.*
import kotlinx.android.synthetic.main.results.*
import java.util.*

class FavoriteActivity : AppCompatActivity(), FavoriteDiceRollAdapter.FavoriteDiceRollClickHandler,
    FavoriteDiceRollAdapter.DeleteDiceRollClickHandler, RollAsyncTask.RollAsyncPostExecute {

    private var mFavoriteDiceRollAdapter: FavoriteDiceRollAdapter? = null

    private var widgetIntentHandled = false

    private var mDiceRolls: MutableList<DiceRoll>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorite)
        setTitle(R.string.favorites_title)

        setupAds()

        restoreSavedInstanceState(savedInstanceState)

        setupRecyclerView()

        add_favorite_fab.setOnClickListener {
            val intent = Intent(this@FavoriteActivity, AddFavoriteActivity::class.java)
            startActivity(intent)
        }

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
    override fun onItemClick(favoriteDiceRoll: DiceRoll) {
        RollAsyncTask(this).execute(favoriteDiceRoll)
    }

    override fun onDeleteClick(favoriteDiceRoll: DiceRoll) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.delete_dialog_title)
            .setMessage(R.string.delete_dialog_message)
            .setPositiveButton(R.string.delete_dialog_positive) { dialog, which ->
                //TODO Delete from Room
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
                val intent = Intent(this@FavoriteActivity, HistoryActivity::class.java)
                startActivity(intent)
                return true
            }
            else -> return super.onOptionsItemSelected(item)
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
        //TODO Save to Room
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
