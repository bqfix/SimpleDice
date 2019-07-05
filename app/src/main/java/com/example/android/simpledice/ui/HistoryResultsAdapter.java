package com.example.android.simpledice.ui;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.android.simpledice.utils.DiceResults;
import com.example.android.simpledice.R;

import java.util.List;

public class HistoryResultsAdapter extends RecyclerView.Adapter<HistoryResultsAdapter.HistoryResultsViewHolder> {

    private List<DiceResults> mHistoryResults;

    private final HistoryResultsClickHandler mHistoryResultsClickHandler;

    HistoryResultsAdapter(HistoryResultsClickHandler historyResultClickHandler) {
        mHistoryResultsClickHandler = historyResultClickHandler;
    }


    //Inner ViewHolder Class
    public class HistoryResultsViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView mNameTextView;
        TextView mDateTextView;
        TextView mTotalTextView;

        //Constructor
        public HistoryResultsViewHolder(@NonNull View itemView) {
            super(itemView);

            mNameTextView = itemView.findViewById(R.id.history_name_tv);
            mDateTextView = itemView.findViewById(R.id.history_date_tv);
            mTotalTextView = itemView.findViewById(R.id.history_total_tv);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int adapterPosition = getAdapterPosition();
            DiceResults diceResults = mHistoryResults.get(adapterPosition);
            mHistoryResultsClickHandler.onItemClick(diceResults);
        }
    }

    @NonNull
    @Override
    public HistoryResultsViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        Context context = viewGroup.getContext();
        int layoutIDForListItem = R.layout.history_recycler_item;
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(layoutIDForListItem, viewGroup, false);

        return new HistoryResultsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryResultsViewHolder viewHolder, int position) {
        DiceResults currentDiceResults = mHistoryResults.get(position);

        String name = currentDiceResults.getName();
        String date = currentDiceResults.getFormattedDateCreated();
        String total = String.valueOf(currentDiceResults.getTotal());

        viewHolder.mNameTextView.setText(name);
        viewHolder.mDateTextView.setText(date);
        viewHolder.mTotalTextView.setText(total);
    }

    @Override
    public int getItemCount() {
        if (mHistoryResults == null) return 0;
        return mHistoryResults.size();
    }

    /**
     * Helper method to set DiceResults List to existing HistoryResultAdapter
     *
     * @param historyResults the new set of DiceResults to be displayed
     */

    public void setHistoryResults(List<DiceResults> historyResults) {
        mHistoryResults = historyResults;
        notifyDataSetChanged();
    }

    //Interface to handle clicks, defined in MainActivity
    public interface HistoryResultsClickHandler {
        void onItemClick(DiceResults historyResults);
    }
}

