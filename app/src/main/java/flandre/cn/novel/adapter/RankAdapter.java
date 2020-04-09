package flandre.cn.novel.adapter;

import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import flandre.cn.novel.R;
import flandre.cn.novel.Tools.NovelConfigureManager;
import flandre.cn.novel.info.NovelInfo;

import java.util.List;
import java.util.Map;

public class RankAdapter extends RecyclerView.Adapter<RankAdapter.Holder> implements View.OnClickListener{
    private List<NovelInfo> data;
    private RankClick listen;

    public List<NovelInfo> getData() {
        return data;
    }

    public void setData(List<NovelInfo> data) {
        this.data = data;
    }

    public RankAdapter(List<NovelInfo> list){
        this.data = list;
    }

    public void update(List<NovelInfo> list){
        this.data = list;
        notifyDataSetChanged();
    }

    public void setListen(RankClick listen){
        this.listen = listen;
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.rank_list, viewGroup, false);
        view.setOnClickListener(this);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(Holder holder, int i) {
        holder.itemView.setTag(i);
        NovelInfo novelInfo = data.get(i);
        if (novelInfo == null) return;
        holder.image.setImageBitmap(novelInfo.getBitmap());
        holder.name.setText(novelInfo.getName());
        holder.introduce.setText(novelInfo.getIntroduce());
        holder.author.setText(novelInfo.getAuthor());
        holder.name.setTextColor(NovelConfigureManager.getConfigure().getNameTheme());
        holder.author.setTextColor(NovelConfigureManager.getConfigure().getAuthorTheme());
        holder.introduce.setTextColor(NovelConfigureManager.getConfigure().getIntroduceTheme());
    }

    @Override
    public int getItemCount() {
        if (data == null) return 0;
        return data.size();
    }

    @Override
    public void onClick(View v) {
        int pos = (int) v.getTag();
        if (listen != null) listen.OnClickListen(v, pos);
    }

    class Holder extends RecyclerView.ViewHolder{
        TextView name, author, introduce;
        ImageView image;

        Holder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.name);
            author = itemView.findViewById(R.id.author);
            introduce = itemView.findViewById(R.id.introduce);
            image = itemView.findViewById(R.id.image);
        }
    }
    public interface RankClick{
        void OnClickListen(View view, int pos);
    }

}
