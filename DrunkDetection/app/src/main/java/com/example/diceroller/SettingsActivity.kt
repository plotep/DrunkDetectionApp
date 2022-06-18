package com.example.diceroller

import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.text.InputType
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.*


class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        if (savedInstanceState == null) {
            supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.settings, SettingsFragment())
                    .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            val mapTypeString = preferences.getString("edit_emergency_number", "Empty")
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
            preferenceManager.findPreference<EditTextPreference>("edit_emergency_number")?.setOnBindEditTextListener { editText -> editText.filters = arrayOf<InputFilter>(LengthFilter(11))  ; editText.inputType = InputType.TYPE_CLASS_NUMBER }
        }


    }

}