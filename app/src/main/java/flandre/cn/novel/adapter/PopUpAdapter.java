package flandre.cn.novel.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import flandre.cn.novel.R;
import flandre.cn.novel.Tools.NovelConfigureManager;
import flandre.cn.novel.Tools.Item;

import java.util.List;

public class PopUpAdapter extends RecyclerView.Adapter<PopUpAdapter.Holder> implements View.OnClickListener {
    private List<Item> items;
    private OnPopUpClickListener listener;

    public PopUpAdapter(List<Item> items) {
        this.items = items;
    }

    public void update(List<Item> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @Override
    public PopUpAdapter.Holder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.dialog_list, viewGroup, false);
        view.setOnClickListener(this);
        return new PopUpAdapter.Holder(view);
    }

    @Override
    public void onBindViewHolder(PopUpAdapter.Holder holder, int i) {
        Item item = items.get(i);
        holder.imageView.setImageResource(item.getImageId());
        holder.textView.setText(item.getText());
        holder.itemView.setTag(i);
        holder.textView.setTextColor(NovelConfigureManager.getConfigure().getNameTheme());
    }

    @Override
    public void onClick(View v) {
        int pos = (int) v.getTag();
        if (listener != null) listener.popUpClickListener(v, pos);
    }

    public void setListener(OnPopUpClickListener listener) {
        this.listener = listener;
    }


    @Override
    public int getItemCount() {
        if (items == null) return 0;
        return items.size();
    }

    public interface OnPopUpClickListener {
        public void popUpClickListener(View view, int pos);
    }

    class Holder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView textView;

        Holder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.txt);
            imageView = itemView.findViewById(R.id.img);
        }
    }
}
