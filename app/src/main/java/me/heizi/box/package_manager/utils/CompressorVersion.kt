package me.heizi.box.package_manager.utils

import me.heizi.box.package_manager.models.BackupType.Companion.HasNoBackup
import me.heizi.box.package_manager.models.BackupType.Companion.MoveDir
import me.heizi.box.package_manager.models.CompleteVersion
import me.heizi.box.package_manager.models.UninstallInfo
import me.heizi.box.package_manager.utils.Compressor.notUsefulFormat
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * 压缩器版本 包含序列化
 */
interface CompressorVersion {
    /**
     * Version code
     */
    val versionCode:Int
    /**
     * 序列化数据
     *
     * @param version
     * @return
     */
    fun serialize(version:CompleteVersion):ByteArray {
        val sb = StringBuilder()
        with(version) {
            """{$KEY_NAME:'$name',${KEY_CREATE_TIME}:$createTime,${KEY_IS_BACKUP}:${backupType != HasNoBackup},"""
        }.let(sb::append)
        //apps:[
        sb.append(KEY_ALL_APPS)
        sb.append(":[")
        for (r in version.apps) {
            sb.append("""{${KEY_NAME}:'${r.applicationName}',${KEY_PACKAGE}:'${r.packageName}',${KEY_SOURCE}:'${r.sourceDirectory}'}""")
        }
        //]
        sb.append("]}")
        return sb.toString().replace("'}{","'},{").toByteArray()
    }

    /**
     * 将数据进行反序列化
     *
     * @param data 已经解压好的字符串
     * @return
     */
    fun deserialize(data: String):CompleteVersion {
        return try {
            //转换成为JSON
            val json = JSONObject(data)
            //拿出apps字段的UninstallInfo
            val uninstallInfo = LinkedList<UninstallInfo>()
            val array = json.getJSONArray(KEY_ALL_APPS)
            repeat(array.length()) { i ->
                val app= array.getJSONObject(i)
                uninstallInfo.add(
                    UninstallInfo.DefaultUninstallInfo(
                        applicationName = app.getString(KEY_NAME),
                        packageName = app.getString(KEY_PACKAGE),
                        sourceDirectory = app.getString(KEY_SOURCE),
                    )
                )
            }//一些别的数据
            object : CompleteVersion {
                override val name: String = json.getString(KEY_NAME)
                override val apps: List<UninstallInfo> = uninstallInfo
                override val createTime: Int = json.getInt(KEY_CREATE_TIME)
                override val backupType: Int = if (json.getBoolean(KEY_IS_BACKUP)) MoveDir else HasNoBackup
            }
        }catch (e:Exception) {
            notUsefulFormat(e)
        }
    }

    /**
     * 压缩
     *
     * @param bytes 未压缩
     * @return 压缩后原始数据
     */
    fun compress(bytes: ByteArray):ByteArray {
        val result = ByteArrayOutputStream()
        GZIPOutputStream(result).use { compressor->
            compressor.write(bytes)
            compressor.flush()
        }
        return result.toByteArray()
    }

    /**
     * 解压数据
     *
     * @param bytes 压缩后的数据
     * @return 解压出来的Bytes
     */
    fun depress(bytes: ByteArray):String {
        return GZIPInputStream(bytes.inputStream()).bufferedReader().readText()
    }
    /**
     * 包装成可用的文字
     *
     * @param unpackage 压缩后的ByteArray
     */
    fun toShareableText(unpackage: ByteArray): String = Base64.getEncoder().encodeToString(unpackage)
    /**
     * 解包
     *
     * @param {v:[versionCode],d:'[text]'}
     * @return
     */
    fun unsharable(text: String):ByteArray {
        return Base64.getDecoder().decode(text)
    }

    /**
     * 将Based64Text解码成为[CompleteVersion]
     *
     * @param text Based 64 文本
     * @return 完整的卸载版本信息
     */
    fun decodeText(text: String): CompleteVersion {
        return deserialize(depress(unsharable(text)))
    }
    /**
     * 将[version]编码成为可分享的文本
     *
     * @param version
     * @return
     */
    fun encodeText(version: CompleteVersion):String {
        return toShareableText(compress(serialize(version)))
    }

    fun encodeForImage(version: CompleteVersion):ByteArray {
        return compress(serialize(version))
    }

    fun decodeForImage(bytes: ByteArray):CompleteVersion {
        return deserialize(depress(bytes))
    }

    companion object {
        const val KEY_NAME = "n"
        const val KEY_PACKAGE = "p"
        const val KEY_SOURCE = "s"
        const val KEY_IS_BACKUP = "b"
        const val KEY_CREATE_TIME ="t"
        const val KEY_ALL_APPS ="a"

        fun getVersionFormCode(code:Int): CompressorVersion? = V1
    }
    object V1 : CompressorVersion {
        override val versionCode: Int = 1
    }
}

