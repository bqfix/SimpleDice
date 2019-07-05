package com.example.android.simpledice.ui;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.example.android.simpledice.utils.DiceRoll;
import com.example.android.simpledice.R;

import java.util.List;


public class FavoriteDiceRollAdapter extends RecyclerView.Adapter<FavoriteDiceRollAdapter.FavoriteDiceRollViewHolder> {

    private List<DiceRoll> mFavoriteDiceRolls;

    private final FavoriteDiceRollClickHandler mFavoriteDiceRollClickHandler;
    private final DeleteDiceRollClickHandler mDeleteDiceRollClickHandler;

    FavoriteDiceRollAdapter(FavoriteDiceRollClickHandler favoriteDiceRollClickHandler, DeleteDiceRollClickHandler deleteDiceRollClickHandler) {
        mFavoriteDiceRollClickHandler = favoriteDiceRollClickHandler;
        mDeleteDiceRollClickHandler = deleteDiceRollClickHandler;
    }


    //Inner ViewHolder Class
    public class FavoriteDiceRollViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView mNameTextView;
        TextView mDescripTextView;
        ImageButton mMoreButton;

        //Constructor
        public FavoriteDiceRollViewHolder(@NonNull View itemView) {
            super(itemView);

            mNameTextView = itemView.findViewById(R.id.favorite_name_tv);
            mDescripTextView = itemView.findViewById(R.id.favorite_descrip_tv);
            mMoreButton = itemView.findViewById(R.id.favorite_more_button);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int adapterPosition = getAdapterPosition();
            DiceRoll diceRoll = mFavoriteDiceRolls.get(adapterPosition);
            mFavoriteDiceRollClickHandler.onItemClick(diceRoll);
        }
    }

    @NonNull
    @Override
    public FavoriteDiceRollViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        Context context = viewGroup.getContext();
        int layoutIDForListItem = R.layout.favorite_recycler_item;
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(layoutIDForListItem, viewGroup, false);

        return new FavoriteDiceRollViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final FavoriteDiceRollViewHolder viewHolder, int position) {
        final DiceRoll currentDiceRoll = mFavoriteDiceRolls.get(position);

        String name = currentDiceRoll.getName();
        String descrip = currentDiceRoll.getFormula();

        viewHolder.mNameTextView.setText(name);
        viewHolder.mDescripTextView.setText(descrip);

        viewHolder.mMoreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Context context = v.getContext();
                PopupMenu popup = new PopupMenu(context, v);
                popup.getMenuInflater().inflate(R.menu.favorite_item_more_menu, popup.getMenu());

                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case (R.id.action_edit_favorite): //Launch AddFavoriteActivity with included diceRoll
                                Intent intent = new Intent(context, AddFavoriteActivity.class);
                                intent.putExtra(context.getString(R.string.dice_roll_parcelable_key), currentDiceRoll);
                                context.startActivity(intent);
                                return true;
                            case (R.id.action_delete_favorite):
                                mDeleteDiceRollClickHandler.onDeleteClick(currentDiceRoll);
                                return true;
                            default:
                                return true;
                        }
                    }
                });
                popup.show();
            }
        });
    }

    @Override
    public int getItemCount() {
        if (mFavoriteDiceRolls == null) return 0;
        return mFavoriteDiceRolls.size();
    }

    /**
     * Helper method to set FavoriteDiceRoll List to existing FavoriteDiceRollAdapter
     *
     * @param favoriteDiceRolls the new set of FavoriteDiceRolls to be displayed
     */

    public void setFavoriteDiceRolls(List<DiceRoll> favoriteDiceRolls) {
        mFavoriteDiceRolls = favoriteDiceRolls;
        notifyDataSetChanged();
    }

    //Interface to handle clicks, defined in MainActivity/FavoriteActivity
    public interface FavoriteDiceRollClickHandler {
        void onItemClick(DiceRoll favoriteDiceRoll);
    }

    //Interface to handle delete clicks, defined in MainActivity/FavoriteActivity
    public interface  DeleteDiceRollClickHandler{
        void onDeleteClick(DiceRoll favoriteDiceRoll);
    }
}
