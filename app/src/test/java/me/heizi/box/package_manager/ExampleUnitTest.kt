package me.heizi.box.package_manager

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */


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