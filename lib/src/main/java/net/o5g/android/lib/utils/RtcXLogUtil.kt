package net.o5g.android.lib.utils

import android.content.Context
import com.tencent.mars.xlog.Log
import com.tencent.mars.xlog.Xlog
import java.io.*
import java.text.SimpleDateFormat
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


/**
 * Author:  王海宇<br>
 * Date:    2020/03/31 10:40<br>
 * Desc:    <br>
 * Edit History:<br>
 */
private const val PUB_KEY =
    "965bd9b1fa99ca2f0928ce142ad57a07f2cebefea9ce3d9d574dbb0003d4a53ef4c8d335fa5f30db9bc12362ab841b92f835f3ed76fbe8573ba430c5ef53da74"
private const val XLOG_FILE_PREFIX = "o5g.net_"
private const val XLOG_CACHE_DAYS = 10
private const val DAY_TIME = 24 * 60 * 60 * 1000

object RtcXLogUtil {
    val dateFormat = SimpleDateFormat("yyyyMMdd")
    private var inited = false
    private lateinit var logPath: String
    internal fun init(context: Context) {
        if (inited)
            return
        System.loadLibrary("c++_shared")
        System.loadLibrary("marsxlog")

        logPath = "${context.getExternalFilesDir("xlog")?.absolutePath}"

        Xlog.appenderOpen(
            Xlog.LEVEL_ALL,
            Xlog.AppednerModeAsync,
            null,
            logPath,
            XLOG_FILE_PREFIX,
            XLOG_CACHE_DAYS,
            PUB_KEY
        )
        Xlog.setConsoleLogOpen(true)

        Log.setLogImp(Xlog())
        inited = true
    }

    internal fun logV(tag: String, log: Any) {
        log(tag, LogLevel.V, log)
    }

    internal fun logI(tag: String, log: Any) {
        log(tag, LogLevel.I, log)
    }

    internal fun logW(tag: String, log: Any) {
        log(tag, LogLevel.W, log)
    }

    fun logE(tag: String, log: Any) {
        log(tag, LogLevel.E, log)
    }

    fun logV(log: Any) {
        if (log is Throwable)
            android.util.Log.v("rtc_log", "ex", log)
        else
            android.util.Log.v("rtc_log", "$log")
    }

    fun logI(log: Any) {
        if (log is Throwable)
            android.util.Log.i("rtc_log", "ex", log)
        else
            android.util.Log.i("rtc_log", "$log")
    }

    fun logW(log: Any) {
        if (log is Throwable)
            android.util.Log.w("rtc_log", "ex", log)
        else
            android.util.Log.w("rtc_log", "$log")
    }

    fun logE(log: Any) {
        if (log is Throwable)
            android.util.Log.e("rtc_log", "ex", log)
        else
            android.util.Log.e("rtc_log", "$log")
    }

    fun log(tag: String, level: LogLevel, log: Any?) {
        val fullLog = "${
            if (log is Throwable) {
                val sw = StringWriter()
                val pw = PrintWriter(sw)
                log.printStackTrace(pw)
                sw
            } else log
        }"
        val logCall: (String) -> Unit =
            when (level) {
                LogLevel.V -> { littleLog ->
                    Log.v(tag, littleLog)
                }
                LogLevel.D -> { littleLog ->
                    Log.d(tag, littleLog)
                }
                LogLevel.I -> { littleLog ->
                    Log.i(tag, littleLog)
                }
                LogLevel.W -> { littleLog ->
                    Log.w(tag, littleLog)
                }
                LogLevel.E -> { littleLog ->
                    Log.e(tag, littleLog)
                }
            }
        synchronized(PUB_KEY) {//确保每次打印的日志都能紧挨在一起
            var index = 0
            while (true) {
                //经过测试，日志输出字符个数超过1800的部分有几率（跟字符占据字节数大小有关）会被截断丢弃，因此将每次日志输出字符个数控制在1800以内来避免该问题
                val length = index + 1800
                val newLine = if (index == 0) "" else "\n"
                if (length >= fullLog.length) {
                    logCall("$newLine${fullLog.substring(index, fullLog.length)}")
                    break
                } else {
                    logCall("$newLine${fullLog.substring(index, length)}")
                    index = length
                }
            }
        }
    }

    enum class LogLevel {
        V, D, I, W, E
    }

    private fun xLogFlush() {
        Log.appenderFlush(true)
    }

    /**
     * 把接受的全部文件打成压缩包
     * @param files<File>;
     * @param logZipFile
    </File> */
    fun getLogZip(days: Int): File? {
        xLogFlush()
        var count = 0
        val day = System.currentTimeMillis()
        val files = mutableListOf<File>()
        while (count < days) {
            val file = File(
                logPath,
                "${XLOG_FILE_PREFIX}_${dateFormat.format(day - count * DAY_TIME)}.xlog"
            )
            if (file.exists() && file.isFile && file.length() > 0)
                files.add(file)
            count++
        }
        println(files)
        if (files.isEmpty())
            return null
        val logZipFile = File(logPath, "logs.zip")
        val zipOS = ZipOutputStream(BufferedOutputStream(FileOutputStream(logZipFile)))
        zipOS.setLevel(Deflater.BEST_COMPRESSION)
        val size = files.size
        for (i in 0 until size) {
            compressFile(files[i], zipOS)
        }
        zipOS.close()
        return logZipFile
    }


    /**
     * 根据输入的文件与输出流对文件进行打包
     * @param inputFile
     * @param zipOS
     */
    private fun compressFile(
        inputFile: File,
        zipOS: ZipOutputStream
    ) {
        /**如果是目录的话这里是不采取操作的 */
        val bins = BufferedInputStream(FileInputStream(inputFile), 512)
        //org.apache.tools.zip.ZipEntry
        val entry = ZipEntry(inputFile.name)
        zipOS.putNextEntry(entry)
        // 向压缩文件中输出数据
        var size: Int
        val buffer = ByteArray(512)
        while (bins.read(buffer).also { size = it } != -1) {
            zipOS.write(buffer, 0, size)
        }
        // 关闭创建的流对象
        bins.close()
        zipOS.closeEntry()
    }
}
