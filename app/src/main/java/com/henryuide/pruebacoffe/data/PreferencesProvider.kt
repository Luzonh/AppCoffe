package com.henryuide.pruebacoffe.data

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager


object PreferencesProvider {
    fun getSecondsRemaining(context: Context): Boolean {
        return prefs(context).getBoolean(SharedPrefHelper.ON_BOARDING.value, false)
    }

    fun setSecondsRemaining(context: Context, value: Boolean) {
        val editor = prefs(context).edit()
        editor.putBoolean(SharedPrefHelper.ON_BOARDING.value, value).apply()
    }

    private fun prefs(context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }
}