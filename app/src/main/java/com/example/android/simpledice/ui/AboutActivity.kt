package com.example.android.simpledice.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.android.simpledice.R

class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        setTitle(R.string.about)
    }
}
