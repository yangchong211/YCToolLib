package com.ycbjie.ycgroupadapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.ycbjie.adapter.AbsGroupedAdapter;
import com.ycbjie.adapter.GroupViewHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * <pre>
 *     @author yangchong
 *     blog  : https://github.com/yangchong211
 *     time  : 2019/9/18
 *     desc  : 这是普通的分组Adapter 每一个组都有头部、尾部和子项。
 *     revise:
 * </pre>
 */
public class GroupedFiveAdapter extends AbsGroupedAdapter {

    private List<GroupEntity> mGroups;

    private static final int TYPE_CHILD_1 = 5;
    private static final int TYPE_CHILD_2 = 6;

    public GroupedFiveAdapter(Context context, List<GroupEntity> groups) {
        super(context);
        mGroups = groups;
    }

    @Override
    public int getGroupCount() {
        return mGroups == null ? 0 : mGroups.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        if (mGroups!=null){
            ArrayList<ChildEntity> children = mGroups.get(groupPosition).getChildren();
            return children == null ? 0 : children.size();
        }
        return 0;
    }

    @Override
    public boolean hasHeader(int groupPosition) {
        return false;
    }

    @Override
    public boolean hasFooter(int groupPosition) {
        return true;
    }

    @Override
    public int getHeaderLayout(int viewType) {
        return R.layout.item_text_header;
    }

    @Override
    public int getFooterLayout(int viewType) {
        return R.layout.item_text_footer;
    }



    @Override
    public void onBindHeaderViewHolder(GroupViewHolder holder, int groupPosition) {
        GroupEntity entity = mGroups.get(groupPosition);
        TextView textView = holder.get(R.id.tv_header);
        textView.setText(entity.getHeader());
    }

    @Override
    public void onBindFooterViewHolder(GroupViewHolder holder, int groupPosition) {
        GroupEntity entity = mGroups.get(groupPosition);
        TextView tvFooterMore = holder.get(R.id.tv_footer_more);
        String footer = entity.getFooter();
        if (footer==null || footer.length()==0){
            //设置不可见
            tvFooterMore.setVisibility(View.GONE);
        } else {
            //设置可见
            tvFooterMore.setText(footer);
            tvFooterMore.setVisibility(View.VISIBLE);
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindChildViewHolder(GroupViewHolder holder, int groupPosition, int childPosition) {
        ChildEntity entity = mGroups.get(groupPosition).getChildren().get(childPosition);
        int viewType = getChildViewType(groupPosition, childPosition);
        TextView tvContent = holder.get(R.id.tv_content);
        if (viewType == TYPE_CHILD_1) {
            tvContent.setText(entity.getChild());
        } else if (viewType == TYPE_CHILD_2) {
            tvContent.setText("另一种视图"+entity.getChild());
        }
    }

    @Override
    public int getChildLayout(int viewType) {
        if (viewType == TYPE_CHILD_1) {
            return R.layout.item_content_view;
        } else {
            return R.layout.item_content_view2;
        }
    }


    @Override
    public int getChildViewType(int groupPosition, int childPosition) {
        if (groupPosition % 2 == 0) {
            return TYPE_CHILD_1;
        } else {
            return TYPE_CHILD_2;
        }
    }

}
