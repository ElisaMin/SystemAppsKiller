package me.heizi.box.package_manager.ui.pre_config

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Unconfined
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import me.heizi.box.package_manager.Application.Companion.TAG
import me.heizi.box.package_manager.utils.set
import me.heizi.kotlinx.shell.CommandResult
import me.heizi.kotlinx.shell.CommandResult.Failed
import me.heizi.kotlinx.shell.CommandResult.Success
import me.heizi.kotlinx.shell.OneTimeExecutor.Companion.su

class PreconfigViewModel : ViewModel() {

    /**
     * START-> CheckSU-> CheckSystemWriteable-> DONE
     *     x-> SuFailed
     *              x-> Unwritable system
     */
    sealed class Status {
        object Ready : Status()
        object CheckSu:Status()
        class SuNotFound(val error:String?):Status()
        object CheckSystemWritable:Status()
        class UnwritableSystem(val result: Failed):Status()
        object Done:Status()
    }

    val status get() = _status.asSharedFlow()
    private val _status = MutableSharedFlow<Status>()

    val errorMessage get() = _errorMessage.asStateFlow()
    val text: StateFlow<String> get() = _text
    val isWaiting get() = _isWaiting.asStateFlow()
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
        _text set text
        _isWaiting set isWaiting
        _isShowingMountInput set isShowingInput
        displayingError(error)
    }
    private fun displayingError(failed: Failed) = displayingError(
        """失败!:${failed.code}
           原因:
           ${failed.errorMessage ?:"（似乎没有错误原因"}
           执行过程：
           ${failed.processingMessage.takeIf { it.isNotEmpty() } ?:"无"}
        """.trimIndent()
    )
    private fun displayingError(string: String?) { _errorMessage set string }

    private fun updateStatus(status: Status) {
        viewModelScope.launch(Unconfined) {
            _status.emit(status)
        }
    }

    /**
     * Start 被通知正式运行
     */
    suspend fun start() {
//        delay(300)
        updateStatus(Status.Ready)
        viewModelScope.launch(Unconfined) {
            status.collect(::deal)
        }
    }

    /**
     * Deal 自动化处理事件
     */
    private fun deal(status: Status) { when(status) {
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
                when(it) {
                    is Success -> Status.Done
                    is Failed-> Status.UnwritableSystem(it)
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
            // TODO: 2021/1/31 完成跳转
        }
    }}

    private inline fun testSu(crossinline block:(CommandResult)->Status) {
        viewModelScope.launch(IO) {
            updateStatus(block(su("echo hello world").await()))
        }
    }
    private inline fun testRW(crossinline block:(CommandResult)->Status) {
        // TODO: 2021/1/31 询问是否需要备份
//        val delete = ""
        val path = "/system/app/HeiziToolX"
        """ echo start
            ${mountString.value}
            if [ -e $path ];then rm -rf $path; fi
            mkdir -rf $path
            echo 0>>$path/testRw
            cat $path/testRw
            mkdir -rf $path
        """.trimIndent().let {
            viewModelScope.launch(IO) {
                updateStatus(su(it).await().let(block))
            }
        }
    }
    
    fun onInputSubmit() {
        // TODO: 2021/2/1 弹出Dialog输入或者直接输入

    }





}