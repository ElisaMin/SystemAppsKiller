package me.heizi.box.package_manager.utils

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

inline fun LifecycleOwner.main(crossinline block: suspend CoroutineScope.()->Unit) {lifecycleScope.launch(Dispatchers.Main) {block()}}
inline fun LifecycleOwner.io(crossinline block: suspend CoroutineScope.()->Unit) {lifecycleScope.launch(Dispatchers.IO) {block()}}
inline fun LifecycleOwner.default(crossinline block: suspend CoroutineScope.()->Unit) {lifecycleScope.launch(Dispatchers.Default) {block()}}
inline fun LifecycleOwner.unconfined(crossinline block: suspend CoroutineScope.()->Unit) {lifecycleScope.launch(Dispatchers.Unconfined) {block()}}