package com.example.android.simpledice.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuCompat
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.android.simpledice.R
import com.example.android.simpledice.database.AllDiceResultsViewModel
import com.example.android.simpledice.database.AppDatabase
import com.example.android.simpledice.database.AppExecutors
import com.example.android.simpledice.utils.DiceResults
import com.google.android.gms.ads.AdRequest
import kotlinx.android.synthetic.main.activity_history.*
import kotlinx.android.synthetic.main.banner_ad.*
import kotlinx.android.synthetic.main.results.*
import java.util.*

class HistoryActivity : AppCompatActivity(), HistoryResultsAdapter.HistoryResultsClickHandler {

    private var mHistoryAdapter: HistoryResultsAdapter? = null

    private var mDiceResults: MutableList<DiceResults>? = null

    private var mDatabase : AppDatabase? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        setTitle(R.string.history_activity_title)

        restoreSavedInstanceState(savedInstanceState)

        setupAds()

        setupHistoryRecyclerView()

        setupDatabaseAndViewModel()

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.history_activity_menu, menu)
        MenuCompat.setGroupDividerEnabled(menu, true)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_delete_all_history -> {
                val deleteHistoryBuilder = AlertDialog.Builder(this@HistoryActivity)
                deleteHistoryBuilder.setTitle(R.string.delete_all_dialog_title)
                    .setMessage(R.string.delete_all_dialog_message)
                    .setPositiveButton(R.string.delete_all_dialog_positive) { dialog, which -> clearAllHistory() }
                    .setNegativeButton(R.string.delete_all_dialog_negative) { dialog, which -> dialog.cancel() }
                deleteHistoryBuilder.show()
                return true
            }
            R.id.action_settings -> {
                val settingsIntent = Intent(this@HistoryActivity, SettingsActivity::class.java)
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
        val layoutManager = LinearLayoutManager(
            this,
            RecyclerView.VERTICAL,
            false
        )
        history_rv.layoutManager = layoutManager

        mHistoryAdapter = HistoryResultsAdapter(this)
        history_rv.adapter = mHistoryAdapter

        history_rv.addItemDecoration(
            DividerItemDecoration(
                this,
                DividerItemDecoration.VERTICAL
            )
        )

        if (mDiceResults == null) {
            mDiceResults = ArrayList()
        }
        mHistoryAdapter!!.setHistoryResults(mDiceResults!!)
    }

    /**
     * A helper method that deletes all the user's history from Firebase, and empties all views on HistoryActivity
     */
    private fun clearAllHistory() {
        AppExecutors.getInstance()!!.diskIO.execute {
            mDatabase!!.diceResultsDao().deleteAllDiceResults()
        }

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

    /**
     * A helper method for preparing access to the database, and populating views relevant to it
     */
    private fun setupDatabaseAndViewModel() {
        mDatabase = AppDatabase.getInstance(this)

        val viewModel = ViewModelProviders.of(this).get(AllDiceResultsViewModel::class.java)
        viewModel.diceResults!!.observe(this, androidx.lifecycle.Observer { databaseDiceResults ->

            //Hide Recycler, show ProgressBar
            history_rv.visibility = View.GONE
            history_progress_bar.visibility = View.VISIBLE

            mDiceResults = arrayListOf() //Clear and repopulate mDiceRolls
            for (diceResults in databaseDiceResults) {
                mDiceResults!!.add(diceResults)
            }
            mHistoryAdapter!!.setHistoryResults(mDiceResults!!)

            //Show Recycler, hide ProgressBar
            history_rv.visibility = View.VISIBLE
            history_progress_bar.visibility = View.GONE
        })
    }

    /**
     * Override, save the values of the results view for later restoration
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        //Include the name, descrip, and total from the results views
        outState.putString(getString(R.string.history_saved_instance_name_key), results_name_tv.text.toString())
        outState.putString(getString(R.string.history_saved_instance_descrip_key), results_descrip_tv.text.toString())
        outState.putString(getString(R.string.history_saved_instance_total_key), results_total_tv.text.toString())
    }



    /**
     * A helper method that restores the savedInstanceState on rotation, called in onCreate
     * @param savedInstanceState to restore, provided by onCreate
     */
    private fun restoreSavedInstanceState(savedInstanceState: Bundle?){
        if (savedInstanceState != null){
            //Get saved values, restore to textviews (default to empty string if unavailable)
            val name = savedInstanceState.getString(getString(R.string.history_saved_instance_name_key), "")
            val total = savedInstanceState.getString(getString(R.string.history_saved_instance_total_key), "")
            val descrip = savedInstanceState.getString(getString(R.string.history_saved_instance_descrip_key), "")

            results_name_tv.text = name
            results_total_tv.text = total
            results_descrip_tv.text = descrip

        } else { //Provide a hint to click on recycler item if there is no savedInstanceState
            results_descrip_tv.text = getString(R.string.history_click_a_result)
        }
    }

    override fun onBackPressed() {
        //If over version 21, scroll the results to start to make activity transitions appear smoother
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            descrip_scrollview.smoothScrollTo(0, 0)
        }
        super.onBackPressed()
    }
}
