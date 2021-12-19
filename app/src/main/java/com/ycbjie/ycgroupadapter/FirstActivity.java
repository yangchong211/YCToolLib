package com.ycbjie.ycgroupadapter;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.Toast;

import com.ycbjie.adapter.AbsGroupAdapter;
import com.ycbjie.adapter.GroupViewHolder;
import com.ycbjie.adapter.GroupLayoutManager;
import com.ycbjie.adapter.OnChildClickListener;
import com.ycbjie.adapter.OnFooterClickListener;
import com.ycbjie.adapter.OnHeaderClickListener;

import java.util.ArrayList;
import java.util.List;

public class FirstActivity extends AppCompatActivity {

    private GroupedFirstAdapter mAdapter;
    private List<GroupEntity> list = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.base_recycler_view);

        RecyclerView mRecyclerView = findViewById(R.id.recycler_view);
        mAdapter = new GroupedFirstAdapter(this, list);
        mAdapter.setOnHeaderClickListener(new OnHeaderClickListener() {
            @Override
            public void onHeaderClick(AbsGroupAdapter adapter, GroupViewHolder holder,
                                      int groupPosition) {
                Toast.makeText(FirstActivity.this,
                        "组头：groupPosition = " + groupPosition,Toast.LENGTH_LONG).show();
            }
        });
        mAdapter.setOnFooterClickListener(new OnFooterClickListener() {
            @Override
            public void onFooterClick(AbsGroupAdapter adapter, GroupViewHolder holder,
                                      int groupPosition) {
                Toast.makeText(FirstActivity.this,
                        "组尾：groupPosition = " + groupPosition,Toast.LENGTH_LONG).show();
                GroupEntity groupEntity = list.get(groupPosition);
                //设置footer点击后不可见状态
                groupEntity.setFooter("");
                ArrayList<ChildEntity> children = groupEntity.getChildren();
                int size = children.size();
                for (int j = 0; j < 10; j++) {
                    children.add(new ChildEntity("逗比"));
                }
                //通知一组里的多个子项插入
                mAdapter.notifyChildRangeInserted(groupPosition,size,10);
            }
        });
        mAdapter.setOnChildClickListener(new OnChildClickListener() {
            @Override
            public void onChildClick(AbsGroupAdapter adapter, GroupViewHolder holder,
                                     int groupPosition, int childPosition) {
                Toast.makeText(FirstActivity.this,"子项：groupPosition = " + groupPosition
                        + ", childPosition = " + childPosition,Toast.LENGTH_LONG).show();
            }
        });
        mRecyclerView.setAdapter(mAdapter);
        //直接使用GroupGridLayoutManager实现子项的Grid效果
        GroupLayoutManager gridLayoutManager = new GroupLayoutManager(this, 3, mAdapter){
            //重写这个方法 改变子项的SpanSize。
            //这个跟重写SpanSizeLookup的getSpanSize方法的使用是一样的。
            @Override
            public int getChildSpanSize(int groupPosition, int childPosition) {
                return super.getChildSpanSize(groupPosition, childPosition);
            }
        };
        mRecyclerView.setLayoutManager(gridLayoutManager);

        getData();
    }



    private void getData() {
        ArrayList<GroupEntity> groups = getGroups(10, 8);
        list.addAll(groups);
        mAdapter.notifyDataSetChanged();
    }


    /**
     * 获取组列表数据
     *
     * @param groupCount    组数量
     * @param childrenCount 每个组里的子项数量
     * @return
     */
    public static ArrayList<GroupEntity> getGroups(int groupCount, int childrenCount) {
        ArrayList<GroupEntity> groups = new ArrayList<>();
        for (int i = 0; i < groupCount; i++) {
            ArrayList<ChildEntity> children = new ArrayList<>();
            for (int j = 0; j < childrenCount; j++) {
                children.add(new ChildEntity("第" + (i + 1) + "组第" + (j + 1) + "项"));
            }
            groups.add(new GroupEntity("第" + (i + 1) + "组头部",
                    "第" + (i + 1) + "组尾部", children));
        }
        return groups;
    }



    private void testApi(){
        //通知一组数据插入
        mAdapter.notifyGroupInserted(1);
        //通知一个子项到组里插入
        mAdapter.notifyChildInserted(1,3);
        //通知一组里的多个子项插入
        mAdapter.notifyChildRangeInserted(1,2,10);
        //通知一组里的所有子项插入
        mAdapter.notifyChildrenInserted(1);
        //通知多组数据插入
        mAdapter.notifyGroupRangeInserted(1,3);
        //通知组头插入
        mAdapter.notifyHeaderInserted(1);
        //通知组尾插入
        mAdapter.notifyFooterInserted(1);


        //移除数据操作
        //通知所有数据删除
        mAdapter.notifyDataRemoved();
        //通知一组数据删除，包括组头,组尾和子项
        mAdapter.notifyGroupRemoved(1);
        //通知多组数据删除，包括组头,组尾和子项
        mAdapter.notifyGroupRangeRemoved(1,3);
        //通知组头删除
        mAdapter.notifyHeaderRemoved(1);
        //通知组尾删除
        mAdapter.notifyFooterRemoved(1);
        //通知一组里的某个子项删除
        mAdapter.notifyChildRemoved(1,3);
        //通知一组里的多个子项删除
        mAdapter.notifyChildRangeRemoved(1,3,4);
        //通知一组里的所有子项删除
        mAdapter.notifyChildrenRemoved(1);
    }



}
