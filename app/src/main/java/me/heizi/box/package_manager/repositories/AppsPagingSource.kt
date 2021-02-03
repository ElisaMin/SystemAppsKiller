package me.heizi.box.package_manager.repositories

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import me.heizi.box.package_manager.Application.Companion.TAG
import me.heizi.box.package_manager.models.DisplayingData
import me.heizi.box.package_manager.models.DisplayingData.Companion.displaying
import me.heizi.box.package_manager.repositories.PackageRepository.Companion.diffPreviousPathAreNotSame
import me.heizi.box.package_manager.repositories.PackageRepository.Companion.getPreviousPath


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
 * todo 添加可过滤功能
 */
class AppsPagingSource(
        private val pm: PackageManager,
        private val source:List<ApplicationInfo>,
): PagingSource<Int, DisplayingData>() {


    init {

    }

    override fun getRefreshKey(state: PagingState<Int, DisplayingData>): Int {
        val result = state.anchorPosition?.let {
            (it/state.config.pageSize)
        }?:0
        Log.i(TAG, "getRefreshKey: positions${state.anchorPosition};page:$result")
        return  result
    }

    private suspend fun MutableList<DisplayingData>.prepend(page: Int,size: Int):Int?{
        Log.i(TAG, "load: 向前")
        if (page==0) return null
        //如果size 10 page 3的话那就是 21-30之间的index 所以开始是((s-1)*p)+1 结尾是(s*p)
        val start = ((page-1)*size)+1
        val end = size*page
        show(start..end)
        return page -1
    }

    private suspend fun MutableList<DisplayingData>.show(intRange: IntRange) {
        //进入循环前准备
        Log.i(TAG, "show: $intRange")
        //获取当前起始的application
        var thatApplicationInfo = source[intRange.first]
        //获取当前的前路径
        var now = getPreviousPath(thatApplicationInfo.sourceDir)
        //如果开始的话直接添加
        if (intRange.first == 0) {
            add(DisplayingData.Header(now))
        }
        for (i in intRange) {
            Log.i(TAG, "show: $i")
            //添加本应用
            val app = thatApplicationInfo.displaying(pm,i)
            add(app)
            //获取下一个的前路径 如果到底了就直接跳过
            if (i<source.size-1) {
                thatApplicationInfo = source[i+1]
                val next = getPreviousPath(thatApplicationInfo.sourceDir)
                //对比
                val isNotSameShortPath = now.diffPreviousPathAreNotSame(next)
                //前路径不等添加标题
                if (isNotSameShortPath) add(DisplayingData.Header(next))
                //给下一次用
                now=next
            }
        }
    }
    
    private suspend fun MutableList<DisplayingData>.append(page:Int, size:Int):Int? {
        Log.i(TAG, "load: 追加")

        val start = size * page
        var end = (size * (page + 1))-1
        
        var nextPage:Int? = page+1
        
        //如果快完了：
        if (end >= source.size) {
            nextPage = null
            end = source.size - 1
        }
        show(start..end)

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
     * 以上无法实现 就这样 嗯
     *
     */
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, DisplayingData> {
        val list = ArrayList<DisplayingData>()

        val thisKey = params.key ?: 0
        Log.i(TAG, "load: this key $thisKey")

        var nextKey:Int? = thisKey+1
        var prevKey:Int? = thisKey-1
        if (thisKey ==0) prevKey = null

        when(params) {
            /**
             * 获取新的List并执行Append操作
             */
            is LoadParams.Refresh -> {
                Log.i(TAG, "load: 刷新中")
                nextKey =  list.append(thisKey,params.loadSize)
                Log.i(TAG, "load: 刷新完成")
            }
            is LoadParams.Append -> {
                nextKey =  list.append(thisKey,params.loadSize)
                prevKey = thisKey
            }
            is LoadParams.Prepend -> {

                prevKey = list.prepend(thisKey,params.loadSize)
                nextKey = thisKey
            }
        }
        Log.i(TAG, "load: result size ${list.size}")

        return LoadResult.Page(list,prevKey ,nextKey,)
    }

}


