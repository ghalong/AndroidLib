package net.o5g.android.lib.utils

import android.os.Environment
import android.os.SystemClock
import androidx.core.net.toUri
import com.ihsanbal.logging.Level
import com.ihsanbal.logging.LoggingInterceptor
import net.o5g.android.lib.LibBase
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.internal.platform.Platform
import java.io.File
import java.io.Reader


/**
 * Author:  王海宇<br>
 * Date:    2020/07/20 19:52<br>
 * Desc:    <br>
 * Edit History:<br>
 */
object FileUtil {

    fun uploadFile(
        url: String,
        files: List<File>?,
        params: Map<String, String>? = null
    ): String? {
        val httpLoggingInterceptor: LoggingInterceptor = LoggingInterceptor.Builder()
            .setLevel(Level.BASIC)
            .log(Platform.INFO)
            .request("Request")
            .response("Response")
            .build()
        val client = OkHttpClient.Builder().addInterceptor(httpLoggingInterceptor).build()
        //3.构建MultipartBody
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .apply {
                params?.forEach {
                    addFormDataPart(it.key, it.value)
                }
                files?.forEach { file ->
                    if (!file.exists() || !file.isFile)
                        return@forEach
                    val mediaType = file.absolutePath.toUri()
                        .getMimeType(LibBase.application).toMediaType()
                    RtcXLogUtil.logE(
                        "upload attachment $mediaType  size  ${file.length() / 1024.0 / 1024} \n${file.absolutePath}\n"
                    )
                    //2.创建RequestBody
                    val fileBody: RequestBody = RequestBody.create(mediaType, file)
                    addFormDataPart("file", file.name, fileBody)
                }
            }
            .build()
        //4.构建请求
//        val url = "${BuildConfig.API_BASE_URI.replace("bi-app.", "report.")}/screenshot/uploadFile"
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        //5.发送请求
        var reader: Reader? = null
        try {
            val response = client.newCall(request).execute()
            reader = response.body?.charStream()
            val result = reader?.readText()
            reader?.close()
            RtcXLogUtil.logE("$url upload attachment with params $params\nresult = $result")
            return result
        } catch (e: Exception) {
            RtcXLogUtil.logE(
                "upload attachment fail $files\n"
            )
            RtcXLogUtil.logE(e)
            SystemClock.sleep(30000)
        } finally {
            reader?.close()
        }
        return null
    }

    private fun getAppCacheDir(): File {
        val dirPath: String
        //SD卡是否存在
        val isSdCardExists =
            Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()
        val isRootDirExists =
            Environment.getExternalStorageDirectory().exists()
        dirPath = if (isSdCardExists && isRootDirExists) {
            LibBase.application.externalCacheDir!!.absolutePath
        } else {
            LibBase.application.cacheDir.absolutePath
        }
        val appDir = File(dirPath)
        if (!appDir.exists()) {
            appDir.mkdirs()
        }
        return appDir
    }

    //获取录音存放路径
    fun getAppCacheDir(dir: CacheDir): File {
        val appDir = getAppCacheDir()
        val recordDir = File(appDir.absolutePath, dir.name)
        if (!recordDir.exists()) {
            recordDir.mkdir()
        }
        return recordDir
    }

}

enum class CacheDir {
    upload_images, upload_audio, backup_files
}