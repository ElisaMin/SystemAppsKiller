package me.heizi.box.package_manager.activities.home

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import me.heizi.box.package_manager.Application
import me.heizi.box.package_manager.R
import me.heizi.box.package_manager.activities.SettingsActivity
import me.heizi.box.package_manager.activities.home.fragments.CleanDialog
import me.heizi.box.package_manager.activities.home.fragments.ExportDialog
import me.heizi.box.package_manager.activities.home.fragments.HelpDialog
import me.heizi.box.package_manager.databinding.ActivityHomeBinding
import me.heizi.box.package_manager.repositories.PackageRepository
import me.heizi.box.package_manager.utils.clickSnackBar
import me.heizi.box.package_manager.utils.dialog
import me.heizi.box.package_manager.utils.longSnackBar
import me.heizi.box.package_manager.utils.shortToast
import kotlin.random.Random

class HomeActivity : AppCompatActivity() {

    val viewModel: HomeContainerViewModel by viewModels()
    private val binding by lazy { ActivityHomeBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        Log.i(TAG, "onCreate: called")
        binding.vm = viewModel
        binding.lifecycleOwner = this
        setSupportActionBar(binding.toolbar)
        collectUninstallResult()
        onBackPressedDispatcher.addCallback(this) { onBackBtn() }
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.home,menu)
        val search = menu.findItem(R.id.search_menu_home)
        search.actionView = SearchView(this).also {
            it.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean = false
                override fun onQueryTextChange(newText: String?): Boolean { viewModel.adapter.filter.filter(newText);return true }
            })
        }
        return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean = when(item.itemId) {
        R.id.launch_settings_menu -> {
            Intent(this,SettingsActivity::class.java).let(::startActivity)
            true
        }
        R.id.launch_export_menu -> {
            ExportDialog().show(supportFragmentManager,"export")
            true
        }
        R.id.launch_input_menu -> {
            CleanDialog().show(supportFragmentManager,"input")
            true
        }
        R.id.launch_help_menu -> {
            HelpDialog().show(supportFragmentManager,"help")
            true
        }
        R.id.search_menu_home -> {true}
        else -> false
    }
    /**
     * Collect uninstall result
     *
     * 展示这些应用
     */
    private fun collectUninstallResult() {
        lifecycleScope.launch(Dispatchers.Unconfined) {
            viewModel.packageRepository.uninstallStatues.collectLatest { s ->
                when(s) {
                    is PackageRepository.UninstallStatues.Success -> {
                        binding.longSnackBar("卸载成功!")
                        viewModel.adapter.removeAt(s.position)
                    }
                    is PackageRepository.UninstallStatues.Failed -> {
                        Log.i(Application.TAG, "onViewCreated: ${s.result}")
                        val message =
                            """|卸载失败:${s.result.code}
                               |原因:${s.result.errorMessage ?: "无"}
                               |过程:${s.result.processingMessage} 
                            """.trimMargin()
                        binding.clickSnackBar(
                            message = message, "查看详细"
                        ) {
                            dialog(
                                title = "详细",
                                message = message
                            )
                        }
                    }
                }
                viewModel.stopProcess()
            }
        }
    }

    /**
     * 有三种状态
     * 空状态，点击一次，点击第二次
     */
    private var isExit:Boolean? = null
    /**
     * On back btn
     *
     * 当第一次调用时则将空状态设置成第一次点击
     * 第一点击是会触发机制在600毫秒内调用可以进入第二次点击状态
     * 第二次点击状态时就会爆炸 但超过600毫秒没有被调用的话就会进入空状态
     */
    private fun onBackBtn() {
        if (isExit ==false) isExit = true
        else lifecycleScope.launch(Dispatchers.Main) {
            val time = Random(System.currentTimeMillis()).nextInt(20,40)
            shortToast("${time*30}毫秒内再次点击即可退出应用")
            launch(Dispatchers.Default) {
                isExit = false
                repeat(time) {
                    if (isExit == true) {
                        finish()
                    }
                    delay(30)
                }
                isExit = null
            }
        }
    }

    companion object {
        private const val TAG = "SingletonActivity"
        val Fragment.parent get() = this.requireActivity() as HomeActivity
        val Fragment.app get() = this.parent.application as Application
    }
}