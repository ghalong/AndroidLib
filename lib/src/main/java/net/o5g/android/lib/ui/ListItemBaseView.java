package net.o5g.android.lib.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Version: V1.4.2<br>
 * Author:  王海宇<br>
 * Date:    2016/12/06 14:42<br>
 * Desc:    <br>
 * Edit History:<br>
 */
public abstract class ListItemBaseView<D> extends FrameLayout implements View.OnClickListener {
    private D data;
    private int position;
    private Map<Integer, List<View>> recycleViews = new HashMap<>();
    private Map<String, List<View>> recycleTypeViews = new HashMap<>();

    public ListItemBaseView(Context context) {
        super(context);
        init(null);
    }

    public ListItemBaseView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);

    }

    public ListItemBaseView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        inflate(getContext(), getInflateResId(), this);
        onInit(attrs);
    }

    protected void onInit(AttributeSet attrs) {

    }

    abstract protected int getInflateResId();

    final protected void registerOnClickListener(int... ids) {
        if (ids != null && ids.length > 0) {
            for (int id : ids) {
                View view = findViewById(id);
                if (view != null) view.setOnClickListener(this);
            }
        }
    }

    protected void clicked(int id) {

    }

    protected abstract void updateView(D data);

    public int getPosition() {
        return position;
    }

    public ListItemBaseView<D> setPosition(int position) {
        this.position = position;
        return this;
    }

    public D getData() {
        return data;
    }

    final public void setData(D data) {
        this.data = data;
        if (data != null)
            updateView(data);
    }

    /**
     * 回收对应类型的动态生成子控件视图上的子控件对象
     * 只能回收同一种类型的子控件，并为这种控件指定type类型
     * @param parent
     * @param type
     */
    final protected void recycleView(ViewGroup parent, int type) {
        if (parent == null)
            return;
        int count = parent.getChildCount();
        if (count > 0) {//复用促销icon
            List<View> recycleList = recycleViews.get(type);
            if (recycleList == null) {
                recycleList = new ArrayList<>();
                recycleViews.put(type, recycleList);
            }
            while (count != 0) {
                recycleList.add(parent.getChildAt(--count));
            }
            parent.removeAllViews();
        }
    }

    /**
     * 根据子控件类型的不同，可以分别回收到不同的缓存中，不用指定子控件类型，也不要求子控件类型相同
     * @param parent
     */
    final protected void recycleView(ViewGroup parent) {
        if (parent == null)
            return;
        int count = parent.getChildCount();
        if (count > 0) {//复用促销icon

            while (count != 0) {
                View subView = parent.getChildAt(--count);
                String typeName = subView.getClass().getCanonicalName();
                List<View> recycleList = recycleTypeViews.get(typeName);
                if (recycleList == null) {
                    recycleList = new ArrayList<>();
                    recycleTypeViews.put(typeName, recycleList);
                }
                recycleList.add(subView);
            }
            parent.removeAllViews();
        }
    }

    /**
     * 根据指定子控件的类型来获取对应子控件
     * @param clazz
     * @param <T>
     * @return
     */
    final protected <T extends View>T getTypedView(Class<T> clazz) {
        String typeName = clazz.getCanonicalName();
        List<View> views = recycleTypeViews.get(typeName);
        if (views != null && views.size() > 0) {
            return (T)(views.remove(0));
        } else {
            return (T)genTypedView(clazz);
        }

    }

    /**
     * 根据指定的子控件类型生成对应的子控件对象
     * @param clazz
     * @return
     */
    protected View genTypedView(Class<? extends View> clazz) {
        return null;
    }

    /**
         * 返回对应类型的子控件，需要先调用recycleView方法
         *
         * @param type
         * @return
         */
    final protected View getTypedView(int type) {
        List<View> views = recycleViews.get(type);
        if (views != null && views.size() > 0) {
            return views.remove(0);
        } else {
            return genTypedView(type);
        }
    }

    protected View genTypedView(int type) {
        return null;
    }

    @Override
    public void onClick(View v) {
        if (v != null)
            clicked(v.getId());
    }
}
