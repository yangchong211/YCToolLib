package com.ycbjie.ycgroupadapter;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.Toast;

import com.ycbjie.adapter.AbsGroupAdapter;
import com.ycbjie.adapter.GroupViewHolder;
import com.ycbjie.adapter.OnChildClickListener;
import com.ycbjie.adapter.OnFooterClickListener;
import com.ycbjie.adapter.OnHeaderClickListener;

import java.util.ArrayList;
import java.util.List;

public class FourActivity extends AppCompatActivity {

    private GroupedFourAdapter mAdapter;
    private List<GroupEntity> list = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.base_recycler_view);

        RecyclerView mRecyclerView = findViewById(R.id.recycler_view);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new GroupedFourAdapter(this, list);
        mAdapter.setOnHeaderClickListener(new OnHeaderClickListener() {
            @Override
            public void onHeaderClick(AbsGroupAdapter adapter, GroupViewHolder holder,
                                      int groupPosition) {
                Toast.makeText(FourActivity.this,
                        "组头：groupPosition = " + groupPosition,Toast.LENGTH_LONG).show();

                if (mAdapter.isExpand(groupPosition)) {
                    mAdapter.collapseGroup(groupPosition,true);
                } else {
                    mAdapter.expandGroup(groupPosition,true);
                }
            }
        });
        mAdapter.setOnFooterClickListener(new OnFooterClickListener() {
            @Override
            public void onFooterClick(AbsGroupAdapter adapter, GroupViewHolder holder,
                                      int groupPosition) {
                Toast.makeText(FourActivity.this,
                        "组尾：groupPosition = " + groupPosition,Toast.LENGTH_LONG).show();
            }
        });
        mAdapter.setOnChildClickListener(new OnChildClickListener() {
            @Override
            public void onChildClick(AbsGroupAdapter adapter, GroupViewHolder holder,
                                     int groupPosition, int childPosition) {
                Toast.makeText(FourActivity.this,"子项：groupPosition = " + groupPosition
                        + ", childPosition = " + childPosition,Toast.LENGTH_LONG).show();
            }
        });
        mRecyclerView.setAdapter(mAdapter);

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

            switch (i){
                case 0:
                    for (int j = 0; j < 2; j++) {
                        children.add(new ChildEntity("第" + (i + 1) + "组第" + (j + 1) + "项"));
                    }
                    break;
                case 1:
                    for (int j = 0; j < 4; j++) {
                        children.add(new ChildEntity("第" + (i + 1) + "组第" + (j + 1) + "项"));
                    }
                    break;
                case 2:
                    for (int j = 0; j < 6; j++) {
                        children.add(new ChildEntity("第" + (i + 1) + "组第" + (j + 1) + "项"));
                    }
                    break;
                case 3:
                    for (int j = 0; j < 3; j++) {
                        children.add(new ChildEntity("第" + (i + 1) + "组第" + (j + 1) + "项"));
                    }
                    break;
                default:
                    for (int j = 0; j < childrenCount; j++) {
                        children.add(new ChildEntity("第" + (i + 1) + "组第" + (j + 1) + "项"));
                    }
                    break;
            }
            groups.add(new GroupEntity("第" + (i + 1) + "组头部",
                    "第" + (i + 1) + "组尾部", children,true));
        }
        return groups;
    }

}
