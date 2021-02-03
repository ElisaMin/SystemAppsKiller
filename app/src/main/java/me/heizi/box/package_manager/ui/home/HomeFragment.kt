package me.heizi.box.package_manager.ui.home

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import me.heizi.box.package_manager.Application.Companion.TAG
import me.heizi.box.package_manager.R
import me.heizi.box.package_manager.SingletonActivity.Companion.parent
import me.heizi.box.package_manager.databinding.HomeFragmentBinding

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
        parent.setSupportActionBar(binding.toolbar)
        viewModel.start(parent.viewModel.packageRepository,parent.preferences)
        binding.homeList.adapter = viewModel.adapter.withLoadStateFooter(getListBottomLoadingView())
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.home,menu)
        val search = menu.findItem(R.id.search_menu_home)
        search.actionView = getSearchView()
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return super.onOptionsItemSelected(item)
    }


    val textView by lazy { TextView(context) }
    val viewHolder by lazy { object :RecyclerView.ViewHolder(textView) {} }
    private fun getListBottomLoadingView():LoadStateAdapter<RecyclerView.ViewHolder> {
        return object  :LoadStateAdapter<RecyclerView.ViewHolder>() {
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, loadState: LoadState) {
                textView.text = loadState.toString()
                Log.i(TAG, "onBindViewHolder: $loadState")
            }
            override fun onCreateViewHolder(
                parent: ViewGroup,
                loadState: LoadState
            ): RecyclerView.ViewHolder {
                Log.i(TAG, "onCreateViewHolder: $loadState")
                return viewHolder
            }

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
                viewModel.filter.filter(newText)
                return true
            }
        })
        return searchView
    }

    /**
     * On uninstall failed
     *
     * 在卸载失败时弹出SnackBar 提示错误
     */
    private fun onUninstallFailed() {
    }
    private fun showMessageThatDismissAfterClick(errorMessage:String ) {
        errorMessage
    }

}