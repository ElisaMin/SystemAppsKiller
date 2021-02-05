package me.heizi.box.package_manager.ui.home

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.Dispatchers.Unconfined
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import me.heizi.box.package_manager.R
import me.heizi.box.package_manager.SingletonActivity.Companion.parent
import me.heizi.box.package_manager.databinding.HomeFragmentBinding
import me.heizi.box.package_manager.utils.clickSnackBar
import me.heizi.box.package_manager.utils.longSnackBar

class HomeFragment : Fragment(R.layout.home_fragment) {
    private val viewModel:HomeViewModel by viewModels()
    private val binding by lazy { HomeFragmentBinding.bind(requireView()) }

    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        findNavController().backStack.clear()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.vm = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        parent.setSupportActionBar(binding.toolbar)
        viewModel.start(parent.viewModel.packageRepository,parent.preferences)
        lifecycleScope.launch(Unconfined) {
            viewModel.uninstallStatues.collectLatest { s ->
                when(s) {
                    is HomeViewModel.UninstallStatues.Success -> {
                        binding.longSnackBar("卸载成功!")
                    }
                    is HomeViewModel.UninstallStatues.Failed -> {
                        binding.clickSnackBar(
                            message =
                            """|卸载失败:${s.result.code}
                               |原因:${s.result.errorMessage ?:"无"}
                               |过程:${s.result.processingMessage} 
                            """.trimMargin()
                        ) {
                            it.isVisible = false
                        }
                    }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.home,menu)
        val search = menu.findItem(R.id.search_menu_home)
        search.actionView = getSearchView()
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return super.onOptionsItemSelected(item)
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