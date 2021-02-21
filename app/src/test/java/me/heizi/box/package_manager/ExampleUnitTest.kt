package me.heizi.box.package_manager

import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */

class Coroutine {
    @Test
    fun join() {
        val time = System.currentTimeMillis()
        fun seeTime(string: String) { println("$string ${System.currentTimeMillis()-time}ms")}
        seeTime("task before")
        val task = GlobalScope.launch(IO) {
            seeTime("task inside")
            launch(Default) {
                seeTime("sub job inside")
                delay(4000)
                seeTime("sub job done")
            }.join()
            seeTime("sub one launched")
            delay(3000)
            seeTime("task done")
        }
        while (task.isActive) Unit
    }
}

//class Flows {
//    @Test
//    fun sharedFlowStop() {
//        val sharedFlow = MutableSharedFlow<Int>()
//        val time = System.currentTimeMillis()
//        fun seeTime(string: String) { println("$string ${System.currentTimeMillis()-time}ms")}
//        var runing = true
//        seeTime("starting ")
//        GlobalScope.launch(Default) {
//            seeTime("inside another thread")
//            launch(IO) {
//                seeTime("starting emit ")
//                repeat(100) {i->
//                    seeTime("emit $i | ")
//                    sharedFlow.emit(i)
//                }
//                cancel()
//            }
////            sharedFlow.shareIn(this,started = sharedFlow.).takeWhile{
////                it<99
////            }.collect {
////                seeTime("collect $it | ")
////            }
//            runing = false
//        }
//        seeTime("thread launched waiting for thread die")
//        while (runing) Unit
//        seeTime("main thread done")
//    }
//}
//class Sorted {
//
//    class Incomparable(
//        val key :String,
//        val other:String = Random(15).nextDouble().hashCode().toString()
//    ) {
//        companion object {
//            val list get() = arrayListOf(Incomparable("zgaddsadgghjhnhdjk"),Incomparable("adffgfgaffa"),Incomparable("dgfszfghhhh"))
//        }
//        override fun toString(): String {
//            return "Incomparable(key='$key', other='$other')"
//        }
//    }
//    @Test
//    fun incomparable() {
//        Incomparable.list.let {
//            println(it)
//            it.sortBy { it.key }
//            println(it)
//        }
//    }
//
//    @Test
//    fun stringListTest() {
//        arrayListOf("a","b","d","aabc").let {
//            println(it)
//            println(it.sort())
//            println(it) // running will  [a, aabc, b, d]
//        }
//    }
//}
//
//class ExampleUnitTest {
//    @Test
//    fun addition_isCorrect() {
//        assertEquals(4, 2 + 2)
//    }
//}
//
//class PackageManagers {
//    @Test
//    fun packageManagerFlags() {
//        println(PackageManager.GET_ACTIVITIES)
//        println(PackageManager.MATCH_SYSTEM_ONLY)
//        println(PackageManager.MATCH_SYSTEM_ONLY and PackageManager.GET_ACTIVITIES)
//    }
//}
//
//class MutableFlowDelegateTest {
//
//
//    companion object {
//        var dispatcher = IO
//        var scope = GlobalScope
//    }
//
//    private class StateFlowImpl<T>(
//            value:T,
//
//    ) {
//        val msf = MutableStateFlow(value)
//
//        operator fun getValue(thisRef: Any?, property: KProperty<*>) = msf.value
//        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
//            scope.launch(dispatcher) {
//                msf.emit(value)
//            }
//        }
//
//        fun asFlow() = msf.asStateFlow()
//    }
//
//
//    @Test
//    fun test() {
//        var flow by StateFlowImpl("strings")
//        GlobalScope.launch(IO) {
//            (flow as StateFlowImpl<String>).asFlow().collect {
//                println(it)
//            }
//        }
//        GlobalScope.launch {
//            flow = "another"
//
//        }
//        runBlocking {
//            delay(610000)
//        }
//
//    }
//
//}
//class Delegate {
//
//    interface NormalInterface {
//        var normalParam:String
//    }
//    class NormalImpl:NormalInterface {
//        override var normalParam: String by DataSave { "hello" }
//    }
//    companion object {
//        fun normalFunction():NormalInterface = NormalImpl()
//    }
//    @Test
//    fun overInterfaceWorkingStillTest() {
//        normalFunction().normalParam = "working still ?"
//    }
//
//    class DataSave <T> (default:()->T) {
//        val hashSet = hashSetOf(default())
//        operator fun getValue(thisRef: Any?, property: KProperty<*>):T {
//            println("getting $hashSet")
//            return hashSet.last()
//        }
//        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
//            println("setting $value")
//            hashSet.add(value)
//        }
//    }
//
//    @Test
//    fun testing() {
//        var data by DataSave{"fuck"}
//        println("get value : $data")
//        data = "me"
//        println("get value : $data")
//
//
//
//    }
//}