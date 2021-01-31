package me.heizi.box.package_manager.utils

import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import me.heizi.box.package_manager.Application.Companion.TAG
import kotlin.reflect.KProperty

/**
 * Preference mapped
 *
 * @property sp
 * @constructor Create empty Preference mapped
 */
open class PreferenceMapped (
    private val sp:SharedPreferences
) {
    /** 使用HashMap存储 */
    val hashMap = HashMap<String,Any?>()
    /** 监听改变事件 */
    private val onChange = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, _ ->
        hashMap.putAll(sharedPreferences.all)
    }

    init {
        hashMap.putAll(sp.all)
        sp.registerOnSharedPreferenceChangeListener(onChange)
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