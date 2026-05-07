package com.henryuide.pruebacoffe.settings

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.henryuide.pruebacoffe.R

class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.about_container, AboutFragment())
            .commit()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

}