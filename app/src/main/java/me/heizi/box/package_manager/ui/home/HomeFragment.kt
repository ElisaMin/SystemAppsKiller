package me.heizi.box.package_manager.ui.home

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.addCallback
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Dispatchers.Unconfined
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import me.heizi.box.package_manager.Application.Companion.TAG
import me.heizi.box.package_manager.R
import me.heizi.box.package_manager.SingletonActivity.Companion.parent
import me.heizi.box.package_manager.databinding.HomeFragmentBinding
import me.heizi.box.package_manager.repositories.PackageRepository
import me.heizi.box.package_manager.utils.clickSnackBar
import me.heizi.box.package_manager.utils.dialog
import me.heizi.box.package_manager.utils.longSnackBar
import me.heizi.box.package_manager.utils.shortToast
import kotlin.random.Random

class HomeFragment : Fragment(R.layout.home_fragment) {
    private val viewModel:HomeViewModel by viewModels {
        object: ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return modelClass.getConstructor(PackageRepository::class.java).newInstance(parent.viewModel.packageRepository)
            }
        }
    }
    private val binding by lazy { HomeFragmentBinding.bind(requireView()) }

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
        else lifecycleScope.launch(Main) {
            val time = Random(System.currentTimeMillis()).nextInt(20,40)
            context?.shortToast("${time*30}毫秒内再次点击即可退出应用")
            launch(Default) {
                isExit = false
                repeat(time) {
                    if (isExit == true) {
                        parent.finish()
                    }
                    delay(30)
                }
                isExit = null
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            onBackBtn()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch(Unconfined) {
            parent.viewModel.packageRepository.uninstallStatues.collectLatest { s ->
                when(s) {
                    is PackageRepository.UninstallStatues.Success -> {
                        binding.longSnackBar("卸载成功!")
                        viewModel.adapter.removeAt(s.position)
                    }
                    is PackageRepository.UninstallStatues.Failed -> {
                        Log.i(TAG, "onViewCreated: ${s.result}")
                        val message =
                            """|卸载失败:${s.result.code}
                               |原因:${s.result.errorMessage ?: "无"}
                               |过程:${s.result.processingMessage} 
                            """.trimMargin()
                        binding.clickSnackBar(
                            message = message, "查看详细"

                        ) {
                            requireContext().dialog(
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


    override fun onResume() {
        super.onResume()
        binding.vm = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        parent.setSupportActionBar(binding.toolbar)
        Log.i(TAG, "onResume: home fragment")
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.home,menu)
        val search = menu.findItem(R.id.search_menu_home)
        search.actionView = getSearchView()
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.launch_settings_menu -> {
                findNavController().navigate(R.id.action_homeFragment_to_settingsFragment)
//                findNavController().navigateUp()
                true
            }
            R.id.launch_export_menu -> {
                findNavController().navigate(R.id.action_homeFragment_to_exportFragment)
//                findNavController().navigateUp()
                true
            }
            R.id.launch_input_menu -> {
                findNavController().navigate(R.id.action_homeFragment_to_cleanFragment)
//                findNavController().navigateUp()
                true
            }
            R.id.launch_help_menu -> {
                findNavController().navigate(R.id.action_homeFragment_to_helpFragment)
                true
            }
            R.id.search_menu_home -> {true}
            else -> false
        }
    }



    /**
     * Get search view
     *
     * 获取可过滤的search view
     */
    private fun getSearchView ():SearchView{
        val searchView = SearchView(requireContext())
        searchView.setOnQueryTextListener(object :SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.adapter.filter.filter(newText)
                return true
            }
        })
        return searchView
    }


}