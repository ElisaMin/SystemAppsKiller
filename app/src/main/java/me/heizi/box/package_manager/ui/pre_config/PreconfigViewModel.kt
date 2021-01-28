package me.heizi.box.package_manager.ui.pre_config

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import me.heizi.box.package_manager.utils.set
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
        object CheckSu:Status()
        class SuNotFound(val error:String?):Status()
        object CheckSystemWritable:Status()
        class UnwritableSystem(val result: Failed):Status()
        object Done:Status()
    }
    private val _flow = MutableSharedFlow<Status>()
    val flow = _flow.asSharedFlow()

    val isWaiting = MutableStateFlow(true)
    val isShowingMountInput = MutableStateFlow(false)
    val errorMessage = MutableStateFlow<String?>(null)
    val mountString = MutableStateFlow("")
    val noteText = MutableStateFlow("欢迎使用黑字卸载器")


    fun start(defaultMountString: String) {
        viewModelScope.launch(IO) {
            mountString.emit(defaultMountString)
            flow.collect { status ->
                when(status) {
                    is Status.CheckSu -> {
                        noteText set "正在获取Root权限...."
                        isWaiting set true
                        _flow.emit(when (val r = checkSU()) {
                            is Success -> Status.CheckSystemWritable
                            is Failed ->  Status.SuNotFound(error = r.errorMessage?:r.processingMessage.takeIf { it.isNotEmpty() })
                        })
                    }
                    is Status.SuNotFound -> {
                        noteText set "获取Root权限失败,请退出。"
                        isWaiting set false
                        errorMessage set status.error
                    }
                    is Status.CheckSystemWritable -> {
                        noteText set "正在检查System是否可写入....."
                        isWaiting set true
                        isShowingMountInput set false
                        errorMessage set null
                        _flow.emit(when (val r = checkSystemWritable()) {
                            is Success-> Status.Done
                            is Failed -> Status.UnwritableSystem(r)
                        })
                    }
                    is Status.UnwritableSystem -> {
                        noteText set "挂载失败，请在下方写入挂载指令。"
                        errorMessage set status.result.errorMessage
                        isWaiting set false
                        isShowingMountInput set true
                    }
                    is Status.Done -> {
                        noteText.emit("完成 正在进入下一步")
                        cancel()
                    }
                }
            }
        }
    }

    override fun onCleared() {

        super.onCleared()
    }
    fun onCheckSystemWritableClicked() = viewModelScope.launch(IO) {
        _flow.emit(Status.CheckSystemWritable)
    }

    private suspend fun checkSU() = (viewModelScope.su("echo hello world").await())
    private suspend fun checkSystemWritable() = viewModelScope.su(mountString.value).await()


}