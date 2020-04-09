package flandre.cn.novel.adapter;

import android.app.Activity;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import flandre.cn.novel.R;
import flandre.cn.novel.Tools.NovelAttr;
import flandre.cn.novel.Tools.NovelConfigureManager;

import java.util.List;
import java.util.Map;

public class MenuAdapter extends NovelAdapter<String> {

    public MenuAdapter(Activity context, List<Map<String, String>> data) {
        super(context, data);
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        Map<String, String> map = data.get(i);
        ViewHolder viewHolder;
        if (view == null) {
            view = inflater.inflate(R.layout.menu_list, null);
            viewHolder = new ViewHolder();
            viewHolder.item = view.findViewById(R.id.text);
            viewHolder.item.setTextColor(NovelConfigureManager.getConfigure().getTextColor());
            view.setTag(viewHolder);
        }else {
            viewHolder = (ViewHolder) view.getTag();
        }
        viewHolder.item.setText(map.get("chapter"));
//        if (Integer.parseInt(map.get("id")) == NovelAttr.textActivity.getChapter()){
//            viewHolder.item.setTextColor(Color.parseColor("#909090"));
//        }
        return view;
    }

    private class ViewHolder {
        private TextView item = null;
    }
}
