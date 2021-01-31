package me.heizi.box.package_manager.utils

import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
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