package me.heizi.kotlinx.shell

import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.heizi.kotlinx.shell.CommandResult.Companion.waitForResult
import org.junit.Test
import java.util.*
import kotlin.system.exitProcess




//fun main() {
//    var isRunning = true
//    GlobalScope.launch {
//        oneTimeExecutor("adb wait-for-devices",prefix = arrayOf("cmd")).waitForResult().let {
//            when(it) {
//                is CommandResult.Success -> {
//                    println(it.message)
//                }
//            }
//            println("exiting")
//            isRunning  = false
//        }
//        println("out of frame")
//    }
//    while (isRunning) println("running still")
//    exitProcess(0)
//
//}
class TestOnWindows {
    val runnable get() = oneTimeExecutor("echo pause","echo.","set /p a=",prefix = arrayOf("cmd"))

    val times get() = Date().time

    @Test
    fun waitingForResult() = runBlocking{
        var time = times
        fun time() {
            times.let {
                println(
                    it - time
                )
                time = it
            }
        }

        println("waiting")
        time()
        runnable.waitForResult().let {
            println("got the result")
            time()
            println(it)
        }
        time()
    }
    @Test
    fun runningWithResult() = runBlocking{
        runnable.onCompletion {
            println("done")
        }.collect {
            println(it)
            cancel()
        }
    }
}