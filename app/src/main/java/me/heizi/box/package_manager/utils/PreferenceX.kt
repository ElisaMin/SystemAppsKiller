package me.heizi.box.package_manager.utils

import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import me.heizi.box.package_manager.Application.Companion.TAG
import java.util.*
import kotlin.reflect.KProperty

/**
 * 一个方便快捷的轮子
 */

open class PreferenceMapped (
    private val sp:SharedPreferences
):LifecycleObserver {
    /** 使用HashMap存储 */
    private var _hashMap:HashMap<String,Any?>? = HashMap<String,Any?>()
    val hashMap get() = _hashMap!!
    /** 监听改变事件 */
    private val onChange = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, _ ->
        hashMap.putAll(sharedPreferences.all)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onStart() {
        hashMap.putAll(sp.all)
        sp.registerOnSharedPreferenceChangeListener(onChange)
    }
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        sp.unregisterOnSharedPreferenceChangeListener(onChange)
        _hashMap = null
    }

    fun<T:Any?> named(key: String) = Map<T>(key)

    @Suppress("UNCHECKED_CAST")
    inner class Map<T:Any?>(private var key:String) {
        operator fun getValue(thisRef: PreferenceMapped, property: KProperty<*>): T {
            return hashMap[key] as T
        }
        operator fun setValue(thisRef: PreferenceMapped, property: KProperty<*>, value: T) {
            Log.i(TAG, "setValue: setting $key $value")
            GlobalScope.launch(Default) {
                sp.edit(commit = true) {
                    when (value) {
                        is Int -> putInt(key, value)
                        is String -> putString(key, value)
                        is Boolean -> putBoolean(key, value)
                        is Float -> putFloat(key, value)
                        is Long -> putLong(key, value)
                        is MutableSet<*> -> putStringSet(key, value as MutableSet<String>)
                        else -> putString(key, value.toString())
                    }
                }
                onChange.onSharedPreferenceChanged(sp,key)
                Log.i(TAG, "setValue: done! $key")
            }
        }

    }

}