package com.example.android.simpledice.widget;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.example.android.simpledice.R;
import com.example.android.simpledice.utils.DiceRoll;
import com.example.android.simpledice.utils.Utils;

import java.util.List;

public class ListWidgetService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        Context applicationContext = this.getApplicationContext();
        if (intent.hasExtra(getString(R.string.widget_dice_roll_parcelable_key))){
            String stringDiceRolls = intent.getStringExtra(getString(R.string.widget_dice_roll_parcelable_key));
            return new ListRemoteViewsFactory(applicationContext, stringDiceRolls);
        }
        return new ListRemoteViewsFactory(applicationContext, "");
    }
}

class ListRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private Context mContext;
    private List<DiceRoll> mDiceRolls;

    //Constructor for DiceRolls List
    public ListRemoteViewsFactory(Context applicationContext, String stringDiceRolls) {
        mContext = applicationContext;
        mDiceRolls = Utils.stringToDiceRolls(stringDiceRolls);
    }

    @Override
    public void onCreate() {

    }

    @Override
    public void onDataSetChanged() {

    }

    @Override
    public void onDestroy() {

    }

    @Override
    public int getCount() {
        if (mDiceRolls == null) return 0;
        return mDiceRolls.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        RemoteViews views = new RemoteViews(mContext.getPackageName(), R.layout.widget_favorite_list_item);

        //Set the data to the favorites item
        DiceRoll diceRoll = mDiceRolls.get(position);
        views.setTextViewText(R.id.widget_item_name_tv, diceRoll.getName());
        views.setTextViewText(R.id.widget_item_formula_tv, diceRoll.getFormula());

        //Add the DiceRoll to a FillInIntent to be sent to FavoritesActivity when clicked.  Set to LinearLayout.
        Bundle extras = new Bundle();
        extras.putParcelable(mContext.getString(R.string.widget_favorites_intent_parcelable_key), diceRoll);
        Intent fillInIntent = new Intent();
        fillInIntent.putExtras(extras);
        views.setOnClickFillInIntent(R.id.widget_list_item_ll, fillInIntent);

        return views;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}