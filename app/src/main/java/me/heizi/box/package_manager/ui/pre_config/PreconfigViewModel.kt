package me.heizi.box.package_manager.ui.pre_config

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Unconfined
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collect
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
    private val _flow = MutableStateFlow<Status>(Status.Ready)
    val flow = _flow.asSharedFlow()

    val isWaiting = MutableLiveData(true)
    val isShowingMountInput = MutableLiveData(false)
    val errorMessage = MutableLiveData<String?>(null)
    val mountString = MutableLiveData("")
    val noteText = MutableLiveData("欢迎使用黑字卸载器")









    private fun whiles(status:Status):Unit = when(status) {

        is Status.CheckSu -> {
            Log.i(TAG, "whiles: su")
            viewModelScope.launch(Unconfined) {
                noteText set "正在获取Root权限...."
                isWaiting set true
                _flow.emit(when (val r = checkSU()) {
                    is Success -> Status.CheckSystemWritable
                    is Failed ->  Status.SuNotFound(error = r.errorMessage?:r.processingMessage.takeIf { it.isNotEmpty() })
                })
            }
            Log.i(TAG, "whiles: emited")
            Unit
        }
        is Status.SuNotFound -> {
            Log.i(TAG, "whiles: su-failed")
            noteText set "获取Root权限失败,请退出。"
            isWaiting set false
            errorMessage set status.error
        }
        is Status.CheckSystemWritable -> {
            Log.i(TAG, "whiles: wr")
            noteText set "正在检查System是否可写入....."
            isWaiting set true
            isShowingMountInput set false
            errorMessage set null
            viewModelScope.launch(Unconfined) {
                _flow.emit(when (val r = checkSystemWritable()) {
                    is Success-> Status.Done
                    is Failed -> Status.UnwritableSystem(r)
                })
            }
            Unit
        }
        is Status.UnwritableSystem -> {
            Log.i(TAG, "whiles: wr-failed")
            noteText set "挂载失败，请在下方写入挂载指令。"
            errorMessage set status.result.errorMessage
            isWaiting set false
            isShowingMountInput set true
        }
        is Status.Done -> {
            viewModelScope.launch(Unconfined) {
                noteText set ("完成 正在进入下一步")
                currentCoroutineContext().cancel()
            }
            Unit
        }

        else -> {
            Log.i(TAG, "whiles: ready")
            Unit
        }
    }

    fun start(defaultMountString: String) {
        Log.i(TAG, "start: called")
        viewModelScope.launch(IO) {
            Log.i(TAG, "start: start")
            mountString set (defaultMountString)
        }
        viewModelScope.launch(Default) {
            _flow.emit(Status.CheckSu)
            Log.i(TAG, "start: su")
        }
        viewModelScope.launch(Default) {
            Log.i(TAG, "start: collecting")
            flow.collect(::whiles)
            Log.i(TAG, "start: collcted")
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.cancel()
    }
    fun onCheckSystemWritableClicked() = viewModelScope.launch(IO) {

        _flow.emit(Status.CheckSystemWritable)
    }

    private suspend fun checkSU() = (viewModelScope.su("echo hello world").await())
    private suspend fun checkSystemWritable():CommandResult {
        delay(30000)
        return Failed("null",null,code = 3)
    }


}