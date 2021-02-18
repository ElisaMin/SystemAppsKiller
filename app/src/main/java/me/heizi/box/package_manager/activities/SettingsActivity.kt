package me.heizi.box.package_manager.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentContainerView
import androidx.preference.PreferenceFragmentCompat
import me.heizi.box.package_manager.Application
import me.heizi.box.package_manager.R

class SettingsActivity : AppCompatActivity() {

    private val settingFragment = SettingsFragment()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(getFragmentContainerView(savedInstanceState))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
    private fun getFragmentContainerView(savedInstanceState: Bundle?):FragmentContainerView{
        val id = 1145141145
        val fragment = FragmentContainerView(this)
        fragment.id = id
        if (savedInstanceState == null) supportFragmentManager.beginTransaction().replace(id,settingFragment).commit()
        return fragment
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            preferenceManager.sharedPreferencesName = Application.PREFERENCES
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
        }
    }
}