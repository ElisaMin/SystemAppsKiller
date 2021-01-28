package me.heizi.box.package_manager.utils

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
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
            GlobalScope.launch(IO) {
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
            }
        }
    }

}