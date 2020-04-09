package flandre.cn.novel.adapter;

import android.app.Activity;
import android.view.LayoutInflater;
import android.widget.BaseAdapter;

import java.util.List;
import java.util.Map;

public abstract class NovelAdapter<T> extends BaseAdapter {
    public List<Map<String, T>> data;
    LayoutInflater inflater;
    Activity activity;

    NovelAdapter(Activity context, List<Map<String, T>> data) {
        this.data = data;
        this.activity = context;
        this.inflater = context.getLayoutInflater();
    }

    public void updateAdapter(List<Map<String, T>> list){
        this.data = list;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Object getItem(int i) {
        return data.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }
}
