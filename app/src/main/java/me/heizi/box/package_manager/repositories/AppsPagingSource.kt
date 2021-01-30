package me.heizi.box.package_manager.repositories

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import me.heizi.box.package_manager.Application.Companion.TAG
import me.heizi.box.package_manager.models.DisplayingData
import me.heizi.box.package_manager.models.DisplayingData.Companion.displaying
import me.heizi.box.package_manager.utils.Apps
import me.heizi.box.package_manager.utils.isUserApp


/**
 * 目标:在滑动之后加载下方的Items的图标以达到节省内存的效果
 *
 * 依赖
 * 数据源为不可变的List,排序准确 todo 当数据源更新时需要对List进行Diff,然后再通知UI
 *
 * Key是啥?
 * key决定了上下的页的内容 所以为Int
 *
 * Value是啥?
 * value拿来给屏幕展示的数据[DisplayingData]]
 *
 */
class AppsPagingSource(
        private val pm: PackageManager,
): PagingSource<Int, DisplayingData>() {

    private lateinit var source:List<ApplicationInfo>
    override fun getRefreshKey(state: PagingState<Int, DisplayingData>): Int {
        Log.i(TAG, "getRefreshKey: onCalled ${state.anchorPosition}")
        return  state.anchorPosition?.plus(1) ?: 0
    }

    
    private fun MutableList<DisplayingData>.append(page:Int, size:Int):Int? {
        Log.i(TAG, "load: 追加")

        val start = size * page
        var end = size * (page + 1)-1
        
        var nextPage:Int? = page+1
        
        //如果快完了：
        if (end >= source.size) {
            nextPage = null
            end = source.size - 1
        }
        val rage = start..end
        Log.i(TAG, "load: $start,$end,$rage",)
        //加载区域内所有的可显示信息
        //0则加标题
        //第一次时加载本次和下次的path
        //后面再进入循环 则加载下次的path即可
        var previousPath = Apps.getPreviousPath(source[start].sourceDir)
        
        if (start == 0) {
            add(DisplayingData.Header(previousPath))
        }
        for (i in rage) {
            val displaying = source[i].displaying(pm, i)
            add(displaying)
            //判断本次和下次是否为一致
            val nextPreviousPath = if (i <= source.lastIndex - 1) Apps.getPreviousPath(source[i + 1].sourceDir) else null
            //不一致时添加标题
            if (nextPreviousPath != null && previousPath != nextPreviousPath) {
                add(DisplayingData.Header(nextPreviousPath))
            }
            //然后赋值
            previousPath = nextPreviousPath ?: break
        }
        return nextPage
    }
    
    /**
     * 设计思维:(把kdoc当成了草稿纸的泻
     *
     * 目标就是 在内存中存放固定大小的可显示内容 即时加载 过时摧毁 (可能这个功能Paging已经帮我们做好了
     * 三种场景:
     * 刷新时 我不太懂刷新时是啥这个跳过
     * 上滑 这个也先慢慢来 跳过
     * 下滑 需求往下
     *
     */
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, DisplayingData> {
        val thisKey = params.key ?: 0
        Log.i(TAG, "load: this key $thisKey")
        val list = ArrayList<DisplayingData>()

        var nextKey:Int? = thisKey+1

        
        when(params) {
            /**
             * 获取新的List并执行Append操作
             */
            is LoadParams.Refresh -> {
                Log.i(TAG, "load: 刷新中")
                source = Apps(pm).asSortedList
                    .filter { !it.isUserApp }
                nextKey =  list.append(thisKey,params.loadSize)
                Log.i(TAG, "load: 刷新完成")
            }
            is LoadParams.Append -> {
                nextKey =  list.append(thisKey,params.loadSize)

            }
            is LoadParams.Prepend -> {
                Log.i(TAG, "load: 向前")
            }
        }

        Log.i(TAG, "load: result size ${list.size}")
        return LoadResult.Page(list,thisKey.takeIf { it != 0 } ,nextKey,)
    }

}

