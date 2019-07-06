package com.example.android.simpledice.ui

import android.content.Intent
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import com.example.android.simpledice.R
import com.example.android.simpledice.utils.DiceRoll


class FavoriteDiceRollAdapter internal constructor(
    private val mFavoriteDiceRollClickHandler: FavoriteDiceRollClickHandler,
    private val mDeleteDiceRollClickHandler: DeleteDiceRollClickHandler
) : RecyclerView.Adapter<FavoriteDiceRollAdapter.FavoriteDiceRollViewHolder>() {

    private var mFavoriteDiceRolls: List<DiceRoll>? = null


    //Inner ViewHolder Class
    inner class FavoriteDiceRollViewHolder//Constructor
        (
        itemView: View,
        internal var mNameTextView: TextView? = null,
        internal var mDescripTextView: TextView? = null,
        internal var mMoreButton: ImageButton? = null
    ) : RecyclerView.ViewHolder(itemView), View.OnClickListener {


        init {

            mNameTextView = itemView.findViewById(R.id.favorite_name_tv)
            mDescripTextView = itemView.findViewById(R.id.favorite_descrip_tv)
            mMoreButton = itemView.findViewById(R.id.favorite_more_button)

            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            val adapterPosition = adapterPosition
            val diceRoll = mFavoriteDiceRolls!![adapterPosition]
            mFavoriteDiceRollClickHandler.onItemClick(diceRoll)
        }
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): FavoriteDiceRollViewHolder {
        val context = viewGroup.context
        val layoutIDForListItem = R.layout.favorite_recycler_item
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(layoutIDForListItem, viewGroup, false)

        return FavoriteDiceRollViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: FavoriteDiceRollViewHolder, position: Int) {
        val currentDiceRoll = mFavoriteDiceRolls!![position]

        val name = currentDiceRoll.name
        val descrip = currentDiceRoll.formula

        viewHolder.mNameTextView!!.text = name
        viewHolder.mDescripTextView!!.text = descrip

        viewHolder.mMoreButton!!.setOnClickListener { v ->
            val context = v.context
            val popup = PopupMenu(context, v)
            popup.menuInflater.inflate(R.menu.favorite_item_more_menu, popup.menu)

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_edit_favorite //Launch AddFavoriteActivity with included diceRoll
                    -> {
                        val intent = Intent(context, AddFavoriteActivity::class.java)
                        intent.putExtra(context.getString(R.string.dice_roll_parcelable_key), currentDiceRoll)
                        context.startActivity(intent)
                        true
                    }
                    R.id.action_delete_favorite -> {
                        mDeleteDiceRollClickHandler.onDeleteClick(currentDiceRoll)
                        true
                    }
                    else -> true
                }
            }
            popup.show()
        }
    }

    override fun getItemCount(): Int {
        return if (mFavoriteDiceRolls == null) 0 else mFavoriteDiceRolls!!.size
    }

    /**
     * Helper method to set FavoriteDiceRoll List to existing FavoriteDiceRollAdapter
     *
     * @param favoriteDiceRolls the new set of FavoriteDiceRolls to be displayed
     */

    fun setFavoriteDiceRolls(favoriteDiceRolls: List<DiceRoll>) {
        mFavoriteDiceRolls = favoriteDiceRolls
        notifyDataSetChanged()
    }

    //Interface to handle clicks, defined in MainActivity/FavoriteActivity
    interface FavoriteDiceRollClickHandler {
        fun onItemClick(favoriteDiceRoll: DiceRoll)
    }

    //Interface to handle delete clicks, defined in MainActivity/FavoriteActivity
    interface DeleteDiceRollClickHandler {
        fun onDeleteClick(favoriteDiceRoll: DiceRoll)
    }
}
