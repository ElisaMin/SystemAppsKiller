package me.heizi.box.package_manager.activities.pre

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Unconfined
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.heizi.box.package_manager.Application.Companion.TAG
import me.heizi.kotlinx.android.set
import me.heizi.kotlinx.shell.CommandResult
import me.heizi.kotlinx.shell.CommandResult.Failed
import me.heizi.kotlinx.shell.CommandResult.Success
import me.heizi.kotlinx.shell.su


class PreConfigViewModel : ViewModel() {

    /**
     * START-> CheckSU-> CheckSystemWriteable-> DONE
     *     x-> SuFailed
     *              x-> Unwritable system
     */
    sealed class Status {
        object Ready : Status()
        object CheckSu: Status()
        class SuNotFound(val error:String?): Status()
        object CheckSystemWritable: Status()
        class UnwritableSystem(val result: Failed): Status()
        object Done: Status()
    }

    val status get() = _status.asSharedFlow()
    private val _status = MutableSharedFlow<Status>()

    val errorMessage        get() = _errorMessage.asStateFlow()
    val text                get() = _text.asStateFlow()
    val isWaiting           get() = _isWaiting.asStateFlow()
    val isShowingMountInput get() = _isShowingMountInput.asStateFlow()

    /**
     * Mount string 双向绑定 输入框内的文字
     */
    val mountString = MutableStateFlow("")

    private val _isWaiting = MutableStateFlow(true)
    private val _isShowingMountInput = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _text = MutableStateFlow("欢迎使用黑字卸载器")


    /**
     * Settings ui 的内容
     *
     * @param text 文字
     * @param isWaiting 是不是在等待中
     * @param isShowingInput 需要输入框吗
     * @param error 错误信息
     */
    private fun settingsUi(
        text: String = "错误！",
        isWaiting: Boolean = false,
        isShowingInput: Boolean = false,
        error: String?=null, ) {
        Log.i(TAG, "settingsUi: text:$text,wait:$isWaiting,input:$isShowingInput,error:$error")
        _text set text
        _isWaiting set isWaiting
        _isShowingMountInput set isShowingInput
        displayingError(error)
        Log.i(TAG, "settingsUi: set done")
        Log.i(TAG, "settingsUi: text:${_text.value},wait:${_isWaiting.value},input:${isShowingMountInput.value},error:${errorMessage.value}")
    }

    private fun displayingError(failed: Failed) = displayingError("""
失败!:${failed.code}
${if (errorMessage.value!=null)
"""
错误:
${errorMessage.value}
""" else ""
}
执行过程：
${failed.processingMessage.takeIf { it.isNotEmpty() } ?:"无"}
""")

    private fun displayingError(string: String?) { _errorMessage set string }

    private fun updateStatus(status: Status) {
        viewModelScope.launch(Unconfined) {
            _status.emit(status)
            Log.i(TAG, "updateStatus: ${status.javaClass.simpleName}")
            Log.i(TAG, "updateStatus: outed")
        }
    }

    private var launched = false

    /**
     * Start 被通知正式运行
     */
    suspend fun start() {
        if (!launched) {
            delay(300)
            updateStatus(Status.Ready)
            launched = false
        }
    }

    /**
     * Deal 自动化处理事件
     */
    fun deal(status: Status) { when(status) {
        is Status.Ready -> {
            Log.i(TAG, "deal: 准备中")
            updateStatus(Status.CheckSu)
        }
        is Status.CheckSu -> { //检查SU权限
            Log.i(TAG, "deal: SU")
            settingsUi(
                text = "正在检查SU权限",
                isWaiting = true
            )
            testSu {
                when (it) {
                    is Success -> Status.CheckSystemWritable
                    is Failed -> Status.SuNotFound(it.errorMessage)
                }
            }
        }
        is Status.SuNotFound -> {
            settingsUi(
                text = "Root权限获取失败，本应用失去了意义。",
                error = status.error
            )
        }
        is Status.CheckSystemWritable -> {
            settingsUi(
                    text = "正在检查/System/App/HeiziToolX/testRw路径是否可访问并写入，模拟备份删除。",
                    isWaiting = true,
            )
            testRW {
                Log.i(TAG, "deal: check result block calling")
                when (it) {
                    is Success -> Status.Done
                    is Failed -> Status.UnwritableSystem(it)
                }
            }
        }
        is Status.UnwritableSystem -> {
            settingsUi(
                text = "操作失败，请在下方的文本框输入可挂载指令。",
                isShowingInput = true
            )
            displayingError(status.result)
        }
        is Status.Done -> {
            settingsUi(
                text = "成功!",
                error = "如果卡在这里的话 重新打开一次也许可以解决???"
            )
        }
    }}

    private inline fun testSu(crossinline block:(CommandResult)-> Status) {
        viewModelScope.launch(IO) {
            updateStatus(block(su("echo hello world",dispatcher = Unconfined).await()))
        }
    }

    private fun testRW(remove:Boolean = false,block:(CommandResult)-> Status) {

        Log.i(TAG, "testRW: called")

        // TODO: 2021/1/31 询问是否需要备份


        val path = "/system/app/HeiziToolX"
        suspend fun create(): CommandResult
            = viewModelScope.su(
                "echo 正在写入",
                mountString.value,
                "chmod 777 /system",
                "chmod 777 /system/app",
                "if [ -e $path ] ;then rm -rf $path; fi ",
                "mkdir -p $path/",
                "echo 0>>$path/testRw",
                isErrorNeeding = true
            ).await()

        if (!remove) {
            Log.i(TAG, "testRW: add")
            viewModelScope.launch(IO) {
                when(val r = create()) {
                    is Success -> {
                        Log.i(TAG, "testRW: success")
                        testRW(true, block)
                        Log.i(TAG, "testRW: add called done")
                    }
                    is Failed -> {
                        Log.i(TAG, "testRW: failed")
                        updateStatus(block(r))
                        Log.i(TAG, "testRW: out of the thread")
                    }
                }
                Log.i(TAG, "testRW: done")
            }
        }else {
            Log.i(TAG, "testRW: remove")
            viewModelScope.launch(IO) {
                val mount = mountString.value
                viewModelScope.su(
                    mount,
                    "chmod 777 $path",
                    "rm -rf $path"
                ).await().let(block).let(::updateStatus)
            }
        }
    }
    
    fun onInputSubmit() {
        updateStatus(Status.CheckSystemWritable)

    }





}