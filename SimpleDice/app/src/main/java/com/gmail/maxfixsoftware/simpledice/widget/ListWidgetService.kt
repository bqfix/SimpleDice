package com.gmail.maxfixsoftware.simpledice.widget

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import android.widget.RemoteViewsService

import com.gmail.maxfixsoftware.simpledice.R
import com.gmail.maxfixsoftware.simpledice.utils.DiceRoll
import com.gmail.maxfixsoftware.simpledice.utils.Utils

class ListWidgetService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val applicationContext = this.applicationContext
        if (intent.hasExtra(getString(R.string.widget_dice_roll_parcelable_key))) {
            val stringDiceRolls = intent.getStringExtra(getString(R.string.widget_dice_roll_parcelable_key))
            return ListRemoteViewsFactory(applicationContext, stringDiceRolls)
        }
        return ListRemoteViewsFactory(applicationContext, "")
    }
}

internal class ListRemoteViewsFactory//Constructor for DiceRolls List
    (private val mContext: Context, stringDiceRolls: String) : RemoteViewsService.RemoteViewsFactory {
    private val mDiceRolls: List<DiceRoll>?

    init {
        mDiceRolls = Utils.stringToDiceRolls(stringDiceRolls)
    }

    override fun onCreate() {

    }

    override fun onDataSetChanged() {

    }

    override fun onDestroy() {

    }

    override fun getCount(): Int {
        return mDiceRolls?.size ?: 0
    }

    override fun getViewAt(position: Int): RemoteViews {
        val views = RemoteViews(mContext.packageName, R.layout.widget_favorite_list_item)

        //Set the data to the favorites item
        val diceRoll = mDiceRolls!![position]
        views.setTextViewText(R.id.widget_item_name_tv, diceRoll.name)
        views.setTextViewText(R.id.widget_item_formula_tv, diceRoll.formula)

        //Add the DiceRoll to a FillInIntent to be sent to FavoritesActivity when clicked.  Set to LinearLayout.
        val extras = Bundle()
        extras.putParcelable(mContext.getString(R.string.widget_favorites_intent_parcelable_key), diceRoll)
        val fillInIntent = Intent()
        fillInIntent.putExtras(extras)
        views.setOnClickFillInIntent(R.id.widget_list_item_ll, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? {
        return null
    }

    override fun getViewTypeCount(): Int {
        return 1
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun hasStableIds(): Boolean {
        return true
    }
}