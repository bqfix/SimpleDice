package com.example.android.simpledice.ui

import android.os.Bundle
import android.support.v4.view.MenuCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.view.Menu
import android.view.MenuItem
import com.example.android.simpledice.R
import com.example.android.simpledice.utils.DiceResults
import com.google.android.gms.ads.AdRequest
import kotlinx.android.synthetic.main.activity_history.*
import kotlinx.android.synthetic.main.banner_ad.*
import kotlinx.android.synthetic.main.results.*
import java.util.*

class HistoryActivity : AppCompatActivity(), HistoryResultsAdapter.HistoryResultsClickHandler {

    private var mHistoryAdapter: HistoryResultsAdapter? = null

    private var mDiceResults: MutableList<DiceResults>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        setTitle(R.string.history_activity_title)

        setupAds()

        setupHistoryRecyclerView()

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
     * A helper method that deletes all the user's history from Firebase, and empties all views on HistoryActivity
     */
    private fun clearAllHistory() {
        //TODO Delete from Room

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
