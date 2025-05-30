package com.xwrl.mvvm.demo.adapter;

import android.app.Application;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.databinding.BindingAdapter;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ObservableArrayList;
import androidx.databinding.ObservableList;
import androidx.databinding.ViewDataBinding;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public abstract class BaseBindingAdapter<M,B extends ViewDataBinding>
        extends RecyclerView.Adapter<BaseBindingAdapter.BaseBindingViewHolder>{

    private static final String TAG = "BaseBindingAdapter";

    protected Application context;
    private ObservableArrayList<M> mItems;
    protected ListChangedCallback mListChangedCallback;
    private InsertedListener mInsertedListener;

    public BaseBindingAdapter(Application context) {
        this.context = context;
        this.mItems = new ObservableArrayList<>();
        this.mListChangedCallback = new ListChangedCallback();
    }

    /**
     * adapter中的数据应该与外面隔开，所以不能返回list集合的地址
     * @return ObservableArrayList<M> 返回一个数据集合即可
     * */
    public ObservableArrayList<M> getItems() {
        ObservableArrayList<M> m = new ObservableArrayList<>();
        m.addAll(mItems);
        return m;
    }

    public BaseBindingAdapter<M, B> setItems(List<M> newItems) {
        if (newItems == null) return this;
        if (mItems != null) {
            if (mItems.size() > 0) mItems.clear();
            mItems.addAll(newItems);
        }
        return this;
    }

    public boolean isHaveMusic(){ return mItems != null && mItems.size() > 0; }

    public void setInsertedListener(InsertedListener insertedListener) {
        this.mInsertedListener = insertedListener;
    }

    public static class BaseBindingViewHolder extends RecyclerView.ViewHolder{

        public BaseBindingViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    @Override
    public int getItemCount() {
        return mItems == null ? 0 : this.mItems.size();
    }

    protected Application getContext() {
        return context;
    }

    /**
     * 释放资源
     * */
    protected void release(){
        if (context != null) { context = null; }
        if (mListChangedCallback != null) { mListChangedCallback = null; }
        if (mItems != null) {
            if (mItems.size() > 0) { mItems.clear(); }
            mItems = null;
        }
    }
    /**
    * 视图、数据绑定
    * */
    @NonNull
    @Override
    public BaseBindingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        B binding = DataBindingUtil.inflate(LayoutInflater.from(this.context),
                                            this.getLayoutResId(viewType), parent, false);
        return new BaseBindingViewHolder(binding.getRoot());
    }

    @Override
    public void onBindViewHolder(@NonNull BaseBindingViewHolder holder, int position) { }

    @Override
    public void onBindViewHolder(@NonNull BaseBindingViewHolder holder, int position,
                                 @NonNull List<Object> payloads) {

        B binding = DataBindingUtil.getBinding(holder.itemView);

        //Log.w(TAG, "onBindViewHolder: "+ position);

        if (payloads.isEmpty()){
            this.onBindItem(binding,this.mItems.get(position),position);
        }else {
            //Log.w(TAG, "onBindViewHolder: ");
            this.onExtraBindItem(binding,++position,payloads);
        }
    }

    @Override
    public void onViewRecycled(@NonNull BaseBindingViewHolder holder) {
        super.onViewRecycled(holder);
        this.onUnBindItem(DataBindingUtil.getBinding(holder.itemView));
    }

    //子类实现
    @LayoutRes
    protected abstract int getLayoutResId(int ViewType);  //每一项item的布局id， 通过int来加载不同布局

    protected abstract void onBindItem(B binding, M item, int position);  //绑定每一项item的布局

    protected abstract void onUnBindItem(B binding);  //取消绑定每一项item的布局

    protected abstract void onExtraBindItem(B binding, int queue, @NonNull List<Object> payloads); //局部更新某项item的布局
    /**
    * RecyclerView视图分离与固定
    * */
    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        if (mItems != null) this.mItems.addOnListChangedCallback(mListChangedCallback);
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        if (mItems != null) this.mItems.removeOnListChangedCallback(mListChangedCallback);
    }
    /**
    * 处理数据集合变化
    * */
    private void onChange(ObservableArrayList<M> newItems){
        resetItems(newItems);
        notifyDataSetChanged();
    }

    private void onItemRangeChanged(ObservableArrayList<M> newItems, int positionStart, int itemCount) {
        resetItems(newItems);
        notifyItemRangeChanged(positionStart, itemCount);
    }

    private void onItemRangeInserted(ObservableArrayList<M> newItems, int positionStart, int itemCount) {
        resetItems(newItems);
        notifyItemRangeInserted(positionStart, itemCount);
    }

    private void onItemRangeMoved(ObservableArrayList<M> newItems) {
        resetItems(newItems);
        notifyDataSetChanged();
    }

    private void onItemRangeRemoved(ObservableArrayList<M> newItems, int positionStart, int itemCount) {
        resetItems(newItems);
        notifyItemRangeRemoved(positionStart, itemCount);
    }

    private void resetItems(ObservableArrayList<M> newItems) {
        this.mItems = newItems;
    }

    private class ListChangedCallback extends ObservableList.OnListChangedCallback<ObservableArrayList<M>>{

        @Override
        public void onChanged(ObservableArrayList<M> newItems) {
            Log.d(TAG, "onChanged: ");
            BaseBindingAdapter.this.onChange(newItems);
        }

        @Override
        public void onItemRangeChanged(ObservableArrayList<M> newItems, int positionStart, int itemCount) {
            BaseBindingAdapter.this.onItemRangeChanged(newItems, positionStart, itemCount);
            Log.d(TAG, "onItemRangeChanged: ");
        }

        @Override
        public void onItemRangeInserted(ObservableArrayList<M> newItems, int positionStart, int itemCount) {
            BaseBindingAdapter.this.onItemRangeInserted(newItems, positionStart, itemCount);
            Log.d(TAG, "onItemRangeInserted: ");
            //通知本列表已加载完成,Handler延时或者相应的回调来实现
            new Handler().postDelayed(()-> {
                if (mInsertedListener != null) { mInsertedListener.onInserted();}
            }, 210);
        }

        @Override
        public void onItemRangeMoved(ObservableArrayList<M> newItems, int fromPosition, int toPosition, int itemCount) {
            BaseBindingAdapter.this.onItemRangeMoved(newItems);
            Log.d(TAG, "onItemRangeMoved: ");
        }

        @Override
        public void onItemRangeRemoved(ObservableArrayList<M> newItems, int positionStart, int itemCount) {
            BaseBindingAdapter.this.onItemRangeRemoved(newItems, positionStart, itemCount);
            Log.d(TAG, "onItemRangeRemoved: ");
        }
    }

    /**
     * 局部更新Item， 添加和移除，不用整体刷新列表
     * 供 子类和外部 调用
     * */
    public void onItemRangeRemoved(int positionStart){  //移除
        if (positionStart > getItemCount() || positionStart < 0) {
            Toast.makeText(context,"列表移除失败！原因：超出范围！",Toast.LENGTH_SHORT).show(); return; }

        int itemCount = getItemCount() - positionStart;
        mItems.remove(positionStart);
        onItemRangeRemoved(mItems, positionStart, itemCount);
        //最后检查itemCount，如果为0，就显示出空列表提示视图
        if (getItemCount() == 0) {
            Log.d(TAG, "onItemRangeRemoved: 列表空了");
        }
    }
    public void onItemRangeInsert(int positionStart){
        if (positionStart > getItemCount() || positionStart < 0) {
            Toast.makeText(context,"列表添加失败！原因：超出范围！",Toast.LENGTH_SHORT).show(); return; }

        int itemCount = getItemCount() - positionStart;
        mItems.remove(positionStart);
        onItemRangeRemoved(mItems, positionStart, itemCount);
    }
    public void onItemRangeToTop(int positionStart){  //置顶

        if (!isHaveMusic() || positionStart > getItemCount() || positionStart < 0) {
            Toast.makeText(context,"列表置顶失败！原因：超出范围！",Toast.LENGTH_SHORT).show(); return; }

        ObservableArrayList<M> temp = new ObservableArrayList<>();
        temp.add(mItems.get(positionStart));

        mItems.remove(positionStart);

        temp.addAll(mItems);

        onChange(temp);
    }

    /**
     * 作用: 解决在使用dataBinding 在布局文件给ImageView src属性绑定DrawableResId，不显示相应图片或显示颜色块的问题
     *
     * 参考：https://blog.csdn.net/Ryfall/article/details/
     * 90750270?spm=1001.2101.3001.6650.3&utm_medium=distribute.pc_relevant.none
     * -task-blog-2%7Edefault%7ECTRLIST%7Edefault-3.no_search_link&depth_1-utm_source
     * =distribute.pc_relevant.none-task-blog-2%7Edefault%7ECTRLIST%7Edefault-3.no_search_link
     *
     * 过程分析：https://blog.csdn.net/zhuhai__yizhi/article/details/52181697
     * */
    @BindingAdapter("android:src")
    public static void setSrc(ImageView view, int resId) {
        //Log.d(TAG, "setSrc: ");
        view.setImageResource(resId);
    }
}
