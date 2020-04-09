package flandre.cn.novel.adapter;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import flandre.cn.novel.R;
import flandre.cn.novel.Tools.NovelConfigure;
import flandre.cn.novel.Tools.NovelConfigureManager;
import flandre.cn.novel.Tools.NovelTools;
import flandre.cn.novel.info.NovelInfo;
import flandre.cn.novel.info.WrapperNovelInfo;
import flandre.cn.novel.view.CircularProgressView;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class ReadTimeAdapter extends RecyclerView.Adapter<ReadTimeAdapter.ItemHolder> implements View.OnClickListener {
    public List<WrapperNovelInfo> data;
    private OnItemClick listener;
    private Context mContext;

    public void setListener(OnItemClick listener) {
        this.listener = listener;
    }

    public ReadTimeAdapter(List<WrapperNovelInfo> data, Context context) {
        this.data = data;
        mContext = context;
    }

    public void update(List<WrapperNovelInfo> list) {
        data = list;
        notifyDataSetChanged();
    }

    @Override
    public ItemHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        // 当read不为空时表示创建的是一个详细信息
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.read_list, viewGroup, false);
        return new ItemHolder(view);
    }

    @Override
    public void onBindViewHolder(ItemHolder holder, int i) {
        setItemData(holder, i);
    }

    /**
     * 设置Item的内容
     */
    private void setItemData(ItemHolder holder, int i) {
        WrapperNovelInfo wrapperNovelInfo = data.get(i);
        NovelInfo info = wrapperNovelInfo.getInfo();
        holder.itemView.setTag(holder);
        holder.itemView.setOnClickListener(this);

        holder.name.setText(info.getName());
        holder.author.setText(info.getAuthor());
        holder.newChapter.setText(info.getChapter());
        double percent = (double) wrapperNovelInfo.getChapter() / (double) wrapperNovelInfo.getCount() * 100;
        holder.progress.setProgress((int) percent);
        holder.percent.setText(new BigDecimal(percent).setScale(1, BigDecimal.ROUND_HALF_UP) + "%");
        holder.image.setImageBitmap(BitmapFactory.decodeFile(info.getImagePath()));
        setTopItemTheme(holder);
        if (!wrapperNovelInfo.isShowDetailInfo()) holder.bottom.setVisibility(View.GONE);
        else {
            holder.year_left.setText(new SimpleDateFormat("yyyy年").format(info.getStart()));
            holder.month_left.setText(new SimpleDateFormat("MM月").format(info.getStart()));
            holder.day_left.setText(new SimpleDateFormat("dd日").format(info.getStart()));
            holder.hour_left.setText(new SimpleDateFormat("HH时").format(info.getStart()));
            holder.minute_left.setText(new SimpleDateFormat("mm分").format(info.getStart()));
            if (info.getFinish() != 0) {
                holder.finish.setText("完成时间");
                holder.year_right.setText(new SimpleDateFormat("yyyy年").format(info.getStart()));
                holder.month_right.setText(new SimpleDateFormat("MM月").format(info.getStart()));
                holder.day_right.setText(new SimpleDateFormat("dd日").format(info.getStart()));
                holder.hour_right.setText(new SimpleDateFormat("HH时").format(info.getStart()));
                holder.minute_right.setText(new SimpleDateFormat("mm分").format(info.getStart()));
            }
            holder.watchChapter.setText(wrapperNovelInfo.getNowChapter());
            holder.watchLately.setText(NovelTools.resolver(new Date().getTime() - info.getTime()) + "前");
            holder.source.setText(NovelConfigureManager.novelSource.get(info.getSource()));
            holder.watchTime.setText(NovelTools.resolver(info.getRead()));
            holder.status.setText(info.getComplete() == 1 ? "已完结" : "连载中");
            setBottomItemTheme(holder);
            holder.bottom.setVisibility(View.VISIBLE);
        }
    }

    private void setTopItemTheme(ItemHolder handler) {
        NovelConfigure configure = NovelConfigureManager.getConfigure();
        handler.name.setTextColor(configure.getNameTheme());
        handler.author.setTextColor(configure.getAuthorTheme());
        handler.newChapter.setTextColor(configure.getIntroduceTheme());
        handler.progress.setBackColor(~configure.getBackgroundTheme() & 0x11FFFFFF | 0x11000000);
        handler.progress.setProgColor(configure.getIntroduceTheme());
        handler.percent.setTextColor(configure.getIntroduceTheme());
    }

    private void setBottomItemTheme(ItemHolder holder) {
        NovelConfigure configure = NovelConfigureManager.getConfigure();
        holder.start.setTextColor(configure.getNameTheme());
        holder.year_left.setTextColor(configure.getNameTheme());
        holder.month_left.setTextColor(configure.getIntroduceTheme());
        holder.day_left.setTextColor(configure.getIntroduceTheme());
        holder.hour_left.setTextColor(configure.getIntroduceTheme());
        holder.minute_left.setTextColor(configure.getIntroduceTheme());
        holder.finish.setTextColor(configure.getNameTheme());
        holder.year_right.setTextColor(configure.getNameTheme());
        holder.month_right.setTextColor(configure.getIntroduceTheme());
        holder.day_right.setTextColor(configure.getIntroduceTheme());
        holder.hour_right.setTextColor(configure.getIntroduceTheme());
        holder.minute_right.setTextColor(configure.getIntroduceTheme());
        holder.sep_left.setBackgroundColor(configure.getNameTheme());
        holder.sep_right.setBackgroundColor(configure.getNameTheme());
        holder.watchChapter.setTextColor(configure.getAuthorTheme());
        holder.watchLately.setTextColor(configure.getAuthorTheme());
        holder.source.setTextColor(configure.getAuthorTheme());
        holder.watchTime.setTextColor(configure.getAuthorTheme());
        holder.status.setTextColor(configure.getIntroduceTheme());
        holder.nowChapterIntro.setTextColor(configure.getNameTheme());
        holder.latelyIntro.setTextColor(configure.getNameTheme());
        holder.sourceIntro.setTextColor(configure.getNameTheme());
        holder.timeIntro.setTextColor(configure.getNameTheme());
    }

    @Override
    public int getItemCount() {
        if (data == null) return 0;
        return data.size();
    }

    @Override
    public void onClick(View v) {
        if (listener != null) listener.itemClick(v, (ItemHolder) v.getTag());
    }

    public class ItemHolder extends RecyclerView.ViewHolder {
        TextView name, author, newChapter, percent;
        TextView start, year_left, month_left, day_left, hour_left, minute_left;
        TextView finish, year_right, month_right, day_right, hour_right, minute_right;
        TextView watchChapter, watchLately, source, watchTime, status;
        TextView nowChapterIntro, latelyIntro, sourceIntro, timeIntro;
        View sep_left, sep_right;
        ImageView image;
        CircularProgressView progress;
        FrameLayout bottom;

        ItemHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.name);
            author = itemView.findViewById(R.id.author);
            newChapter = itemView.findViewById(R.id.newChapter);
            percent = itemView.findViewById(R.id.percent);
            start = itemView.findViewById(R.id.start);
            year_left = itemView.findViewById(R.id.year_left);
            month_left = itemView.findViewById(R.id.month_left);
            day_left = itemView.findViewById(R.id.day_left);
            hour_left = itemView.findViewById(R.id.hour_left);
            minute_left = itemView.findViewById(R.id.minute_left);
            finish = itemView.findViewById(R.id.finish);
            year_right = itemView.findViewById(R.id.year_right);
            month_right = itemView.findViewById(R.id.month_right);
            day_right = itemView.findViewById(R.id.day_right);
            hour_right = itemView.findViewById(R.id.hour_right);
            minute_right = itemView.findViewById(R.id.minute_right);
            watchChapter = itemView.findViewById(R.id.watchChapter);
            watchLately = itemView.findViewById(R.id.watchLately);
            source = itemView.findViewById(R.id.source);
            watchTime = itemView.findViewById(R.id.watchTime);
            status = itemView.findViewById(R.id.status);
            sep_left = itemView.findViewById(R.id.sep_left);
            sep_right = itemView.findViewById(R.id.sep_right);
            image = itemView.findViewById(R.id.image);
            progress = itemView.findViewById(R.id.progress);
            bottom = itemView.findViewById(R.id.bottom);
            nowChapterIntro = itemView.findViewById(R.id.nowChapterIntro);
            latelyIntro = itemView.findViewById(R.id.latelyIntro);
            sourceIntro = itemView.findViewById(R.id.sourceIntro);
            timeIntro = itemView.findViewById(R.id.timeIntro);
        }
    }

    /**
     * Adapter的Item被点击的处理事件
     */
    public interface OnItemClick {
        void itemClick(View view, ReadTimeAdapter.ItemHolder holder);
    }
}
