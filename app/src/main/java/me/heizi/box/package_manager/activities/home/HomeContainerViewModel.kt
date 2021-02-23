package me.heizi.box.package_manager.activities.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.heizi.box.package_manager.Application.Companion.DEFAULT_MOUNT_STRING
import me.heizi.box.package_manager.Application.Companion.app
import me.heizi.box.package_manager.activities.home.adapters.UninstallApplicationAdapter
import me.heizi.box.package_manager.models.BackupType
import me.heizi.box.package_manager.repositories.PackageRepository
import me.heizi.box.package_manager.utils.set

class HomeContainerViewModel(application: Application) : AndroidViewModel(application) {

    val preferences = app.preferenceMapper
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
    val adapter by lazy {
        UninstallApplicationAdapter(
            viewModelScope,
            packageRepository.defaultAdapterService,
            stopProcessing = {_processing set  false},
            processing = { _processing set true }
        )
    }
    val processing get() = _processing.asStateFlow()


    private val _processing = MutableStateFlow(true)

    init {
    }
    fun stopProcess() {
        _processing set false
    }
    fun startProgress() {
        _processing set true
    }

}