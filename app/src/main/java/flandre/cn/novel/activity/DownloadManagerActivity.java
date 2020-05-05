package flandre.cn.novel.activity;

import android.content.*;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import flandre.cn.novel.R;
import flandre.cn.novel.Tools.NovelConfigure;
import flandre.cn.novel.Tools.NovelConfigureManager;
import flandre.cn.novel.database.SQLTools;
import flandre.cn.novel.database.SQLiteNovel;
import flandre.cn.novel.info.NovelDownloadInfo;
import flandre.cn.novel.Tools.Decoration;

import java.util.Date;
import java.util.List;

/**
 * 下载管理
 * 2020.4.2
 */
public class DownloadManagerActivity extends BaseActivity {
    private Adapter adapter;
    private SQLiteNovel sqLiteNovel;
    private TextView textView;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download_manager);
        setupMusicService();
        NovelConfigureManager.getConfigure(getApplicationContext());
        addDownloadFinishListener(this);
        handler = new Handler(getMainLooper());

        sqLiteNovel = SQLiteNovel.getSqLiteNovel(getApplicationContext());
        setupNovelService();

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("下载管理");
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setBackgroundDrawable(new ColorDrawable(NovelConfigureManager.getConfigure().getMainTheme()));
        textView = findViewById(R.id.none);
        textView.setTextColor(NovelConfigureManager.getConfigure().getIntroduceTheme());
        findViewById(R.id.total).setBackgroundColor(NovelConfigureManager.getConfigure().getBackgroundTheme());

        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setOrientation(LinearLayoutManager.VERTICAL);

        Decoration decoration = new Decoration(this);
        RecyclerView recyclerView = findViewById(R.id.data);
        recyclerView.setLayoutManager(manager);
        recyclerView.addItemDecoration(decoration);
        adapter = new Adapter(null);
        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(false);
        loadData();
    }

    /**
     * 加载下载信息
     */
    private void loadData() {
        List<NovelDownloadInfo> downloadInfo = SQLTools.getDownloadInfo(sqLiteNovel);
        if (downloadInfo.size() > 0) {
            adapter.updateData(downloadInfo);
            textView.setVisibility(View.GONE);
        } else {
            textView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        removeDownloadFinishListener(this);
    }

    @Override
    public void onDownloadFinish(int downloadFinish, int downloadCount, long downloadId) {
        int pos = 0;
        for (; pos < adapter.infos.size(); pos++)
            if (adapter.infos.get(pos).getId() == downloadId)
                break;
        if (pos >= adapter.infos.size()) return;
        NovelDownloadInfo downloadInfo = adapter.infos.get(pos);
        // 如果下载下一个任务, 重新导入
        if (downloadInfo.getStatus() != SQLiteNovel.DOWNLOAD_PAUSE) {
            loadData();
            return;
        }
        downloadInfo.setCount(downloadCount);
        downloadInfo.setFinish(downloadFinish);
        // 下载完成时更新状态
        if (downloadCount == downloadFinish) {
            downloadInfo.setStatus(SQLiteNovel.DOWNLOAD_FINISH);
            ContentValues values = new ContentValues();
            values.put("finish", downloadFinish);
            values.put("count", downloadCount);
            values.put("status", downloadInfo.getStatus());
            sqLiteNovel.getReadableDatabase().update("download", values, "id = ?", new String[]{String.valueOf(downloadId)});
        }
        // 这里有个小bug, 一直更新界面的话会消化不了用户的点击
        // adapter.notifyDataSetChanged();
        // 使用消息队列应该可以解决这个bug, 因为点击事件使用消息队列
        handler.post(new Runnable() {
            @Override
            public void run() {
                adapter.notifyDataSetChanged();
            }
        });
        // adapter.notifyItemChanged(pos);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    class Adapter extends RecyclerView.Adapter<Adapter.Holder> {
        List<NovelDownloadInfo> infos;

        private String[] status = new String[]{"暂停", "等待", "继续", "完成"};

        public Adapter(List<NovelDownloadInfo> infos) {
            this.infos = infos;
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.download_list, viewGroup, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int i) {
            NovelDownloadInfo downloadInfo = infos.get(i);
            double progress = (double) downloadInfo.getFinish() / (double) downloadInfo.getCount() * 100;
            String present = downloadInfo.getFinish() + "/" + downloadInfo.getCount();
            holder.info.setText(present);
            holder.action.setText(status[downloadInfo.getStatus()]);
            holder.name.setText(downloadInfo.getTable());
            holder.progress.setProgress((int) progress);
            setTheme(holder);
            setListener(holder);
        }

        private void setTheme(Holder holder) {
            holder.info.setTextColor(NovelConfigureManager.getConfigure().getIntroduceTheme());
            holder.name.setTextColor(NovelConfigureManager.getConfigure().getNameTheme());
            holder.delete.setImageResource(NovelConfigureManager.getConfigure().getMode() == NovelConfigure.DAY
                    ? R.drawable.close_day : R.drawable.close_night);
            holder.action.setTextColor(NovelConfigureManager.getConfigure().getAuthorTheme());
            holder.progress.setProgressDrawable(NovelConfigureManager.getConfigure().getMode() == NovelConfigure.DAY ?
                    getResources().getDrawable(R.drawable.progress_day) : getResources().getDrawable(R.drawable.progress_night));
        }

        private void setListener(Holder holder) {
            holder.delete.setTag(holder);
            holder.action.setTag(holder);
            holder.itemView.setTag(holder);

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    changeStatus(v);
                }
            });

            holder.action.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    changeStatus(v);
                }
            });

            holder.delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int pos = ((Holder) v.getTag()).getAdapterPosition();
                    NovelDownloadInfo downloadInfo = infos.get(pos);
                    if (downloadInfo.getStatus() == SQLiteNovel.DOWNLOAD_PAUSE) mService.stopDownload(true, false);
                    sqLiteNovel.getReadableDatabase().delete("download", "id = ?",
                            new String[]{String.valueOf(infos.get(pos).getId())});
                    infos.remove(pos);
                    if (infos.size() == 0) textView.setVisibility(View.GONE);
                    notifyItemRemoved(pos);
                }
            });
        }

        /**
         * 改变点击的下载状态
         */
        private void changeStatus(View view) {
            int pos = ((Holder) view.getTag()).getAdapterPosition();
            if (pos < 0) return;
            NovelDownloadInfo downloadInfo = infos.get(pos);
            switch (downloadInfo.getStatus()) {
                case SQLiteNovel.DOWNLOAD_PAUSE:
                    // 用户点击了暂停, 把下载暂停
                    // 如果暂停成功修改文本, 不然什么都不做
                    if (mService.stopDownload(true, true))
                        downloadInfo.setStatus(SQLiteNovel.DOWNLOAD_CONTINUE);
                    else {
                        Toast.makeText(DownloadManagerActivity.this, "点太快了", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    break;
                case SQLiteNovel.DOWNLOAD_WAIT:
                    // 用户点击了等待, 把等待设置为继续
                    downloadInfo.setStatus(SQLiteNovel.DOWNLOAD_CONTINUE);
                    break;
                case SQLiteNovel.DOWNLOAD_CONTINUE:
                    // 当用户点击了继续, 如果有等待的也设置为等待, 没有就开始下载这个
                    for (NovelDownloadInfo info : infos)
                        if (info.getStatus() == SQLiteNovel.DOWNLOAD_PAUSE) {
                            downloadInfo.setStatus(SQLiteNovel.DOWNLOAD_WAIT);
                            break;
                        }
                    if (downloadInfo.getStatus() == SQLiteNovel.DOWNLOAD_CONTINUE)
                        if (mService.download(downloadInfo))
                            downloadInfo.setStatus(SQLiteNovel.DOWNLOAD_PAUSE);
                        else {
                            Toast.makeText(DownloadManagerActivity.this, "点太快了", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    break;
                case SQLiteNovel.DOWNLOAD_FINISH:
                    // 点击已经完成的什么都不做
                    return;
            }
            ContentValues values = new ContentValues();
            values.put("status", downloadInfo.getStatus());
            values.put("time", new Date().getTime());
            sqLiteNovel.getReadableDatabase().update("download", values, "id = ?",
                    new String[]{String.valueOf(downloadInfo.getId())});
            notifyDataSetChanged();
//             notifyItemChanged(pos);
        }

        @Override
        public int getItemCount() {
            if (infos == null) return 0;
            return infos.size();
        }

        public void updateData(List<NovelDownloadInfo> infos) {
            this.infos = infos;
            notifyDataSetChanged();
        }

        class Holder extends RecyclerView.ViewHolder {
            ImageView delete;
            TextView name;
            TextView action;
            ProgressBar progress;
            TextView info;

            public Holder(@NonNull View itemView) {
                super(itemView);
                delete = itemView.findViewById(R.id.delete);
                name = itemView.findViewById(R.id.name);
                action = itemView.findViewById(R.id.action);
                progress = itemView.findViewById(R.id.progress);
                info = itemView.findViewById(R.id.info);
            }
        }
    }
}
