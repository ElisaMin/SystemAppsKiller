package me.heizi.kotlinx.shell

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import me.heizi.kotlinx.shell.CommandResult.Companion.waitForResult
import java.util.concurrent.Executors

/**
 * 执行一些立即获取结果的指令 无需进入下一次的loop
 *
 * 观察生命周期的摧毁事件释放内存
 * @param scope such as like [ViewModel.getViewModelScope] or [Activity.getLifecyclerScpope]
 */
class OneTimeExecutor(
    scope: CoroutineScope
):LifecycleObserver {

    companion object {
        private var _instance:OneTimeExecutor? = null
        val instance get() = _instance!!
        fun getInstance(scope: CoroutineScope):OneTimeExecutor {
            if (_instance == null) {
                _instance = OneTimeExecutor(scope)
            }
            return _instance!!
        }

        /**
         * 使用SU异步执行Shell[commandLines]
         *
         * @return [Deferred]
         */
        fun CoroutineScope.su(
            vararg commandLines: String,
            dispatcher: CoroutineDispatcher = Dispatchers.IO
        ): Deferred<CommandResult> = async(dispatcher) {
            getInstance(this)
                .run(commandLines = commandLines,arrayOf("su"))
                .waitForResult()
        }
        /**
         * 使用sh异步执行Shell[commandLines]
         *
         * @return [Deferred]
         */
        fun CoroutineScope.sh(
            vararg commandLines: String,
            dispatcher: CoroutineDispatcher = Dispatchers.IO
        ): Deferred<CommandResult> = async(dispatcher) {
            getInstance(this)
                .run(commandLines = commandLines,arrayOf("sh"))
                .waitForResult()
        }

    }

    private val pool by lazy { Executors.newFixedThreadPool(4) }
    private val dispatcher: CoroutineDispatcher by lazy { pool.asCoroutineDispatcher() }
    private val scope by lazy { scope + dispatcher }

//    @JvmName("run1")
//    fun run(vararg commandLines:String, prefix: Array<String>) = run(commandLines, prefix)

    /**
     *
     *
     * @param commandLines 所要丢给shell的指令
     * @param prefix 决定了以哪种形式打开这个解释器
     * @return
     */
    @Suppress( "BlockingMethodInNonBlockingContext")
    fun run(
        commandLines: Array<out String>,
        prefix:Array<String> = arrayOf("sh")
    ): Flow<ProcessingResults> {
        val flow = MutableSharedFlow<ProcessingResults>()
        val process = Runtime.getRuntime().exec(prefix)
        val waitQueue = Array(3) {false}

        scope.launch(dispatcher) {
            process.outputStream.writer().let {
                for( i in commandLines) {
                    it.write(i)
                    it.write("\n")
                    it.flush()
                }
                process.outputStream.runCatching {
                    close()
                }
                waitQueue[0] = true
            }
        }
        scope.launch(dispatcher) {
            process.inputStream.bufferedReader().lineSequence().forEach {
                flow.emit(ProcessingResults.Message(it))
            }
            waitQueue[1] = true
        }
        scope.launch(dispatcher) {
            process.errorStream.bufferedReader().lineSequence().forEach {
                flow.emit(ProcessingResults.Error(it))
            }
            waitQueue[2] = true
        }
        scope.launch(dispatcher) {
            //等待执行完成
            while (waitQueue.contains(false)) delay(1)
            flow.emit(ProcessingResults.CODE(process.waitFor()))
            kotlin.runCatching {
                process.inputStream.close()
                process.errorStream.close()
                process.destroy()
            }
            flow.emit(ProcessingResults.Closed)
        }
        return flow
    }
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private fun destroy() {
        dispatcher.cancelChildren()
        dispatcher.cancel()
        pool.shutdown()
    }

}