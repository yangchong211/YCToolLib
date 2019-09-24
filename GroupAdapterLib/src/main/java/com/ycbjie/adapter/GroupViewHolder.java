package com.ycbjie.adapter;

import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.View;


/**
 * <pre>
 *     @author yangchong
 *     blog  : https://github.com/yangchong211
 *     time  : 2019/9/18
 *     desc  : 通用的RecyclerView.ViewHolder。提供了根据viewId获取View的方法。
 *     revise:
 * </pre>
 */
public class GroupViewHolder extends RecyclerView.ViewHolder {

    private SparseArray<View> mViews;

    public GroupViewHolder(View itemView) {
        super(itemView);
        mViews = new SparseArray<>();
    }

    /**
     * 根据View Id 获取对应的View
     *
     * @param viewId
     * @param <T>
     * @return
     */
    public <T extends View> T get(int viewId) {
        View view = mViews.get(viewId);
        if (view == null) {
            view = this.itemView.findViewById(viewId);
            mViews.put(viewId, view);
        }
        return (T) view;
    }

}
