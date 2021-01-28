package me.heizi.box.package_manager.utils

import kotlinx.coroutines.flow.MutableStateFlow

inline infix fun <T> MutableStateFlow<T>.set(value:T) {
    this.value = value
}