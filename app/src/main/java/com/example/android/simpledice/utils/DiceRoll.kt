package com.example.android.simpledice.utils

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class DiceRoll(
    var name: String = "",
    var formula: String = "",
    var hasOverHundredDice: Boolean = false
) : Parcelable
