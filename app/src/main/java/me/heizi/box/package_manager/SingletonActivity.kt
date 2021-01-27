package me.heizi.box.package_manager

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class SingletonActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.containner)
    }
}