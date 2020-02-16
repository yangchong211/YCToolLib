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
- demo中的效果图
![image](https://github.com/yangchong211/YCGroupAdapter/blob/master/image/1.png)
![image](https://github.com/yangchong211/YCGroupAdapter/blob/master/image/2.png)
![image](https://github.com/yangchong211/YCGroupAdapter/blob/master/image/3.png)
![image](https://github.com/yangchong211/YCGroupAdapter/blob/master/image/4.png)
![image](https://github.com/yangchong211/YCGroupAdapter/blob/master/image/5.png)
![image](https://github.com/yangchong211/YCGroupAdapter/blob/master/image/6.png)
![image](https://github.com/yangchong211/YCGroupAdapter/blob/master/image/7.png)


- 实际项目中的效果图
![image](https://github.com/yangchong211/YCGroupAdapter/blob/master/image/10.png)
![image](https://github.com/yangchong211/YCGroupAdapter/blob/master/image/11.png)




#### 1.2 该库功能和优势
- 按组划分的自定义adapter适配器，一个recyclerView可以完成强大的group+children类型的业务需求。
- 每组支持添加header，footer，children，且每一个都支持设置多类型type的view视图。
- 支持局部插入刷新，局部移除刷新，也就是说可以按组插入或者移除数据，或者按组中child的某个未知插入或者移除数据。
- 支持组中header，footer，child的各个视图view的自定义点击事件。且返回具体的索引！
- 常见使用场景：仿懂车帝，汽车之家分组图片查看器；仿QQ联系人分组，可以折叠和伸展；以及复杂分组页面……
- 添加了object同步锁处理adapter中data添加，获取和移除等方法，有效避免多线程或者其他操作导致数据错位或者偶发性fast-fail。




### 02.如何使用
#### 2.1 如何引入
- 如下所示
    ```
    implementation 'cn.yc:GroupAdapterLib:1.0.3'
    ```


#### 2.2 最简单使用
- 必须的三个步骤代码，如下所示
    ```
    mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    mAdapter = new GroupedSecondAdapter(this, list);
    mRecyclerView.setAdapter(mAdapter);
    ```
- 关于如何实现仿照QQ分组的功能
    ```
    /**
     * 判断当前组是否展开
     *
     * @param groupPosition
     * @return
     */
    public boolean isExpand(int groupPosition) {
        GroupEntity entity = mGroups.get(groupPosition);
        return entity.isExpand();
    }
    
    /**
     * 展开一个组
     *
     * @param groupPosition
     */
    public void expandGroup(int groupPosition) {
        expandGroup(groupPosition, false);
    }
    
    /**
     * 展开一个组
     *
     * @param groupPosition
     * @param animate
     */
    public void expandGroup(int groupPosition, boolean animate) {
        GroupEntity entity = mGroups.get(groupPosition);
        entity.setExpand(true);
        if (animate) {
            notifyChildrenInserted(groupPosition);
        } else {
            notifyDataChanged();
        }
    }
    
    /**
     * 收起一个组
     *
     * @param groupPosition
     */
    public void collapseGroup(int groupPosition) {
        collapseGroup(groupPosition, false);
    }
    
    /**
     * 收起一个组
     *
     * @param groupPosition
     * @param animate
     */
    public void collapseGroup(int groupPosition, boolean animate) {
        GroupEntity entity = mGroups.get(groupPosition);
        entity.setExpand(false);
        if (animate) {
            notifyChildrenRemoved(groupPosition);
        } else {
            notifyDataChanged();
        }
    }
    
    /**
     * 收起所有的组
     */
    public void collapseGroup() {
        for (int i=0 ; i<mGroups.size() ; i++){
            GroupEntity entity = mGroups.get(i);
            entity.setExpand(false);
        }
        notifyDataChanged();
    }
    ```

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
- 那么如何控制组中的header或者footer是否显示呢？
    - 返回true表示显示，返回false表示不显示……就是这么简单
    ```
    @Override
    public boolean hasHeader(int groupPosition) {
        return true;
    }
    
    @Override
    public boolean hasFooter(int groupPosition) {
        return true;
    }
    ```

#### 3.2 notify相关
- 插入数据
    ```
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
    ```
- 移除数据
    ```
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
    ```


#### 3.3 点击事件listener
- 设置组header点击事件
    ```
    mAdapter.setOnHeaderClickListener(new OnHeaderClickListener() {
        @Override
        public void onHeaderClick(AbsGroupedAdapter adapter, GroupViewHolder holder,
                                  int groupPosition) {
            Toast.makeText(SecondActivity.this,
                    "组头：groupPosition = " + groupPosition,Toast.LENGTH_LONG).show();
        }
    });
    ```
- 设置组footer点击事件
    ```
    mAdapter.setOnFooterClickListener(new OnFooterClickListener() {
        @Override
        public void onFooterClick(AbsGroupedAdapter adapter, GroupViewHolder holder,
                                  int groupPosition) {
            Toast.makeText(SecondActivity.this,
                    "组尾：groupPosition = " + groupPosition,Toast.LENGTH_LONG).show();
        }
    });
    ```
- 设置组中children点击事件
    ```
    mAdapter.setOnChildClickListener(new OnChildClickListener() {
        @Override
        public void onChildClick(AbsGroupedAdapter adapter, GroupViewHolder holder,
                                 int groupPosition, int childPosition) {
            Toast.makeText(SecondActivity.this,"子项：groupPosition = " + groupPosition
                    + ", childPosition = " + childPosition,Toast.LENGTH_LONG).show();
        }
    });
    ```



#### 参考案例说明
- https://github.com/msdx/group-recycler-adapter
- https://www.jianshu.com/p/7b5607a7fbe1
- https://www.jianshu.com/p/26b0911f396f
- https://github.com/donkingliang/GroupedRecyclerViewAdapter
- https://www.jianshu.com/p/1558cbd20d17



#### 其他推荐说明
- 1.[技术博客汇总](https://www.jianshu.com/p/614cb839182c)
- 2.[开源项目汇总](https://blog.csdn.net/m0_37700275/article/details/80863574)
- 3.[生活博客汇总](https://blog.csdn.net/m0_37700275/article/details/79832978)
- 4.[喜马拉雅音频汇总](https://www.jianshu.com/p/f665de16d1eb)
- 5.[其他汇总](https://www.jianshu.com/p/53017c3fc75d)
- 6.[重点推荐：博客笔记大汇总，开源文件都是md格式](https://github.com/yangchong211/YCBlogs)



#### 关于LICENSE
```
Copyright 2017 yangchong211（github.com/yangchong211）

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

