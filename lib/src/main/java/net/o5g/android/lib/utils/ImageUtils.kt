package net.o5g.android.lib.utils

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.media.ExifInterface
import android.net.Uri
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.view.WindowManager
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import id.zelory.compressor.Compressor
import net.o5g.android.lib.R
import net.o5g.android.lib.RtcBase
import net.o5g.android.lib.isNullOrBlankReturnNull
import java.io.File
import java.io.IOException

private const val IMAGE_MAX_SIZE = 1024 * 1024

object ImageUtils {

    /**
     * 计算并返回适合在屏幕上进行展示的图片尺寸，原则：不小于屏幕20%，不超过屏幕50%
     */
    fun getSizedImageDimension(imageDimension: ImageDimension): ImageDimension {
        var maxWith = 0f
        var maxHeight = 0f
        var minWidth = 0f
        var minHeight = 0f
        if (maxWith == 0f) {
            val outMetrics = DisplayMetrics();
            (RtcBase.application.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
                .getMetrics(outMetrics);
            val widthPixels = outMetrics.widthPixels
            maxWith = widthPixels * 0.5f
            maxHeight = widthPixels * 0.5f
            minWidth = widthPixels * 0.2f
            minHeight = widthPixels * 0.2f
        }

        //设置图片宽高 计算图片比例算出适合的宽高
        val picWidth = imageDimension.width.toFloat()
        val picHeight = imageDimension.height.toFloat()
        var realWidth = maxWith
        var realHeight = maxHeight
        if (picWidth > 0 && picHeight > 0) {
            //如果宽>高 则固定宽度 计算高度
            if (picWidth > picHeight) {
                realWidth = maxWith
                realHeight = realWidth * picHeight / picWidth
            } else {
                //否则固定高度,计算宽度
                realHeight = maxHeight
                realWidth = realHeight * picWidth / picHeight
            }
            //如果宽高小于最小宽高,则设置为最小宽高
            if (realHeight < minHeight) {
                realHeight = minHeight
            }
            if (realWidth < minWidth) {
                realWidth = minWidth
            }
        }
        return ImageDimension(realWidth.toInt(), realHeight.toInt())
    }

    /**
     * 指定图片URL并加载到对应控件
     */
    fun loadPic(
        view: ImageView,
        url: String?,
        defaultPic: Int = R.mipmap.ic_launcher,
        errorPic: Int = R.mipmap.ic_launcher,
        call: ((Drawable?) -> Unit)? = null
    ) {
        try {
            loadPic(Glide.with(view.context).load(url), defaultPic, errorPic, call, view)
        } catch (e: Exception) {
            RtcXLogUtil.logE(e)
        }
    }

    /**
     * 指定图片URI并加载到对应控件，通常用于加载本地图片
     */
    fun loadPic(
        view: ImageView,
        url: Uri?,
        defaultPic: Int = R.mipmap.ic_launcher,
        errorPic: Int = R.mipmap.ic_launcher,
        call: ((Drawable?) -> Unit)? = null
    ) {
        try {
            loadPic(Glide.with(view.context).load(url), defaultPic, errorPic, call, view)
        } catch (e: Exception) {
            RtcXLogUtil.logE(e)
        }
    }

    private fun loadPic(
        glide: RequestBuilder<Drawable>,
        defaultPic: Int,
        errorPic: Int,
        call: ((Drawable?) -> Unit)?,
        view: ImageView
    ) {
        glide.apply(RequestOptions().placeholder(defaultPic).error(errorPic))
            .addListener(object : RequestListener<Drawable> {
                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    call?.invoke(resource)
                    return false
                }

                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    call?.invoke(null)
                    return false
                }
            })
            .into(view)
    }

    /**
     * 将指定URL的图片加载为Bitmap数据并回调给调用者
     */
    fun loadBitmap(url: String, call: (Bitmap?) -> Unit) {
        if (url.isNullOrBlankReturnNull() == null)
            ui {
                call(null)
            }
        else
            background {
                try {
                    val img = Glide.with(RtcBase.application).asBitmap().load(url).submit().get()
                    ui { call(img) }
                } catch (e: Exception) {
                    ui {
                        call(null)
                    }
                    RtcXLogUtil.logE(e)
                }
            }
    }

    /**
     * 根据图片绝对路径查出图片URI
     */
    fun getImageContentUri(
        context: Context,
        path: String
    ): Uri? {
        val cursor: Cursor? = context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Images.Media._ID),
            MediaStore.Images.Media.DATA + "=? ",
            arrayOf(path),
            null
        )
        return if (cursor != null && cursor.moveToFirst()) {
            val id: Int =
                cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID))
            val baseUri =
                Uri.parse("content://media/external/images/media")
            Uri.withAppendedPath(baseUri, "" + id)
        } else {
            // 如果图片不在手机的共享图片数据库，就先把它插入。
            if (File(path).exists()) {
                val values = ContentValues()
                values.put(MediaStore.Images.Media.DATA, path)
                context.contentResolver
                    .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            } else {
                null
            }
        }
    }

    /**
     * 借助图片方向信息获取图片真正宽高信息
     */
    fun getImageDimension(path: String): ImageDimension {
        //获取Options对象
        val options = BitmapFactory.Options()
        //仅做解码处理，不加载到内存
        options.inJustDecodeBounds = true
        //解析文件
        BitmapFactory.decodeFile(path, options)
        //获取宽高
        val width = options.outWidth
        val height = options.outHeight

        try {
            val exifInterface = ExifInterface(path)
            val orientation =
                exifInterface.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            if (orientation == ExifInterface.ORIENTATION_ROTATE_90 || orientation == ExifInterface.ORIENTATION_ROTATE_270)
                return ImageDimension(height, width)

        } catch (e: IOException) {
            e.printStackTrace()
        }
        return ImageDimension(width, height)
    }

    /**
     * 图片分辨率及体积双重压缩，体积不超过1MB，分辨率压缩规则看代码
     */
    fun compressImage(file: File): File {
        val imageDimension: ImageDimension = getImageDimension(file.absolutePath)
        val maxLen = 1280.toFloat()
        val ratio = imageDimension.width.toFloat() / imageDimension.height
        var realWidth = imageDimension.width.toFloat()
        var realHeight = imageDimension.height.toFloat()
        if (imageDimension.width > maxLen || imageDimension.height > maxLen) {//只要有一条边长大于maxLen，则尝试缩小图片尺寸
            if (ratio < 2 && ratio > 0.5) {
                if (imageDimension.width > imageDimension.height) {//如果长短比相差不大，则将长边压缩到maxLen，短边等比压缩
                    realWidth = maxLen
                    realHeight = imageDimension.height * realWidth / imageDimension.width
                } else {
                    realHeight = maxLen
                    realWidth = imageDimension.width * realHeight / imageDimension.height
                }
            } else if (imageDimension.width > maxLen && imageDimension.height > maxLen) {
                //如果长短比相差非常大，并且两条边长都超过了maxLen，则将短边缩短到maxLen，长边等比缩短
                //如果按长边压缩，则短边可能会严重失真
                if (imageDimension.width > imageDimension.height) {
                    realHeight = maxLen
                    realWidth = imageDimension.width * realHeight / imageDimension.height
                } else {
                    realWidth = maxLen
                    realHeight = imageDimension.height * realWidth / imageDimension.width
                }
            } else {
                //如果长宽比相差非常大，但是有一条边长短于maxLen，则使用原尺寸
            }
        } else {
            //两条边长都不超，则不改变尺寸
        }
        if (realWidth == imageDimension.width.toFloat() && realHeight == imageDimension.height.toFloat() && file.length() <= IMAGE_MAX_SIZE) {
            val newFile = File(
                FileUtil.getAppCacheDir(CacheDir.upload_images).absolutePath,
                file.name
            )
            if (!newFile.exists())
                file.copyTo(newFile)
            return newFile
        }

        var compRatio = 100
        val compressor = Compressor(RtcBase.application)
            .setMaxWidth(realWidth.toInt())
            .setMaxHeight(realHeight.toInt())
            .setCompressFormat(Bitmap.CompressFormat.JPEG)
            .setDestinationDirectoryPath(FileUtil.getAppCacheDir(CacheDir.upload_images).absolutePath)
        var compFile = file
        while (compRatio > 0) {
            compRatio -= 5
            compFile =
                compressor.setQuality(compRatio).compressToFile(file)
            RtcXLogUtil.logE("\norig size ${file.length()} comped $compRatio size is ${compFile.length()}  limit $IMAGE_MAX_SIZE\n")
            if (compFile.length() <= IMAGE_MAX_SIZE) {
                RtcXLogUtil.logE(
                    "\norig file size is ${file.length() / 1024.0 / 1024} comped file size is ${compFile.length() / 1024.0 / 1024}" +
                        "\n$imageDimension>>$realWidth*$realHeight>>${
                            getImageDimension(
                                compFile.absolutePath
                            )
                        }\n"
                )
                return compFile
            }
        }
        return compFile
    }
}

data class ImageDimension(
    val width: Int,
    val height: Int
)