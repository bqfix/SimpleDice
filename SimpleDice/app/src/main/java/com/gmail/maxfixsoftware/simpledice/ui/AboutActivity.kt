package com.gmail.maxfixsoftware.simpledice.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.gmail.maxfixsoftware.simpledice.R
import kotlinx.android.synthetic.main.toolbar.*

class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        setTitle(R.string.about)
    }
}
