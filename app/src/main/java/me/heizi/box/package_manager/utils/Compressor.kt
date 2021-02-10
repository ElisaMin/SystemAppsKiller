package me.heizi.box.package_manager.utils

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import me.heizi.box.package_manager.models.JsonContent
import me.heizi.box.package_manager.models.UninstallInfo
import me.heizi.box.package_manager.models.VersionConnected
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * 一个JSON文件使用GZIP进压缩后用Base64进行编码，在网络传播。
 * 源格式：{v:1,d:**any char on it*}
 */
object Compressor {
    private const val V1 = 1
    private val usefulFormat = """\{v:\d,d:.+\}""".toRegex()

    private const val TAG = "UninstallerCoding"

    private const val KEY_NAME = "n"
    private const val KEY_PACKAGE = "p"
    private const val KEY_SOURCE = "s"
    private const val KEY_VERSION = "v"
    private const val KEY_DATA = "d"
    private const val KEY_IS_BACKUP = "b"
    private const val KEY_CREATE_TIME ="t"
    private const val KEY_ALL_APPS ="a"

    private fun notUsefulFormat():Nothing { throw IllegalArgumentException("数据损坏或非本应用支持的格式") }
    private fun <E:Exception> notUsefulFormat(e:E):Nothing { throw IllegalArgumentException("数据损坏或非本应用支持的格式",e) }
    private fun versionNotMatch():Nothing { throw IllegalArgumentException("该卸载方案的版本不属于本版本的软件可解析的范围,请适当升级或降级。") }

    /**
     * 把正在分享的可复制文本decode出去
     *
     * @param source 类似于这样的格式: {v:1,d:anyway}
     * @return
     */
    fun buildJson(source:String): JsonContent {
        if (!source.matches(usefulFormat)) notUsefulFormat()
        val (version, data) = decodeVersionAndDataInfo(source)
        return when(version) {
            V1 -> readDataV1(data)
            else -> versionNotMatch()
        }
    }

    /**
     * 对GZIP经过BASE64编码后的字符串进行解码
     *
     * 和[compressAndEncodeByBase64]是一对双胞胎
     * @self 字符串->GZIP->BASE64
     * @return BASE64->GZIP->字符串
     */
    private fun String.decodeBase64AndGzipUncompress():String {
        return Base64.getDecoder().decode(this).let { compressData->
            GZIPInputStream(compressData.inputStream()).bufferedReader().readText()
        }
    }

    /**
     * 对字符压缩后编码
     *
     * @self SOURCE
     * @return SOURCE->GZIP->BASE64->RESULT
     */
    private fun String.compressAndEncodeByBase64():String {
        val compressed = ByteArrayOutputStream()
        GZIPOutputStream(compressed).use { compresser->
            compresser.write(toByteArray())
        }
        return Base64.getEncoder().encode(compressed.toByteArray()).let(::String)
    }

    /**
     * Read data v1
     *
     * @param data 一个base64编码后的GZIP压缩文件,解压后时完全契合README或者[JsonContent]格式的json.
     * @return [JsonContent]
     */
    @Suppress("NAME_SHADOWING")
    private fun readDataV1(data:String): JsonContent {
        //解码
        val data = data.decodeBase64AndGzipUncompress()
        return try {
            //转换成为JSON
            val data = JSONObject(data)
            //拿出apps字段的UninstallInfo
            val uninstallInfo = LinkedList<UninstallInfo>()
            val array = data.getJSONArray(KEY_ALL_APPS)
            repeat(array.length()) { i ->
                val app= array.getJSONObject(i)
                uninstallInfo.add(
                    UninstallInfo.DefaultUninstallInfo(
                        applicationName = app.getString(KEY_NAME),
                        packageName = app.getString(KEY_PACKAGE),
                        sourceDirectory = app.getString(KEY_SOURCE),
                        dataDirectory = app.getString(KEY_DATA)
                    )
                )
            }//一些别的数据
            JsonContent(
                name = data.getString(KEY_NAME),
                createTime = data.getInt(KEY_CREATE_TIME),
                isBackup = data.getBoolean(KEY_IS_BACKUP),
                apps = uninstallInfo
            )
        }catch (e:Exception) {
            notUsefulFormat(e)
        }
    }

    /**
     * 把[VersionConnected]转换成为可分享文本 v1
     *
     * @param version 转换的对象
     * @return 一个base64编码后的GZIP压缩文件,解压后时完全契合README或者[JsonContent]格式的json. 并且有版本信息
     */
    suspend fun generateV1(version: VersionConnected):String {
        val listBuildingTask = GlobalScope.async(Dispatchers.Default) {
            val sb = StringBuilder()
            //apps:[
            sb.append(KEY_ALL_APPS)
            sb.append(":[")
            for (r in version.apps) {
                val dataPath = r.data?.let { "'$it'" } ?: "null"
                sb.append("""{$KEY_NAME:'${r.name}',$KEY_PACKAGE:'${r.packageName}',$KEY_SOURCE:'${r.source}',$KEY_DATA:$dataPath}""")
            }
            //]
            sb.append("]")
            val result = sb.toString().replace("}{","},{")
            Log.i(TAG, "generateV1: list$result")
            result
        }
        val sb = StringBuilder()
        with(version) {
            sb.append("""{$KEY_NAME:'$name',$KEY_CREATE_TIME:$createTime,$KEY_IS_BACKUP:$isBackup,""")
            sb.append(listBuildingTask.await())
            sb.append("}")
        }
        //获得不包含版本信息的原始数据
        val result = sb.toString().compressAndEncodeByBase64()
        Log.d(TAG, "generateV1: result:$result")
        return """{v:1,d:'$result'}"""
    }

    /**
     * Decode version and data info
     *
     * @param string {[KEY_VERSION]:1,[KEY_DATA]:avafgfhaghaffhgasf==}
     * @return version and data
     */
    private fun decodeVersionAndDataInfo(string: String):Pair<Int,String> {
        return try {
            val data = JSONObject(string)
            data.getInt(KEY_VERSION) to data.getString(KEY_DATA)
        }catch (e:Exception) {
            notUsefulFormat(e)
        }
    }
}