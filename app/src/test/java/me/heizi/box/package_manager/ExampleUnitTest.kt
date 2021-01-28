package me.heizi.box.package_manager

import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.reflect.KProperty

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }
}

class MutableFlowDelegateTest {


    companion object {
        var dispatcher = IO
        var scope = GlobalScope
    }

    private class StateFlowImpl<T>(
            value:T,

    ) {
        val msf = MutableStateFlow(value)

        operator fun getValue(thisRef: Any?, property: KProperty<*>) = msf.value
        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            scope.launch(dispatcher) {
                msf.emit(value)
            }
        }

        fun asFlow() = msf.asStateFlow()
    }


    @Test
    fun test() {
        var flow by StateFlowImpl("strings")
        GlobalScope.launch(IO) {
            (flow as StateFlowImpl<String>).asFlow().collect {
                println(it)
            }
        }
        GlobalScope.launch {
            flow = "another"

        }
        runBlocking {
            delay(610000)
        }

    }

}
class Delegate {

    interface NormalInterface {
        var normalParam:String
    }
    class NormalImpl:NormalInterface {
        override var normalParam: String by DataSave { "hello" }
    }
    companion object {
        fun normalFunction():NormalInterface = NormalImpl()
    }
    @Test
    fun overInterfaceWorkingStillTest() {
        normalFunction().normalParam = "working still ?"
    }

    class DataSave <T> (default:()->T) {
        val hashSet = hashSetOf(default())
        operator fun getValue(thisRef: Any?, property: KProperty<*>):T {
            println("getting $hashSet")
            return hashSet.last()
        }
        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            println("setting $value")
            hashSet.add(value)
        }
    }

    @Test
    fun testing() {
        var data by DataSave{"fuck"}
        println("get value : $data")
        data = "me"
        println("get value : $data")



    }
}