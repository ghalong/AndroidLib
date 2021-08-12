package net.o5g.android.lib.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import java.util.*

/**
 * Version: V1.4.2<br></br>
 * Author:  王海宇<br></br>
 * Date:    2016/12/06 14:42<br></br>
 * Desc:    <br></br>
 * Edit History:<br></br>
 */
abstract class ItemBaseView<D> : FrameLayout, View.OnClickListener {
    var data: D? = null
        set(data) {
            field = data
            updateView(data)
        }
    var position = 0
        private set
    private val recycleViews: MutableMap<Int, MutableList<View>> = HashMap()
    private val recycleTypeViews: MutableMap<String, MutableList<View>> = HashMap()

    constructor(context: Context?) : super(context!!) {
        init(null)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context!!, attrs) {
        init(attrs)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context!!, attrs, defStyleAttr) {
        init(attrs)
    }

    private fun init(attrs: AttributeSet?) {
        inflate(context, inflateResId, this)
        onInit(attrs)
    }

    open fun onInit(attrs: AttributeSet?) {}
    protected abstract val inflateResId: Int

    protected fun registerOnClickListener(vararg ids: Int) {
        if (ids.isNotEmpty()) {
            for (id in ids) {
                val view = findViewById<View>(id)
                view?.setOnClickListener(this)
            }
        }
    }

    open fun clicked(id: Int) {}
    protected abstract fun updateView(data: D?)
    fun setPosition(position: Int): ItemBaseView<D> {
        this.position = position
        return this
    }

    /**
     * 回收对应类型的动态生成子控件视图上的子控件对象
     * 只能回收同一种类型的子控件，并为这种控件指定type类型
     * @param parent
     * @param type
     */
    protected fun recycleView(parent: ViewGroup?, type: Int) {
        if (parent == null) return
        var count = parent.childCount
        if (count > 0) { //复用促销icon
            var recycleList = recycleViews[type]
            if (recycleList == null) {
                recycleList = ArrayList()
                recycleViews[type] = recycleList
            }
            while (count != 0) {
                recycleList.add(parent.getChildAt(--count))
            }
            parent.removeAllViews()
        }
    }

    /**
     * 根据子控件类型的不同，可以分别回收到不同的缓存中，不用指定子控件类型，也不要求子控件类型相同
     * @param parent
     */
    protected fun recycleView(parent: ViewGroup?) {
        if (parent == null) return
        var count = parent.childCount
        if (count > 0) { //复用促销icon
            while (count != 0) {
                val subView = parent.getChildAt(--count)
                val typeName = subView.javaClass.canonicalName!!
                var recycleList = recycleTypeViews[typeName]
                if (recycleList == null) {
                    recycleList = ArrayList()
                    recycleTypeViews[typeName] = recycleList
                }
                recycleList.add(subView)
            }
            parent.removeAllViews()
        }
    }

    /**
     * 根据指定子控件的类型来获取对应子控件
     * @param clazz
     * @param <T>
     * @return
    </T> */
    protected fun <T : View?> getTypedView(clazz: Class<out T>): T? {
        val typeName = clazz.canonicalName!!
        val views: MutableList<View>? = recycleTypeViews[typeName]

        return if (views != null && views.size > 0) {
            views.removeAt(0) as T
        } else {
            genTypedView(clazz) as T?
        }
    }

    /**
     * 根据指定的子控件类型生成对应的子控件对象
     * @param clazz
     * @return
     */
    open fun genTypedView(clazz: Class<out View>?): View? {
        return null
    }

    /**
     * 返回对应类型的子控件，需要先调用recycleView方法
     *
     * @param type
     * @return
     */
    open fun getTypedView(type: Int): View? {
        val views: MutableList<View>? = recycleViews[type]
        return if (views != null && views.size > 0) {
            views.removeAt(0)
        } else {
            genTypedView(type)
        }
    }

    open fun genTypedView(type: Int): View? {
        return null
    }

    override fun onClick(v: View) {
        clicked(v.id)
    }
}