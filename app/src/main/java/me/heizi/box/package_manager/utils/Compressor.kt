package me.heizi.box.package_manager.utils

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import com.google.zxing.*
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import me.heizi.box.package_manager.Application.Companion.TAG
import me.heizi.box.package_manager.models.CompleteVersion
import org.json.JSONObject
import java.nio.charset.Charset

object Compressor {

    fun notUsefulFormat():Nothing { throw IllegalArgumentException("数据损坏或非本应用支持的格式") }
    fun <E:Exception> notUsefulFormat(e:E):Nothing { throw IllegalArgumentException("数据损坏或非本应用支持的格式",e) }
    private fun versionNotMatch():Nothing { throw IllegalArgumentException("该卸载方案的版本不属于本版本的软件可解析的范围,请适当升级或降级。") }
    private val usefulFormat get() = """\{v:\d,d:.+\}""".toRegex()


    fun read(bitmap: Bitmap): CompleteVersion {
        val h = bitmap.height
        val w = bitmap.width
        Log.i(TAG, "read: $h,$w")
        val pixels = IntArray(w*h)

        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        val rgbLuminanceSource = RGBLuminanceSource(w, h, pixels)
        return QRCodeReader().decode(BinaryBitmap(GlobalHistogramBinarizer(rgbLuminanceSource)), hashMapOf(
            DecodeHintType.CHARACTER_SET to CHARSET,
            DecodeHintType.TRY_HARDER to java.lang.Boolean.TRUE,
            DecodeHintType.PURE_BARCODE to java.lang.Boolean.TRUE,
        )).text.toByteArray(Charset.forName(CHARSET)).let {
            val (versionCode,data)  = unpackedVersion(it)
            when(val v = CompressorVersion.getVersionFormCode(versionCode) ) {
                is CompressorVersion.V1 ->  v.decodeForImage(data)
                else -> versionNotMatch()
            }
        }
    }

    /**
     * 解释从用户手里拿到的[text]
     *
     * @param text {v:[CompleteVersion.versionCode],d:'anyway'}
     * @return
     */
    fun read(text:String): CompleteVersion {
        if (!text.matches(usefulFormat)) notUsefulFormat()
        val (version, data) = unpackedVersion(text)
        return when(val v = CompressorVersion.getVersionFormCode(version)) {
            CompressorVersion.V1 -> v.decodeText(data)
            else -> versionNotMatch()
        }
    }

    private val currentCompressor get() = CompressorVersion.V1


    fun CompleteVersion.toQrCode(): Bitmap? = currentCompressor.run {
        genQrCode(String(packageVersion(encodeForImage(this@toQrCode)), charset(CHARSET)))
    }

    fun CompleteVersion.toShareableText():String = currentCompressor.run {
        packageVersionText(encodeText(this@toShareableText))
    }

    private fun CompressorVersion.packageVersionText(text: String) =
        """{$KEY_VERSION:$versionCode,$KEY_DATA:'${text}'}"""

    private fun genQrCode(data: String):Bitmap? = try {
        var end = 500
        QRCodeWriter().encode(data, BarcodeFormat.QR_CODE,end,end, hashMapOf(
            EncodeHintType.CHARACTER_SET to CHARSET,
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.L
        )).let { bitMatrix ->
            var limit = 0
            do {
                val bool = bitMatrix[limit,limit]
                limit++
            }while (!bool)
            var start = 0
            if (limit > SKIP_PIXEL_MARGIN) {
                start = limit- SKIP_PIXEL_MARGIN
                end -= start
            }
            Bitmap.createBitmap(bitMatrix.width, bitMatrix.width, Bitmap.Config.ARGB_8888).apply {
                val array = start..end
                for (x in array) for (y in array) {
                    setPixel(x, y, (if (bitMatrix[x, y]) Color.BLACK else Color.WHITE))
                }
            }
        }
    }catch (e: WriterException) {
        if (e.message != "Data too big") throw Exception("未知错误",e)
        null
    }catch (e:Exception) {
        throw Exception("未知错误",e)
    }

    /**
     * 包装版本进二维码
     *
     * @param bytes
     * @return
     */
    private fun CompressorVersion.packageVersion(bytes: ByteArray): ByteArray {
        val result = ByteArray(bytes.size+1) {
            bytes[it]
        }
        result[bytes.size] = versionCode.toByte()
        return bytes
    }
    /**
     * 解析版本信息
     *
     * @param string
     * @return
     */
    private fun unpackedVersion(string: String):Pair<Int,String> {
        return try {
            val data = JSONObject(string)
            data.getInt(KEY_VERSION) to data.getString(KEY_DATA)
        }catch (e:Exception) {
            notUsefulFormat(e)
        }
    }

    private fun unpackedVersion(bytes: ByteArray):Pair<Int,ByteArray> {
        return try {
            val versionCode = bytes.last().toInt()
            val data = ByteArray(bytes.size-1,bytes::get)
            versionCode to data
        }catch (e:Exception) {
            notUsefulFormat(e)
        }
    }
    private const val KEY_VERSION = "v"
    private const val KEY_DATA = "d"
    private const val CHARSET = "ISO8859-1"
    private const val SKIP_PIXEL_MARGIN = 10
}