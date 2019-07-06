package com.example.android.simpledice.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews

import com.example.android.simpledice.R
import com.example.android.simpledice.ui.FavoriteActivity
import com.example.android.simpledice.utils.DiceRoll
import com.example.android.simpledice.utils.Utils

import java.util.ArrayList

/**
 * Implementation of App Widget functionality.
 */
class FavoritesWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        //This diceRolls is blank, because the onUpdate method should never be called.  Rather, updates occur from the above updateAppWidget method being called by changes in the app.
        val diceRolls = ArrayList<DiceRoll>()
        updateAppWidget(context, appWidgetManager, appWidgetIds, diceRolls)

    }

    override fun onEnabled(context: Context) {

    }

    override fun onDisabled(context: Context) {

    }

    companion object {

        fun updateAppWidget(
            context: Context, appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray, diceRolls: List<DiceRoll>
        ) {
            //Iterate through all widgets and update each
            for (appWidgetId in appWidgetIds) {
                // Construct the RemoteViews object
                val views = RemoteViews(context.packageName, R.layout.favorites_widget)

                //Intent to send to ListWidgetService
                val intent = Intent(context, ListWidgetService::class.java)
                val stringDiceRolls =
                    Utils.diceRollsToString(diceRolls) //Convert diceRolls to String because parcelables don't send correctly
                intent.putExtra(context.getString(R.string.widget_dice_roll_parcelable_key), stringDiceRolls)
                intent.data = Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME))
                views.setRemoteAdapter(R.id.widget_favorites_lv, intent)
                views.setEmptyView(R.id.widget_favorites_lv, R.id.widget_empty)

                //PendingIntentTemplate to launch FavoritesActivity when a favorite is clicked
                val favoritesIntent = Intent(context, FavoriteActivity::class.java)
                val pendingIntent =
                    PendingIntent.getActivity(context, 0, favoritesIntent, PendingIntent.FLAG_UPDATE_CURRENT)
                views.setPendingIntentTemplate(R.id.widget_favorites_lv, pendingIntent)
                views.setOnClickPendingIntent(R.id.widget_empty, pendingIntent)

                // Instruct the widget manager to update the widget
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }
}
