package flandre.cn.novel.adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import flandre.cn.novel.R;
import flandre.cn.novel.Tools.NovelConfigureManager;
import flandre.cn.novel.info.Item;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.Holder> implements View.OnClickListener {
    private List<Item> list;
    private OnItemClick onItemClick;

    public UserAdapter(List<Item> list){
        this.list = list;
    }

    public void updateList(List<Item> items){
        this.list = items;
        notifyDataSetChanged();
    }

    public void setOnItemClick(OnItemClick onItemClick) {
        this.onItemClick = onItemClick;
    }

    @NonNull
    @Override
    public UserAdapter.Holder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.user_list, viewGroup, false);
        view.setOnClickListener(this);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserAdapter.Holder holder, int i) {
        Item item = list.get(i);
        holder.itemView.setTag(i);
        holder.imageView.setImageResource(item.getImageId());
        holder.textView.setText(item.getText());
        holder.textView.setTextColor(NovelConfigureManager.getConfigure().getAuthorTheme());
    }

    @Override
    public int getItemCount() {
        if (list == null) return 0;
        return list.size();
    }

    @Override
    public void onClick(View v) {
        int position = (int) v.getTag();
        if (onItemClick != null) onItemClick.onClick(position);
    }

    class Holder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView textView;

        Holder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.txt);
            imageView = itemView.findViewById(R.id.icon);
        }
    }

    public interface OnItemClick{
        public void onClick(int position);
    }
}
