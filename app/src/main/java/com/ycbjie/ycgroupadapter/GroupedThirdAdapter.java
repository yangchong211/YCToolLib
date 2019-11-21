package com.ycbjie.ycgroupadapter;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.ycbjie.adapter.AbsGroupAdapter;
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
public class GroupedThirdAdapter extends AbsGroupAdapter {

    private List<GroupEntity> mGroups;

    public GroupedThirdAdapter(Context context, List<GroupEntity> groups) {
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
    public int getChildLayout(int viewType) {
        return R.layout.item_content_view;
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

    @Override
    public void onBindChildViewHolder(GroupViewHolder holder, int groupPosition, int childPosition) {
        ChildEntity entity = mGroups.get(groupPosition).getChildren().get(childPosition);
        TextView tv_content = holder.get(R.id.tv_content);
        tv_content.setText(entity.getChild());
    }

}
