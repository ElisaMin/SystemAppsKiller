package me.heizi.box.package_manager.utils

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


//@SuppressWarnings("INLINING_IS_INSIGNIFICANT")
inline infix fun <T> MutableStateFlow<T>.set(value:T) {
    this.value = value
}
inline infix fun <T> MutableLiveData<T>.set(value: T) {
    MainScope().launch {
        this@set.value = value
    }
}

inline fun <T> MutableLiveData<T>.unMutable() = this as LiveData<T>
inline fun <T> MutableStateFlow<T>.unMutable() = this as StateFlow<T>