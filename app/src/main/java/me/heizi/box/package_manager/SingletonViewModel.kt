package me.heizi.box.package_manager

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import me.heizi.box.package_manager.repositories.PackageRepository

class SingletonViewModel(application: Application) : AndroidViewModel(application) {
    val packageRepository = PackageRepository(application)
}