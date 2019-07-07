package com.example.android.simpledice.utils

import android.content.Context
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.android.simpledice.R
import kotlinx.android.parcel.Parcelize
import java.text.DateFormat
import java.util.*

@Entity(tableName = "dice_results")
@Parcelize
class DiceResults(
    @PrimaryKey(autoGenerate = true)
    var databaseId: Int? = null,
    var name: String = "",
    var descrip: String = "",
    var total: Long = 0,
    var dateCreated: Long = System.currentTimeMillis()
) : Parcelable {
    val formattedDateCreated: String
        get() {
            val dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault())
            val date = Date(dateCreated)
            return dateFormat.format(date)
        }


    /** A helper method used to save the Results to both SharedPreferences (as the latest roll) and History
     *
     * @param context used to save the results
     */
    fun saveToSharedPreferences(context: Context) {
        //Logic to add to SharedPrefs
        val sharedPreferences =
            context.getSharedPreferences(context.getString(R.string.shared_preferences_key), Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        editor.putString(context.getString(R.string.dice_results_name_key), name)
        editor.putString(context.getString(R.string.dice_results_descrip_key), descrip)
        editor.putLong(context.getString(R.string.dice_results_total_key), total)
        editor.putLong(context.getString(R.string.dice_results_date_key), dateCreated)
        editor.apply()
    }
}
