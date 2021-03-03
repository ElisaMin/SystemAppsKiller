package me.heizi.box.package_manager.log

import android.content.Context
import android.util.Log
import androidx.room.Room

object Log {
    private const val TAG = "heizi.Log"
    private const val I = "INFO"
    private const val E = "ERROR"
    private lateinit var instance:LogHouse
    var isWriteToDatabase = false
    fun registered(context: Context) {
        instance = Room.databaseBuilder(context,LogHouse::class.java,"logs").allowMainThreadQueries().build()
    }
    fun i(message:String) {
        Log.i(TAG, message)
        instance.manager.save(message,null, I)
    }
    fun Exception.log() {
        Log.e(TAG, "error", this)
        if (isWriteToDatabase) instance.manager.save(message = message,process = stackTrace.joinToString(";"),level = E)
    }
}