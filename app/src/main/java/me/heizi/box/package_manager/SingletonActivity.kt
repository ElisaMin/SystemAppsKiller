package me.heizi.box.package_manager

import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import me.heizi.box.package_manager.models.PreferencesMapper

class SingletonActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SingletonActivity"
        val Fragment.parent get() = this.requireActivity() as SingletonActivity
        val Fragment.app get() = this.parent.application as Application
    }

    val viewModel:SingletonViewModel by viewModels()
    val preferences by lazy { PreferencesMapper(this) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.containner)
        Log.i(TAG, "onCreate: called")
        viewModel

    }
}