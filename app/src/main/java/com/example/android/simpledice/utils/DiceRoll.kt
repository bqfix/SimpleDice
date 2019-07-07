package com.example.android.simpledice.utils

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize

@Entity(tableName = "dice_rolls")
@Parcelize
class DiceRoll(
    @PrimaryKey(autoGenerate = true)
    var databaseId: Int? = null,
    var name: String = "",
    var formula: String = "",
    var hasOverHundredDice: Boolean = false
) : Parcelable
