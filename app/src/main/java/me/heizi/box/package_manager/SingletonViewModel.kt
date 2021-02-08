package me.heizi.box.package_manager

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import me.heizi.box.package_manager.Application.Companion.DEFAULT_MOUNT_STRING
import me.heizi.box.package_manager.Application.Companion.PREFERENCES
import me.heizi.box.package_manager.models.BackupType
import me.heizi.box.package_manager.models.PreferencesMapper
import me.heizi.box.package_manager.repositories.PackageRepository

class SingletonViewModel(application: Application) : AndroidViewModel(application) {
    val preferences = PreferencesMapper(application.getSharedPreferences(PREFERENCES,Context.MODE_PRIVATE))
    val packageRepository = PackageRepository(
            viewModelScope,application,
            getMountString = {preferences.mountString?: DEFAULT_MOUNT_STRING},
            getBackupType = {
                if (preferences.isBackup!=false) {
                    try {
                        if (preferences.backupPath == null) BackupType.BackupWithPath.Default
                        else BackupType.BackupWithPath.Custom(preferences.backupPath!!)
                    }catch (e:Exception) {
                        BackupType.BackupWithOutPath
                    }
                } else BackupType.JustRemove
            },
    )

}