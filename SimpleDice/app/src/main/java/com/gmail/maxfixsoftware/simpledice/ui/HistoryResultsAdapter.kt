package com.gmail.maxfixsoftware.simpledice.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.gmail.maxfixsoftware.simpledice.R
import com.gmail.maxfixsoftware.simpledice.utils.DiceResults

class HistoryResultsAdapter internal constructor(private val mHistoryResultsClickHandler: HistoryResultsClickHandler) :
    androidx.recyclerview.widget.RecyclerView.Adapter<HistoryResultsAdapter.HistoryResultsViewHolder>() {

    private var mHistoryResults: List<DiceResults>? = null


    //Inner ViewHolder Class
    inner class HistoryResultsViewHolder
        (
        itemView: View,
        var mNameTextView: TextView? = null,
        var mDateTextView: TextView? = null,
        var mTotalTextView: TextView? = null
    ) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView), View.OnClickListener {

        init {

            mNameTextView = itemView.findViewById(R.id.history_name_tv)
            mDateTextView = itemView.findViewById(R.id.history_date_tv)
            mTotalTextView = itemView.findViewById(R.id.history_total_tv)

            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            val adapterPosition = adapterPosition
            val diceResults = mHistoryResults!![adapterPosition]
            mHistoryResultsClickHandler.onItemClick(diceResults)
        }
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): HistoryResultsViewHolder {
        val context = viewGroup.context
        val layoutIDForListItem = R.layout.history_recycler_item
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(layoutIDForListItem, viewGroup, false)

        return HistoryResultsViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: HistoryResultsViewHolder, position: Int) {
        val currentDiceResults = mHistoryResults!![position]

        val name = currentDiceResults.name
        val date = currentDiceResults.formattedDateCreated
        val total = currentDiceResults.total.toString()

        viewHolder.mNameTextView!!.text = name
        viewHolder.mDateTextView!!.text = date
        viewHolder.mTotalTextView!!.text = total
    }

    override fun getItemCount(): Int {
        return if (mHistoryResults == null) 0 else mHistoryResults!!.size
    }

    /**
     * Helper method to set DiceResults List to existing HistoryResultAdapter
     *
     * @param historyResults the new set of DiceResults to be displayed
     */

    fun setHistoryResults(historyResults: List<DiceResults>) {
        mHistoryResults = historyResults
        notifyDataSetChanged()
    }

    //Interface to handle clicks, defined in MainActivity
    interface HistoryResultsClickHandler {
        fun onItemClick(historyResults: DiceResults)
    }
}

