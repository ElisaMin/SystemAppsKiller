package me.heizi.box.package_manager.ui.settings

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import me.heizi.box.package_manager.R

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }
}