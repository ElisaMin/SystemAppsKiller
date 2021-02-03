package me.heizi.box.package_manager

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import me.heizi.box.package_manager.repositories.PackageRepository

class SingletonViewModel(application: Application) : AndroidViewModel(application) {
    val packageRepository = PackageRepository(viewModelScope,application)
}