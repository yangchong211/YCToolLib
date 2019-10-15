# YCGroupAdapter
- 01.前沿说明
    - 1.1 案例展示效果
    - 1.2 该库功能和优势
    - 1.3 相关类介绍说明
- 02.如何使用
    - 2.1 如何引入
    - 2.2 最简单使用
    - 2.3 使用建议
- 03.常用api
    - 3.1 自定义adapter
    - 3.2 notify相关
    - 3.3 点击事件listener
- 04.问题反馈
- 05.优化相关
- 06.关于参考
- 07.其他说明介绍


### 01.前沿说明
#### 1.1 案例展示效果



#### 1.2 该库功能和优势
- 按组划分的自定义adapter适配器，一个recyclerView可以完成强大的group+children类型的业务需求。
- 每组支持添加header，footer，children，且每一个都支持设置多类型type的view视图。
- 支持局部插入刷新，局部移除刷新，也就是说可以按组插入或者移除数据，或者按组中child的某个未知插入或者移除数据。
- 支持组中header，footer，child的各个视图view的自定义点击事件
- 常见使用场景：仿懂车帝，汽车之家分组图片查看器；仿QQ联系人分组，可以折叠和伸展；以及复杂分组页面……


### 02.如何使用
#### 2.1 如何引入
- 如下所示
    ```
    implementation 'cn.yc:GroupAdapterLib:1.0.0'
    ```


#### 2.2 最简单使用
- 必须的三个步骤代码，如下所示
    ```
    mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    mAdapter = new GroupedSecondAdapter(this, list);
    mRecyclerView.setAdapter(mAdapter);
    ```
- 关于自定义adapter代码，直接看3.1代码



#### 2.3 使用建议



### 03.常用api
#### 3.1 自定义adapter
- 代码如下所示
    ```
    public class GroupedSecondAdapter extends AbsGroupedAdapter {
    
        private List<GroupEntity> mGroups;
    
        public GroupedSecondAdapter(Context context, List<GroupEntity> groups) {
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
            return true;
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
            
        }
    
        @Override
        public void onBindFooterViewHolder(GroupViewHolder holder, int groupPosition) {
            
        }
    
        @Override
        public void onBindChildViewHolder(GroupViewHolder holder, int groupPosition, int childPosition) {
            
        }
    
    }
    ```


#### 3.2 notify相关


#### 3.3 点击事件listener
- 代码如下所示
    ```
    mAdapter.setOnHeaderClickListener(new OnHeaderClickListener() {
        @Override
        public void onHeaderClick(AbsGroupedAdapter adapter, GroupViewHolder holder,
                                  int groupPosition) {
            Toast.makeText(SecondActivity.this,
                    "组头：groupPosition = " + groupPosition,Toast.LENGTH_LONG).show();
        }
    });
    mAdapter.setOnFooterClickListener(new OnFooterClickListener() {
        @Override
        public void onFooterClick(AbsGroupedAdapter adapter, GroupViewHolder holder,
                                  int groupPosition) {
            Toast.makeText(SecondActivity.this,
                    "组尾：groupPosition = " + groupPosition,Toast.LENGTH_LONG).show();
        }
    });
    mAdapter.setOnChildClickListener(new OnChildClickListener() {
        @Override
        public void onChildClick(AbsGroupedAdapter adapter, GroupViewHolder holder,
                                 int groupPosition, int childPosition) {
            Toast.makeText(SecondActivity.this,"子项：groupPosition = " + groupPosition
                    + ", childPosition = " + childPosition,Toast.LENGTH_LONG).show();
        }
    });
    ```


